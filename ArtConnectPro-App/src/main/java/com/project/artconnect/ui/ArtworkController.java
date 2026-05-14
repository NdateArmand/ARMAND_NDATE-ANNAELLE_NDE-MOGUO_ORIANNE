package com.project.artconnect.ui;

import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Session;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;

public class ArtworkController implements RoleAware {

    @FXML private TableView<Artwork>           artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> statusColumn;
    @FXML private GridPane         crudForm;
    @FXML private Label            crudTitle;
    @FXML private HBox             crudBox;
    @FXML private TextField        formTitle;
    @FXML private TextField        formType;
    @FXML private TextField        formMedium;
    @FXML private TextField        formPrice;
    @FXML private TextField        formYear;
    @FXML private ComboBox<String> formStatus;
    @FXML private ComboBox<String> formArtist;
    @FXML private Label            statusLabel;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService  artistService  = ServiceProvider.getArtistService();
    private final JdbcArtworkDao artworkDao     = new JdbcArtworkDao();
    private String originalTitle = null; // titre de la ligne sélectionnée

    @FXML
    public void initialize() {
        titleColumn .setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn  .setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn .setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        artistColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getArtist() != null ? cd.getValue().getArtist().getName() : "Unknown"));

        if (formStatus != null) {
            formStatus.setItems(FXCollections.observableArrayList("FOR_SALE", "SOLD", "EXHIBITED"));
            formStatus.setValue("FOR_SALE");
        }
        if (formArtist != null)
            formArtist.setItems(FXCollections.observableArrayList(
                    artistService.getAllArtists().stream().map(a -> a.getName()).toList()));

        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        clearForm();

        if (session.isPublic() || session.isMembre()) {
            // Lecture seule
            setVisible(crudForm,  false);
            setVisible(crudBox,   false);
            setVisible(crudTitle, false);

        } else if (session.isArtiste()) {
            // ARTISTE : CRUD sur ses œuvres, artiste verrouillé
            setVisible(crudForm,  true);
            setVisible(crudBox,   true);
            setVisible(crudTitle, true);
            if (crudTitle  != null) crudTitle.setText("My Artworks");
            if (formArtist != null) { formArtist.setValue(session.getDisplayName()); formArtist.setDisable(true); }
            artworkTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) fillForm(sel); });

        } else {
            // ORGANISATEUR : CRUD complet
            setVisible(crudForm,  true);
            setVisible(crudBox,   true);
            setVisible(crudTitle, true);
            if (crudTitle  != null) crudTitle.setText("Add / Edit Artwork");
            if (formArtist != null) formArtist.setDisable(false);
            artworkTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) fillForm(sel); });
        }

        refreshTable();
    }

    @FXML private void handleCreate() {
        originalTitle = null; // garantir un INSERT et non un UPDATE
        try { artworkService.createArtwork(buildFromForm());
            setStatus("Artwork added.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleUpdate() {
        Artwork sel = artworkTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select an artwork.", true); return; }
        Session session = Session.getInstance();
        if (session.isArtiste()) {
            String owner = sel.getArtist() != null ? sel.getArtist().getName() : "";
            if (!owner.equals(session.getDisplayName())) {
                setStatus("Vous ne pouvez modifier que vos propres œuvres.", true); return;
            }
        }
        try {
            Artwork a = buildFromForm(); // contient le nouveau titre saisi
            // Passer l'ancien titre au DAO pour le WHERE, le nouveau titre est dans a.getTitle()
            String oldTitle = originalTitle != null ? originalTitle : a.getTitle();
            artworkDao.update(a, oldTitle);
            setStatus("Updated.", false); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleDelete() {
        Artwork sel = artworkTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select an artwork.", true); return; }
        Session session = Session.getInstance();
        if (session.isArtiste()) {
            String owner = sel.getArtist() != null ? sel.getArtist().getName() : "";
            if (!owner.equals(session.getDisplayName())) {
                setStatus("Vous ne pouvez supprimer que vos propres œuvres.", true); return;
            }
        }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { artworkService.deleteArtwork(sel.getTitle());
                    setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML private void handleResetForm() {
        clearForm();
        Session s = Session.getInstance();
        if (s.isArtiste() && formArtist != null) formArtist.setValue(s.getDisplayName());
    }

    private void refreshTable() {
        Session session = Session.getInstance();
        List<Artwork> all = artworkService.getAllArtworks();
        if (session.isArtiste())
            all = all.stream().filter(a -> a.getArtist() != null
                    && a.getArtist().getName().equals(session.getDisplayName())).toList();
        artworkTable.setItems(FXCollections.observableArrayList(all));
    }

    private Artwork buildFromForm() {
        if (formTitle.getText().isBlank()) throw new IllegalArgumentException("Title is required.");
        Artwork a = new Artwork();
        a.setTitle (formTitle .getText().trim());
        a.setType  (formType  != null ? formType .getText().trim() : "");
        a.setMedium(formMedium != null ? formMedium.getText().trim() : "");
        if (formPrice != null && !formPrice.getText().isBlank()) a.setPrice(Double.parseDouble(formPrice.getText().trim()));
        if (formYear  != null && !formYear .getText().isBlank()) a.setCreationYear(Integer.parseInt(formYear.getText().trim()));
        if (formStatus != null && formStatus.getValue() != null) a.setStatus(Artwork.Status.valueOf(formStatus.getValue()));
        Session session = Session.getInstance();
        String artistName = session.isArtiste() ? session.getDisplayName()
                : (formArtist != null ? formArtist.getValue() : null);
        if (artistName != null) { final String n = artistName; artistService.getArtistByName(n).ifPresent(a::setArtist); }
        return a;
    }

    private void fillForm(Artwork a) {
        originalTitle = a.getTitle(); // sauvegarder le titre original
        if (formTitle  != null) formTitle .setText(a.getTitle()  != null ? a.getTitle()  : "");
        if (formType   != null) formType  .setText(a.getType()   != null ? a.getType()   : "");
        if (formMedium != null) formMedium.setText(a.getMedium() != null ? a.getMedium() : "");
        if (formPrice  != null) formPrice .setText(String.valueOf(a.getPrice()));
        if (formYear   != null) formYear  .setText(a.getCreationYear() != null ? a.getCreationYear().toString() : "");
        if (formStatus != null && a.getStatus() != null) formStatus.setValue(a.getStatus().name());
        if (formArtist != null && !Session.getInstance().isArtiste()) {
            // Toujours mettre à jour formArtist (même si null) pour éviter d'afficher le mauvais artiste
            formArtist.setValue(a.getArtist() != null ? a.getArtist().getName() : null);
        }
    }

    private void clearForm() {
        originalTitle = null;
        if (formTitle  != null) formTitle.clear();
        if (formType   != null) formType.clear();
        if (formMedium != null) formMedium.clear();
        if (formPrice  != null) formPrice.clear();
        if (formYear   != null) formYear.clear();
        if (formStatus != null) formStatus.setValue("FOR_SALE");
        if (formArtist != null && !Session.getInstance().isArtiste()) formArtist.setValue(null);
        if (statusLabel != null) statusLabel.setText("");
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setStatus(String msg, boolean err) {
        if (statusLabel != null) { statusLabel.setText(msg);
            statusLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;"); }
    }
}
