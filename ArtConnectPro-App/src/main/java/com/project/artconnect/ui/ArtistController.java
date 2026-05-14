package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Session;
import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ArtistController implements RoleAware {

    @FXML private TextField            searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist>            artistTable;
    @FXML private TableColumn<Artist, String>  nameColumn;
    @FXML private TableColumn<Artist, String>  cityColumn;
    @FXML private TableColumn<Artist, String>  emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;
    @FXML private TableColumn<Artist, String>  disciplineColumn;
    @FXML private GridPane             crudForm;
    @FXML private Label                crudTitle;
    @FXML private HBox                 crudBox;
    @FXML private TextField            formName;
    @FXML private TextField            formCity;
    @FXML private TextField            formEmail;
    @FXML private TextField            formPhone;
    @FXML private TextField            formYear;
    @FXML private ComboBox<Discipline> formDiscipline;
    @FXML private Label                statusLabel;

    private final ArtistService artistService = ServiceProvider.getArtistService();
    private final JdbcArtistDao artistDao = new JdbcArtistDao();
    private String originalName = null; // nom original de la ligne sélectionnée

    @FXML
    public void initialize() {
        nameColumn .setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn .setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn .setCellValueFactory(new PropertyValueFactory<>("birthYear"));
        if (disciplineColumn != null)
            disciplineColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                    cd.getValue().getDisciplines().isEmpty() ? "—"
                            : cd.getValue().getDisciplines().get(0).getName()));

        var disciplines = FXCollections.observableArrayList(artistService.getAllDisciplines());
        if (disciplineFilter != null) disciplineFilter.setItems(disciplines);
        if (formDiscipline   != null) formDiscipline  .setItems(disciplines);

        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        // Retirer le listener existant pour éviter les doublons
        artistTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {});

        clearForm();

        if (session.isPublic() || session.isMembre()) {
            // Lecture seule
            setVisible(crudForm,  false);
            setVisible(crudBox,   false);
            setVisible(crudTitle, false);

        } else if (session.isArtiste()) {
            // Formulaire visible, verrouillé sur son propre profil
            setVisible(crudForm,  true);
            setVisible(crudBox,   true);
            setVisible(crudTitle, true);
            if (crudTitle != null) crudTitle.setText("Edit My Profile");
            if (formName  != null) formName.setEditable(false);

            artistTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> {
                        if (sel != null && !sel.getName().equals(session.getDisplayName())) {
                            artistTable.getItems().stream()
                                    .filter(a -> a.getName().equals(session.getDisplayName()))
                                    .findFirst()
                                    .ifPresent(a -> { artistTable.getSelectionModel().select(a); fillForm(a); });
                        } else if (sel != null) {
                            fillForm(sel);
                        }
                    });

        } else {
            // ORGANISATEUR
            setVisible(crudForm,  true);
            setVisible(crudBox,   true);
            setVisible(crudTitle, true);
            if (crudTitle != null) crudTitle.setText("Add / Edit Artist");
            if (formName  != null) formName.setEditable(true);
            artistTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) fillForm(sel); });
        }

        refreshTable();

        if (session.isArtiste()) {
            artistTable.getItems().stream()
                    .filter(a -> a.getName().equals(session.getDisplayName()))
                    .findFirst()
                    .ifPresent(a -> { artistTable.getSelectionModel().select(a); fillForm(a); });
        }
    }

    @FXML private void handleSearch() {
        Discipline d = disciplineFilter != null ? disciplineFilter.getValue() : null;
        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(searchField.getText(),
                        d != null ? d.getName() : null, null)));
    }

    @FXML private void handleReset() {
        searchField.clear();
        if (disciplineFilter != null) disciplineFilter.setValue(null);
        refreshTable();
        Session s = Session.getInstance();
        if (s.isArtiste()) artistTable.getItems().stream()
                .filter(a -> a.getName().equals(s.getDisplayName()))
                .findFirst().ifPresent(a -> { artistTable.getSelectionModel().select(a); fillForm(a); });
    }

    @FXML private void handleCreate() {
        if (!Session.getInstance().isOrganisateur()) { setStatus("Accès refusé.", true); return; }
        originalName = null; // garantir un INSERT
        try {
            Artist a = buildFromForm();
            artistService.createArtist(a);
            if (formDiscipline != null && formDiscipline.getValue() != null)
                linkDiscipline(a.getName(), formDiscipline.getValue().getName());
            setStatus("Artist added.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleUpdate() {
        Artist sel = artistTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select an artist.", true); return; }
        Session session = Session.getInstance();
        if (session.isArtiste() && !sel.getName().equals(session.getDisplayName())) {
            setStatus("Vous ne pouvez modifier que votre propre profil.", true); return;
        }
        try {
            Artist a = buildFromForm(); // contient le nouveau nom saisi
            // Pour l'artiste connecté : forcer son propre nom (ne peut pas changer son nom)
            if (session.isArtiste()) a.setName(session.getDisplayName());
            // Passer l'ancien nom au DAO pour le WHERE
            String oldName = (session.isArtiste() || originalName == null)
                    ? a.getName() : originalName;
            artistDao.update(a, oldName);
            setStatus("Profile updated.", false); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleDelete() {
        if (!Session.getInstance().isOrganisateur()) { setStatus("Accès refusé.", true); return; }
        Artist sel = artistTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select an artist.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel.getName() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { artistService.deleteArtist(sel.getName());
                    setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML private void handleResetForm() {
        clearForm();
        Session s = Session.getInstance();
        if (s.isArtiste()) artistTable.getItems().stream()
                .filter(a -> a.getName().equals(s.getDisplayName()))
                .findFirst().ifPresent(this::fillForm);
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    private Artist buildFromForm() {
        if (formName.getText().isBlank()) throw new IllegalArgumentException("Name is required.");
        Artist a = new Artist();
        a.setName        (formName .getText().trim());
        a.setCity        (formCity != null ? formCity .getText().trim() : "");
        a.setContactEmail(formEmail != null ? formEmail.getText().trim() : "");
        a.setPhone       (formPhone != null ? formPhone.getText().trim() : "");
        if (formYear != null && !formYear.getText().isBlank())
            a.setBirthYear(Integer.parseInt(formYear.getText().trim()));
        a.setActive(true);
        return a;
    }

    private void fillForm(Artist a) {
        originalName = a.getName();
        if (formName  != null) formName .setText(a.getName()         != null ? a.getName()         : "");
        if (formCity  != null) formCity .setText(a.getCity()         != null ? a.getCity()         : "");
        if (formEmail != null) formEmail.setText(a.getContactEmail() != null ? a.getContactEmail() : "");
        if (formPhone != null) formPhone.setText(a.getPhone()        != null ? a.getPhone()        : "");
        if (formYear  != null) formYear .setText(a.getBirthYear()    != null ? a.getBirthYear().toString() : "");
        if (formDiscipline != null && !a.getDisciplines().isEmpty())
            formDiscipline.setValue(a.getDisciplines().get(0));
    }

    private void clearForm() {
        originalName = null;
        if (formName       != null) formName.clear();
        if (formCity       != null) formCity.clear();
        if (formEmail      != null) formEmail.clear();
        if (formPhone      != null) formPhone.clear();
        if (formYear       != null) formYear.clear();
        if (formDiscipline != null) formDiscipline.setValue(null);
        if (statusLabel    != null) statusLabel.setText("");
    }

    private void linkDiscipline(String artistName, String disciplineName) {
        String sql = "INSERT IGNORE INTO artiste_discipline (id_artiste, id_discipline) " +
                     "SELECT a.id_artiste, d.id_discipline FROM artiste a, discipline d " +
                     "WHERE a.nom=? AND d.nom=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artistName); ps.setString(2, disciplineName); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Erreur liaison discipline: " + e.getMessage()); }
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setStatus(String msg, boolean err) {
        if (statusLabel != null) { statusLabel.setText(msg);
            statusLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;"); }
    }
}
