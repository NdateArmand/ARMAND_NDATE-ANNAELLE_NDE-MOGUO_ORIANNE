package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Session;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class WorkshopController implements RoleAware {

    @FXML private TableView<Workshop>           workshopTable;
    @FXML private TableColumn<Workshop, String> titleColumn;
    @FXML private TableColumn<Workshop, String> instructorColumn;
    @FXML private TableColumn<Workshop, String> dateColumn;
    @FXML private TableColumn<Workshop, Double> priceColumn;
    @FXML private TableColumn<Workshop, String> levelColumn;
    @FXML private TableColumn<Workshop, Number> durationColumn;
    @FXML private TableColumn<Workshop, Number> inscritsColumn;  // ORGANISATEUR
    @FXML private TableColumn<Workshop, String> statutMembreColumn; // MEMBRE

    // ARTISTE : label inscrits
    @FXML private Label artistInscritsLabel;

    // ORGANISATEUR : panneau liste inscrits
    @FXML private VBox     inscritsPanel;
    @FXML private Label    inscritsPanelTitle;
    @FXML private ListView<String> inscritsListView;

    // MEMBRE : inscription/désinscription
    @FXML private HBox  inscriptionBox;
    @FXML private Button btnDesinscrire;
    @FXML private Label inscriptionLabel;

    // CRUD
    @FXML private GridPane         crudForm;
    @FXML private Label            crudTitle;
    @FXML private HBox             crudBox;
    @FXML private TextField        formTitle;
    @FXML private ComboBox<String> formInstructor;
    @FXML private TextField        formPrice;
    @FXML private TextField        formDate;
    @FXML private ComboBox<String> formLevel;
    @FXML private TextField        formDuration;
    @FXML private TextField        formMaxPart;
    @FXML private TextField        formLocation;
    @FXML private Label            statusLabel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService   artistService   = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn    .setCellValueFactory(new PropertyValueFactory<>("title"));
        priceColumn    .setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn    .setCellValueFactory(new PropertyValueFactory<>("level"));
        durationColumn .setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        dateColumn     .setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDate() != null ? cd.getValue().getDate().format(FMT) : "—"));
        instructorColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getInstructor() != null ? cd.getValue().getInstructor().getName() : "—"));

        // Colonne inscrits — calculée depuis la BDD
        if (inscritsColumn != null)
            inscritsColumn.setCellValueFactory(cd ->
                    new SimpleIntegerProperty(getNbInscrits(cd.getValue().getTitle())));
        if (statutMembreColumn != null)
            statutMembreColumn.setCellValueFactory(cd -> {
                Session s = Session.getInstance();
                if (s.isMembre() && s.getIdMembre() != null)
                    return new SimpleStringProperty(estInscrit(cd.getValue().getTitle(), s.getIdMembre()) ? "✅ Inscrit" : "⬜ Non inscrit");
                return new SimpleStringProperty("");
            });

        if (formLevel != null)
            formLevel.setItems(FXCollections.observableArrayList(
                    "débutant","intermédiaire","avancé","tous niveaux"));
        if (formInstructor != null)
            formInstructor.setItems(FXCollections.observableArrayList(
                    artistService.getAllArtists().stream().map(Artist::getName).toList()));

        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        clearForm();

        // Masquer tout par défaut
        setVisible(artistInscritsLabel,  false);
        setVisible(inscritsPanel,        false);
        setVisible(inscritsColumn,       false);
        setVisible(statutMembreColumn,   false);
        setVisible(inscriptionBox,       false);
        setVisible(crudForm,            false);
        setVisible(crudBox,             false);
        setVisible(crudTitle,           false);

        if (session.isPublic()) {
            // Lecture seule, rien

        } else if (session.isMembre()) {
            // Tableau complet + colonne statut + boutons selon sélection
            setVisible(statutMembreColumn, true);
            setVisible(inscriptionBox, true);
            workshopTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> updateMembreButtons(sel));

        } else if (session.isArtiste()) {
            // CRUD ses ateliers + label inscrits au clic
            setVisible(crudForm,            true);
            setVisible(crudBox,             true);
            setVisible(crudTitle,           true);
            setVisible(artistInscritsLabel, true);
            if (crudTitle     != null) crudTitle.setText("My Workshops");
            if (formInstructor != null) { formInstructor.setValue(session.getDisplayName()); formInstructor.setDisable(true); }
            workshopTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> {
                        if (sel != null) {
                            fillForm(sel);
                            int nb  = getNbInscrits(sel.getTitle());
                            int max = sel.getMaxParticipants();
                            if (artistInscritsLabel != null)
                                artistInscritsLabel.setText("Inscrits : " + nb + " / " + max);
                        }
                    });

        } else {
            // ORGANISATEUR : CRUD + colonne inscrits + panneau liste
            setVisible(crudForm,       true);
            setVisible(crudBox,        true);
            setVisible(crudTitle,      true);
            setVisible(inscritsColumn, true);
            setVisible(inscritsPanel,  true);
            if (crudTitle     != null) crudTitle.setText("Add / Edit Workshop");
            if (formInstructor != null) formInstructor.setDisable(false);
            workshopTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> {
                        if (sel != null) {
                            fillForm(sel);
                            loadInscritsPanel(sel.getTitle(), true);
                        }
                    });
        }

        refreshTable();
    }

    // ── Mise à jour boutons MEMBRE selon atelier sélectionné ──────────────────
    private void updateMembreButtons(Workshop sel) {
        if (sel == null || !Session.getInstance().isMembre()) return;
        Integer idMembre = Session.getInstance().getIdMembre();
        if (idMembre == null) return;
        boolean dejaInscrit = estInscrit(sel.getTitle(), idMembre);
        if (btnDesinscrire != null) setVisible(btnDesinscrire, dejaInscrit);
    }

    // ── Chargement du panneau inscrits (ORGANISATEUR) ─────────────────────────
    private void loadInscritsPanel(String titre, boolean isAtelier) {
        if (inscritsListView == null) return;
        List<String> membres = getMembresInscritsAtelier(titre);
        inscritsListView.setItems(FXCollections.observableArrayList(membres));
        if (inscritsPanelTitle != null)
            inscritsPanelTitle.setText("Membres inscrits (" + membres.size() + ") — " + titre);
    }

    // ── Action MEMBRE : S'inscrire ─────────────────────────────────────────────
    @FXML private void handleInscrire() {
        Workshop sel = workshopTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setInscription("Sélectionnez un atelier.", true); return; }
        Session session = Session.getInstance();
        if (!session.isMembre() || session.getIdMembre() == null) {
            setInscription("Vous devez être connecté comme membre.", true); return;
        }
        if (estInscrit(sel.getTitle(), session.getIdMembre())) {
            setInscription("Déjà inscrit à cet atelier.", false); return;
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            int idAtelier = getIdAtelier(conn, sel.getTitle());
            if (idAtelier < 0) { setInscription("Atelier introuvable.", true); return; }
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inscription (id_membre,id_atelier,date_inscription,nb_places,statut_paie) VALUES(?,?,CURDATE(),1,'en attente')")) {
                ps.setInt(1, session.getIdMembre()); ps.setInt(2, idAtelier);
                ps.executeUpdate(); conn.commit();
                setInscription("Inscription confirmée !", false);
                refreshTable();
                if (btnDesinscrire != null) setVisible(btnDesinscrire, true);
            } catch (SQLException e) { conn.rollback(); setInscription("Erreur : " + e.getMessage(), true); }
        } catch (SQLException e) { setInscription("Erreur : " + e.getMessage(), true); }
    }

    // ── Action MEMBRE : Se désinscrire ─────────────────────────────────────────
    @FXML private void handleDesinscrire() {
        Workshop sel = workshopTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setInscription("Sélectionnez un atelier.", true); return; }
        Session session = Session.getInstance();
        if (session.getIdMembre() == null) return;
        try (Connection conn = ConnectionManager.getConnection()) {
            int idAtelier = getIdAtelier(conn, sel.getTitle());
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE inscription SET statut_paie='annulé' WHERE id_membre=? AND id_atelier=?")) {
                ps.setInt(1, session.getIdMembre()); ps.setInt(2, idAtelier);
                ps.executeUpdate(); conn.commit();
                setInscription("Désinscription effectuée.", false);
                refreshTable();
                if (btnDesinscrire != null) setVisible(btnDesinscrire, false);
            } catch (SQLException e) { conn.rollback(); setInscription("Erreur : " + e.getMessage(), true); }
        } catch (SQLException e) { setInscription("Erreur : " + e.getMessage(), true); }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    @FXML private void handleCreate() {
        try { saveWorkshop(buildFromForm()); setStatus("Workshop added.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }
    @FXML private void handleUpdate() {
        Workshop sel = workshopTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a workshop.", true); return; }
        Session session = Session.getInstance();
        if (session.isArtiste()) {
            String instr = sel.getInstructor() != null ? sel.getInstructor().getName() : "";
            if (!instr.equals(session.getDisplayName())) { setStatus("Vos ateliers uniquement.", true); return; }
        }
        try { updateWorkshop(buildFromForm()); setStatus("Updated.", false); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }
    @FXML private void handleDelete() {
        Workshop sel = workshopTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a workshop.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { deleteWorkshop(sel.getTitle()); setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }
    @FXML private void handleResetForm() {
        clearForm();
        Session s = Session.getInstance();
        if (s.isArtiste() && formInstructor != null) formInstructor.setValue(s.getDisplayName());
        if (artistInscritsLabel != null) artistInscritsLabel.setText("");
    }

    // ── SQL helpers ───────────────────────────────────────────────────────────
    private int getNbInscrits(String titre) {
        String sql = "SELECT COALESCE(SUM(i.nb_places),0) FROM inscription i " +
                     "JOIN atelier a ON a.id_atelier=i.id_atelier " +
                     "WHERE a.titre=? AND i.statut_paie!='annulé'";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("getNbInscrits: " + e.getMessage()); }
        return 0;
    }

    private List<String> getMembresInscritsAtelier(String titre) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT m.nom, i.date_inscription, i.statut_paie " +
                     "FROM inscription i " +
                     "JOIN membre_communaute m ON m.id_membre=i.id_membre " +
                     "JOIN atelier a ON a.id_atelier=i.id_atelier " +
                     "WHERE a.titre=? AND i.statut_paie!='annulé' ORDER BY m.nom";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(rs.getString("nom") + "  —  inscrit le " + rs.getString("date_inscription"));
            }
        } catch (SQLException e) { System.err.println("getMembresInscrits: " + e.getMessage()); }
        return list;
    }

    private boolean estInscrit(String titre, int idMembre) {
        String sql = "SELECT COUNT(*) FROM inscription i JOIN atelier a ON a.id_atelier=i.id_atelier " +
                     "WHERE a.titre=? AND i.id_membre=? AND i.statut_paie!='annulé'";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre); ps.setInt(2, idMembre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (SQLException e) { return false; }
    }

    private int getIdAtelier(Connection conn, String titre) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_atelier FROM atelier WHERE titre=? LIMIT 1")) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("id_atelier") : -1; }
        }
    }

    private void refreshTable() {
        Session session = Session.getInstance();
        List<Workshop> all = workshopService.getAllWorkshops();
        if (session.isArtiste())
            all = all.stream().filter(w -> w.getInstructor() != null
                    && w.getInstructor().getName().equals(session.getDisplayName())).toList();
        // MEMBRE : voir tous les ateliers (colonne statut indique si inscrit)
        workshopTable.setItems(FXCollections.observableArrayList(all));
    }

    private void saveWorkshop(Workshop w) throws SQLException {
        String sql = "INSERT INTO ATELIER (titre,date_atelier,duree,participants_max,prix,description,niveau,lieu) VALUES(?,?,?,?,?,NULL,?,?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setString(1,w.getTitle()); ps.setTimestamp(2,w.getDate()!=null?Timestamp.valueOf(w.getDate()):null);
            ps.setInt(3,w.getDurationMinutes()); ps.setInt(4,w.getMaxParticipants());
            ps.setDouble(5,w.getPrice()); ps.setString(6,w.getLevel()); ps.setString(7,w.getLocation());
            ps.executeUpdate();
            if (w.getInstructor() != null) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        try (PreparedStatement pl = conn.prepareStatement("UPDATE ARTISTE SET id_atelier=? WHERE nom=?")) {
                            pl.setInt(1,keys.getInt(1)); pl.setString(2,w.getInstructor().getName()); pl.executeUpdate();
                        }
                    }
                }
            }
            conn.commit();
        }
    }
    private void updateWorkshop(Workshop w) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE ATELIER SET date_atelier=?,duree=?,participants_max=?,prix=?,niveau=?,lieu=? WHERE titre=?")) {
            conn.setAutoCommit(false);
            ps.setTimestamp(1,w.getDate()!=null?Timestamp.valueOf(w.getDate()):null);
            ps.setInt(2,w.getDurationMinutes()); ps.setInt(3,w.getMaxParticipants());
            ps.setDouble(4,w.getPrice()); ps.setString(5,w.getLevel());
            ps.setString(6,w.getLocation()); ps.setString(7,w.getTitle());
            ps.executeUpdate(); conn.commit();
        }
    }
    private void deleteWorkshop(String title) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM ATELIER WHERE titre=?")) {
            conn.setAutoCommit(false); ps.setString(1,title); ps.executeUpdate(); conn.commit();
        }
    }
    private Workshop buildFromForm() {
        if (formTitle==null||formTitle.getText().isBlank()) throw new IllegalArgumentException("Title required.");
        Workshop w = new Workshop();
        w.setTitle(formTitle.getText().trim());
        w.setLocation(formLocation!=null?formLocation.getText().trim():"");
        w.setLevel(formLevel!=null?formLevel.getValue():null);
        if (formPrice!=null&&!formPrice.getText().isBlank()) w.setPrice(Double.parseDouble(formPrice.getText().trim()));
        if (formMaxPart!=null&&!formMaxPart.getText().isBlank()) w.setMaxParticipants(Integer.parseInt(formMaxPart.getText().trim()));
        if (formDuration!=null&&!formDuration.getText().isBlank()) w.setDurationMinutes(Integer.parseInt(formDuration.getText().trim()));
        if (formDate!=null&&!formDate.getText().isBlank()) {
            try { w.setDate(LocalDateTime.parse(formDate.getText().trim(),FMT)); }
            catch (DateTimeParseException ex) { throw new IllegalArgumentException("Format: YYYY-MM-DD HH:MM"); }
        }
        if (formInstructor!=null&&formInstructor.getValue()!=null)
            w.setInstructor(artistService.getArtistByName(formInstructor.getValue()).orElse(null));
        return w;
    }
    private void fillForm(Workshop w) {
        if (formTitle    !=null) formTitle   .setText(w.getTitle()    !=null?w.getTitle()    :"");
        if (formLocation !=null) formLocation.setText(w.getLocation()!=null?w.getLocation():"");
        if (formPrice    !=null) formPrice   .setText(String.valueOf(w.getPrice()));
        if (formMaxPart  !=null) formMaxPart .setText(String.valueOf(w.getMaxParticipants()));
        if (formDuration !=null) formDuration.setText(String.valueOf(w.getDurationMinutes()));
        if (formDate!=null&&w.getDate()!=null) formDate.setText(w.getDate().format(FMT));
        if (formLevel!=null&&w.getLevel()!=null) formLevel.setValue(w.getLevel());
        if (formInstructor!=null&&w.getInstructor()!=null&&!Session.getInstance().isArtiste())
            formInstructor.setValue(w.getInstructor().getName());
    }
    private void clearForm() {
        if (formTitle   !=null) formTitle.clear();
        if (formLocation!=null) formLocation.clear();
        if (formPrice   !=null) formPrice.clear();
        if (formMaxPart !=null) formMaxPart.clear();
        if (formDuration!=null) formDuration.clear();
        if (formDate    !=null) formDate.clear();
        if (formLevel   !=null) formLevel.setValue(null);
        if (formInstructor!=null&&!Session.getInstance().isArtiste()) formInstructor.setValue(null);
        if (statusLabel !=null) statusLabel.setText("");
        if (artistInscritsLabel!=null) artistInscritsLabel.setText("");
        if (inscritsListView!=null) inscritsListView.getItems().clear();
        if (inscritsPanelTitle!=null) inscritsPanelTitle.setText("");
    }
    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node!=null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setVisible(TableColumn<?,?> col, boolean v) {
        if (col!=null) col.setVisible(v);
    }
    private void setStatus(String msg, boolean err) {
        if (statusLabel!=null) { statusLabel.setText(msg);
            statusLabel.setStyle(err?"-fx-text-fill:red;":"-fx-text-fill:green;"); }
    }
    private void setInscription(String msg, boolean err) {
        if (inscriptionLabel!=null) { inscriptionLabel.setText(msg);
            inscriptionLabel.setStyle(err?"-fx-text-fill:red;":"-fx-text-fill:green;"); }
    }
}
