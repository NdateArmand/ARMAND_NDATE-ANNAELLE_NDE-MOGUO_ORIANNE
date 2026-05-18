package com.project.artconnect.ui;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.ExposerDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Session;
import com.project.artconnect.persistence.JdbcExposerDao;
import com.project.artconnect.service.GalleryService;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ExhibitionController implements RoleAware {

    // ── Table expositions ─────────────────────────────────────────────────────
    @FXML private TableView<Exhibition>              exhibitionTable;
    @FXML private TableColumn<Exhibition, String>    titleColumn;
    @FXML private TableColumn<Exhibition, String>    galleryColumn;
    @FXML private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML private TableColumn<Exhibition, String>    themeColumn;
    @FXML private TableColumn<Exhibition, Number>    reservesColumn;
    @FXML private TableColumn<Exhibition, String>    statutMembreColumn;

    // ── Panneau réservations ORGANISATEUR ────────────────────────────────────
    @FXML private VBox     reservesPanel;
    @FXML private Label    reservesPanelTitle;
    @FXML private ListView<String> reservesListView;

    // ── MEMBRE ────────────────────────────────────────────────────────────────
    @FXML private HBox   reservationBox;
    @FXML private Button btnAnnulerReservation;
    @FXML private Label  reservationLabel;

    // ── CRUD exposition ───────────────────────────────────────────────────────
    @FXML private GridPane        crudForm;
    @FXML private Label           crudTitle;
    @FXML private HBox            crudBox;
    @FXML private TextField       formTitle;
    @FXML private TextField       formTheme;
    @FXML private ComboBox<String> formGallery;
    @FXML private TextField       formStartDate;
    @FXML private TextField       formEndDate;
    @FXML private TextField       formDescription;
    @FXML private Label           statusLabel;

    // ── Panneau gestion des œuvres (ORGANISATEUR) ─────────────────────────────
    /** Conteneur principal du panneau œuvres — à déclarer dans le FXML */
    @FXML private VBox artworksPanel;
    /** Label titre du panneau (ex : "Œuvres — Mon Exposition") */
    @FXML private Label artworksPanelTitle;
    /** Liste de gauche : œuvres disponibles (non encore dans l'expo) */
    @FXML private ListView<Artwork> availableArtworksList;
    /** Liste de droite : œuvres actuellement dans l'expo */
    @FXML private ListView<Artwork> exhibitedArtworksList;
    /** Champ optionnel pour saisir l'emplacement avant d'ajouter */
    @FXML private TextField emplacementField;
    /** Label de statut propre au panneau œuvres */
    @FXML private Label artworksStatusLabel;

    // ── DAO & Services ────────────────────────────────────────────────────────
    private final ExhibitionDao  exhibitionDao  = DaoFactory.getExhibitionDao();
    private final GalleryService galleryService = ServiceProvider.getGalleryService();
    private final ExposerDao     exposerDao     = new JdbcExposerDao();

    private String originalTitle      = null;
    /** Titre de l'exposition sélectionnée, utilisé par le panneau œuvres */
    private String selectedExpoTitle  = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        titleColumn  .setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn   .setCellValueFactory(new PropertyValueFactory<>("startDate"));
        themeColumn  .setCellValueFactory(new PropertyValueFactory<>("theme"));
        galleryColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getGallery() != null ? cd.getValue().getGallery().getName() : "—"));

        if (reservesColumn != null)
            reservesColumn.setCellValueFactory(cd ->
                    new SimpleIntegerProperty(getNbReservations(cd.getValue().getTitle())));

        if (statutMembreColumn != null)
            statutMembreColumn.setCellValueFactory(cd -> {
                Session s = Session.getInstance();
                if (s.isMembre() && s.getIdMembre() != null)
                    return new SimpleStringProperty(
                            aReserve(cd.getValue().getTitle(), s.getIdMembre()) ? "✅ Réservé" : "⬜ Non réservé");
                return new SimpleStringProperty("");
            });

        if (formGallery != null)
            formGallery.setItems(FXCollections.observableArrayList(
                    galleryService.getAllGalleries().stream().map(Gallery::getName).toList()));

        // Rendre chaque item des ListViews d'œuvres lisible
        configurerCellFactory(availableArtworksList);
        configurerCellFactory(exhibitedArtworksList);

        applyRole();
    }

    /** Affiche "Titre — Artiste (Type)" dans les ListView<Artwork>. */
    private void configurerCellFactory(ListView<Artwork> lv) {
        if (lv == null) return;
        lv.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Artwork a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); return; }
                String artiste = a.getArtist() != null ? a.getArtist().getName() : "Artiste inconnu";
                String type    = a.getType()   != null ? a.getType()             : "";
                setText(a.getTitle() + "  —  " + artiste + (type.isBlank() ? "" : "  (" + type + ")"));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestion des rôles
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        clearForm();

        setVisible(reservationBox,     false);
        setVisible(reservesPanel,      false);
        setVisible(reservesColumn,     false);
        setVisible(statutMembreColumn, false);
        setVisible(crudForm,           false);
        setVisible(crudBox,            false);
        setVisible(crudTitle,          false);
        setVisible(artworksPanel,      false);   // panneau œuvres masqué par défaut

        if (session.isMembre()) {
            setVisible(statutMembreColumn, true);
            setVisible(reservationBox, true);
            exhibitionTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> updateMembreButtons(sel));

        } else if (session.isArtiste() || session.isPublic()) {
            // Lecture seule

        } else {
            // ── ORGANISATEUR ──────────────────────────────────────────────
            setVisible(crudForm,       true);
            setVisible(crudBox,        true);
            setVisible(crudTitle,      true);
            setVisible(reservesPanel,  true);
            setVisible(reservesColumn, true);
            setVisible(artworksPanel,  true);    // panneau œuvres visible

            exhibitionTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> {
                        if (sel != null) {
                            fillForm(sel);
                            loadReservesPanel(sel.getTitle());
                            loadArtworksPanel(sel.getTitle()); // ← nouveau
                        }
                    });
        }

        refreshTable();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panneau œuvres — ORGANISATEUR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge les deux listes du panneau œuvres pour l'exposition donnée.
     * Appelé automatiquement à chaque sélection d'une exposition.
     */
    private void loadArtworksPanel(String expoTitle) {
        selectedExpoTitle = expoTitle;
        if (artworksPanelTitle != null)
            artworksPanelTitle.setText("Œuvres — " + expoTitle);
        if (artworksStatusLabel != null)
            artworksStatusLabel.setText("");
        refreshArtworksLists();
    }

    /** Rafraîchit les deux listes sans changer l'exposition sélectionnée. */
    private void refreshArtworksLists() {
        if (selectedExpoTitle == null) return;
        try {
            List<Artwork> dansExpo    = exposerDao.findArtworksByExhibition(selectedExpoTitle);
            List<Artwork> horsExpo    = exposerDao.findArtworksNotInExhibition(selectedExpoTitle);
            if (availableArtworksList != null)
                availableArtworksList.setItems(FXCollections.observableArrayList(horsExpo));
            if (exhibitedArtworksList != null)
                exhibitedArtworksList.setItems(FXCollections.observableArrayList(dansExpo));
        } catch (Exception e) {
            setArtworksStatus("Erreur chargement œuvres : " + e.getMessage(), true);
        }
    }

    /**
     * Bouton "→ Ajouter à l'exposition".
     * Prend l'œuvre sélectionnée dans availableArtworksList et l'insère dans EXPOSER.
     */
    @FXML
    private void handleAddArtworkToExhibition() {
        if (selectedExpoTitle == null) {
            setArtworksStatus("Sélectionnez d'abord une exposition.", true); return;
        }
        Artwork sel = availableArtworksList != null
                ? availableArtworksList.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            setArtworksStatus("Sélectionnez une œuvre dans la liste de gauche.", true); return;
        }
        String emplacement = emplacementField != null ? emplacementField.getText().trim() : null;
        if (emplacement != null && emplacement.isBlank()) emplacement = null;
        try {
            exposerDao.addArtworkToExhibition(selectedExpoTitle, sel.getId(), emplacement);
            setArtworksStatus("\"" + sel.getTitle() + "\" ajoutée à l'exposition.", false);
            if (emplacementField != null) emplacementField.clear();
            refreshArtworksLists();
        } catch (Exception e) {
            setArtworksStatus("Erreur : " + e.getMessage(), true);
        }
    }

    /**
     * Bouton "← Retirer de l'exposition".
     * Prend l'œuvre sélectionnée dans exhibitedArtworksList et la supprime de EXPOSER.
     */
    @FXML
    private void handleRemoveArtworkFromExhibition() {
        if (selectedExpoTitle == null) {
            setArtworksStatus("Sélectionnez d'abord une exposition.", true); return;
        }
        Artwork sel = exhibitedArtworksList != null
                ? exhibitedArtworksList.getSelectionModel().getSelectedItem() : null;
        if (sel == null) {
            setArtworksStatus("Sélectionnez une œuvre dans la liste de droite.", true); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Retirer \"" + sel.getTitle() + "\" de l'exposition ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    exposerDao.removeArtworkFromExhibition(selectedExpoTitle, sel.getId());
                    setArtworksStatus("\"" + sel.getTitle() + "\" retirée de l'exposition.", false);
                    refreshArtworksLists();
                } catch (Exception e) {
                    setArtworksStatus("Erreur : " + e.getMessage(), true);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Réservations MEMBRE
    // ─────────────────────────────────────────────────────────────────────────

    private void updateMembreButtons(Exhibition sel) {
        if (sel == null || !Session.getInstance().isMembre()) return;
        Integer idMembre = Session.getInstance().getIdMembre();
        if (idMembre == null) return;
        boolean dejaReserve = aReserve(sel.getTitle(), idMembre);
        if (btnAnnulerReservation != null) setVisible(btnAnnulerReservation, dejaReserve);
    }

    private void loadReservesPanel(String titre) {
        if (reservesListView == null) return;
        List<String> membres = getMembresReserves(titre);
        reservesListView.setItems(FXCollections.observableArrayList(membres));
        if (reservesPanelTitle != null)
            reservesPanelTitle.setText("Membres ayant réservé (" + membres.size() + ") — " + titre);
    }

    @FXML private void handleReserver() {
        Exhibition sel = exhibitionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setReservation("Sélectionnez une exposition.", true); return; }
        Session session = Session.getInstance();
        if (!session.isMembre() || session.getIdMembre() == null) {
            setReservation("Vous devez être connecté comme membre.", true); return;
        }
        if (aReserve(sel.getTitle(), session.getIdMembre())) {
            setReservation("Vous avez déjà réservé cette exposition.", false); return;
        }
        try (Connection conn = ConnectionManager.getConnection()) {
            int idExpo = getIdExpo(conn, sel.getTitle());
            if (idExpo < 0) { setReservation("Exposition introuvable.", true); return; }
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO reservation (id_exposition,id_membre,date,est_present) VALUES(?,?,CURDATE(),FALSE)")) {
                ps.setInt(1, idExpo); ps.setInt(2, session.getIdMembre());
                ps.executeUpdate(); conn.commit();
                setReservation("Réservation confirmée !", false);
                refreshTable();
                if (btnAnnulerReservation != null) setVisible(btnAnnulerReservation, true);
            } catch (SQLException e) { conn.rollback(); setReservation("Erreur : " + e.getMessage(), true); }
        } catch (SQLException e) { setReservation("Erreur : " + e.getMessage(), true); }
    }

    @FXML private void handleAnnulerReservation() {
        Exhibition sel = exhibitionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setReservation("Sélectionnez une exposition.", true); return; }
        Session session = Session.getInstance();
        if (session.getIdMembre() == null) return;
        try (Connection conn = ConnectionManager.getConnection()) {
            int idExpo = getIdExpo(conn, sel.getTitle());
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM reservation WHERE id_exposition=? AND id_membre=?")) {
                ps.setInt(1, idExpo); ps.setInt(2, session.getIdMembre());
                ps.executeUpdate(); conn.commit();
                setReservation("Réservation annulée.", false);
                refreshTable();
                if (btnAnnulerReservation != null) setVisible(btnAnnulerReservation, false);
            } catch (SQLException e) { conn.rollback(); setReservation("Erreur : " + e.getMessage(), true); }
        } catch (SQLException e) { setReservation("Erreur : " + e.getMessage(), true); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD exposition
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleCreate() {
        originalTitle = null;
        try { saveWithGallery(buildFromForm()); setStatus("Exposition ajoutée.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Erreur : " + e.getMessage(), true); }
    }

    @FXML private void handleUpdate() {
        if (exhibitionTable.getSelectionModel().getSelectedItem() == null) { setStatus("Sélectionnez une exposition.", true); return; }
        try {
            Exhibition e = buildFromForm();
            if (originalTitle != null) e.setTitle(originalTitle);
            exhibitionDao.update(e); setStatus("Mise à jour effectuée.", false); refreshTable();
        } catch (Exception e) { setStatus("Erreur : " + e.getMessage(), true); }
    }

    @FXML private void handleDelete() {
        Exhibition sel = exhibitionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Sélectionnez une exposition.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Supprimer \"" + sel.getTitle() + "\" ?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    exhibitionDao.delete(sel.getTitle());
                    setStatus("Exposition supprimée.", false); clearForm(); refreshTable();
                    // Vider le panneau œuvres
                    selectedExpoTitle = null;
                    if (availableArtworksList != null) availableArtworksList.getItems().clear();
                    if (exhibitedArtworksList != null) exhibitedArtworksList.getItems().clear();
                    if (artworksPanelTitle   != null) artworksPanelTitle.setText("Sélectionnez une exposition");
                } catch (Exception e) { setStatus("Erreur : " + e.getMessage(), true); }
            }
        });
    }

    @FXML private void handleResetForm() { clearForm(); }

    // ─────────────────────────────────────────────────────────────────────────
    // SQL helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int getNbReservations(String titre) {
        String sql = "SELECT COUNT(*) FROM reservation r JOIN exposition e ON e.id_exposition=r.id_exposition WHERE e.titre=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("getNbReservations: " + e.getMessage()); }
        return 0;
    }

    private List<String> getMembresReserves(String titre) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT m.nom, r.date FROM reservation r " +
                     "JOIN membre_communaute m ON m.id_membre=r.id_membre " +
                     "JOIN exposition e ON e.id_exposition=r.id_exposition " +
                     "WHERE e.titre=? ORDER BY m.nom";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(rs.getString("nom") + "  —  réservé le " + rs.getString("date"));
            }
        } catch (SQLException e) { System.err.println("getMembresReserves: " + e.getMessage()); }
        return list;
    }

    private boolean aReserve(String titre, int idMembre) {
        String sql = "SELECT COUNT(*) FROM reservation r JOIN exposition e ON e.id_exposition=r.id_exposition " +
                     "WHERE e.titre=? AND r.id_membre=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titre); ps.setInt(2, idMembre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (SQLException e) { return false; }
    }

    private int getIdExpo(Connection conn, String titre) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_exposition FROM exposition WHERE titre=? LIMIT 1")) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt("id_exposition") : -1; }
        }
    }

    private void refreshTable() {
        exhibitionTable.setItems(FXCollections.observableArrayList(exhibitionDao.findAll()));
    }

    private void saveWithGallery(Exhibition e) throws SQLException {
        exhibitionDao.save(e);
        if (formGallery != null && formGallery.getValue() != null) {
            try (Connection conn = ConnectionManager.getConnection()) {
                int idExpo;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id_exposition FROM exposition WHERE titre=? ORDER BY id_exposition DESC LIMIT 1")) {
                    ps.setString(1, e.getTitle());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return; idExpo = rs.getInt("id_exposition");
                    }
                }
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement("UPDATE galerie SET id_exposition=? WHERE nom=?")) {
                    ps.setInt(1, idExpo); ps.setString(2, formGallery.getValue());
                    ps.executeUpdate(); conn.commit();
                }
            }
        }
    }

    private Exhibition buildFromForm() {
        if (formTitle == null || formTitle.getText().isBlank())
            throw new IllegalArgumentException("Le titre est obligatoire.");
        Exhibition e = new Exhibition();
        e.setTitle      (formTitle.getText().trim());
        e.setTheme      (formTheme       != null ? formTheme.getText().trim()       : "");
        e.setDescription(formDescription != null ? formDescription.getText().trim() : "");
        try {
            if (formStartDate != null) e.setStartDate(LocalDate.parse(formStartDate.getText().trim()));
            if (formEndDate   != null) e.setEndDate  (LocalDate.parse(formEndDate.getText().trim()));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates : format YYYY-MM-DD requis.");
        }
        return e;
    }

    private void fillForm(Exhibition e) {
        originalTitle = e.getTitle();
        if (formTitle       != null) formTitle      .setText(e.getTitle()       != null ? e.getTitle()       : "");
        if (formTheme       != null) formTheme      .setText(e.getTheme()       != null ? e.getTheme()       : "");
        if (formDescription != null) formDescription.setText(e.getDescription() != null ? e.getDescription() : "");
        if (formStartDate   != null) formStartDate  .setText(e.getStartDate()   != null ? e.getStartDate().toString() : "");
        if (formEndDate     != null) formEndDate    .setText(e.getEndDate()     != null ? e.getEndDate().toString()   : "");
        if (formGallery != null && e.getGallery() != null) formGallery.setValue(e.getGallery().getName());
    }

    private void clearForm() {
        originalTitle = null;
        if (formTitle       != null) formTitle.clear();
        if (formTheme       != null) formTheme.clear();
        if (formDescription != null) formDescription.clear();
        if (formStartDate   != null) formStartDate.clear();
        if (formEndDate     != null) formEndDate.clear();
        if (formGallery     != null) formGallery.setValue(null);
        if (statusLabel     != null) statusLabel.setText("");
        if (reservesListView   != null) reservesListView.getItems().clear();
        if (reservesPanelTitle != null) reservesPanelTitle.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers UI
    // ─────────────────────────────────────────────────────────────────────────

    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setVisible(TableColumn<?, ?> col, boolean v) { if (col != null) col.setVisible(v); }

    private void setStatus(String msg, boolean err) {
        if (statusLabel != null) { statusLabel.setText(msg);
            statusLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;"); }
    }
    private void setReservation(String msg, boolean err) {
        if (reservationLabel != null) { reservationLabel.setText(msg);
            reservationLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;"); }
    }
    private void setArtworksStatus(String msg, boolean err) {
        if (artworksStatusLabel != null) { artworksStatusLabel.setText(msg);
            artworksStatusLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;"); }
    }
}
