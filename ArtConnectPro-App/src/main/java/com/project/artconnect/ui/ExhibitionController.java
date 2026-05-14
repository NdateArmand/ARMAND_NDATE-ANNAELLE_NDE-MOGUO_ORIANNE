package com.project.artconnect.ui;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.impl.DaoFactory;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Session;
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

    @FXML private TableView<Exhibition>              exhibitionTable;
    @FXML private TableColumn<Exhibition, String>    titleColumn;
    @FXML private TableColumn<Exhibition, String>    galleryColumn;
    @FXML private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML private TableColumn<Exhibition, String>    themeColumn;
    @FXML private TableColumn<Exhibition, Number>    reservesColumn;     // ORGANISATEUR
    @FXML private TableColumn<Exhibition, String>    statutMembreColumn; // MEMBRE

    // ORGANISATEUR : panneau réservations
    @FXML private VBox     reservesPanel;
    @FXML private Label    reservesPanelTitle;
    @FXML private ListView<String> reservesListView;

    // MEMBRE
    @FXML private HBox   reservationBox;
    @FXML private Button btnAnnulerReservation;
    @FXML private Label  reservationLabel;

    // CRUD
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

    private final ExhibitionDao  exhibitionDao  = DaoFactory.getExhibitionDao();
    private final GalleryService galleryService = ServiceProvider.getGalleryService();
    private String originalTitle = null;

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
                    return new SimpleStringProperty(aReserve(cd.getValue().getTitle(), s.getIdMembre()) ? "✅ Réservé" : "⬜ Non réservé");
                return new SimpleStringProperty("");
            });
        if (formGallery != null)
            formGallery.setItems(FXCollections.observableArrayList(
                    galleryService.getAllGalleries().stream().map(Gallery::getName).toList()));
        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        clearForm();

        setVisible(reservationBox,     false);
        setVisible(reservesPanel,      false);
        setVisible(reservesColumn,     false);
        setVisible(statutMembreColumn, false);
        setVisible(crudForm,        false);
        setVisible(crudBox,         false);
        setVisible(crudTitle,       false);

        if (session.isMembre()) {
            setVisible(statutMembreColumn, true);
            setVisible(reservationBox, true);
            exhibitionTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> updateMembreButtons(sel));

        } else if (session.isArtiste() || session.isPublic()) {
            // Lecture seule

        } else {
            // ORGANISATEUR
            setVisible(crudForm,       true);
            setVisible(crudBox,        true);
            setVisible(crudTitle,      true);
            setVisible(reservesPanel,  true);
            setVisible(reservesColumn, true);
            exhibitionTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> {
                        if (sel != null) { fillForm(sel); loadReservesPanel(sel.getTitle()); }
                    });
        }

        refreshTable();
    }

    // ── Boutons MEMBRE selon sélection ────────────────────────────────────────
    private void updateMembreButtons(Exhibition sel) {
        if (sel == null || !Session.getInstance().isMembre()) return;
        Integer idMembre = Session.getInstance().getIdMembre();
        if (idMembre == null) return;
        boolean dejaReserve = aReserve(sel.getTitle(), idMembre);
        if (btnAnnulerReservation != null) setVisible(btnAnnulerReservation, dejaReserve);
    }

    // ── Panneau réservations ORGANISATEUR ────────────────────────────────────
    private void loadReservesPanel(String titre) {
        if (reservesListView == null) return;
        List<String> membres = getMembresReserves(titre);
        reservesListView.setItems(FXCollections.observableArrayList(membres));
        if (reservesPanelTitle != null)
            reservesPanelTitle.setText("Membres ayant réservé (" + membres.size() + ") — " + titre);
    }

    // ── MEMBRE : Réserver ─────────────────────────────────────────────────────
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

    // ── MEMBRE : Annuler réservation ─────────────────────────────────────────
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

    // ── CRUD ──────────────────────────────────────────────────────────────────
    @FXML private void handleCreate() {
        originalTitle = null; // garantir un INSERT
        try { saveWithGallery(buildFromForm()); setStatus("Exhibition added.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }
    @FXML private void handleUpdate() {
        if (exhibitionTable.getSelectionModel().getSelectedItem() == null) { setStatus("Select.", true); return; }
        try {
            Exhibition e = buildFromForm(); // contient le nouveau titre
            String oldTitle = originalTitle != null ? originalTitle : e.getTitle();
            exhibitionDao.update(e, oldTitle);
            // Mettre à jour la galerie liée si sélectionnée
            updateGalleryLink(e.getTitle());
            setStatus("Updated.", false); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }
    @FXML private void handleDelete() {
        Exhibition sel = exhibitionTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel.getTitle() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { exhibitionDao.delete(sel.getTitle()); setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }
    @FXML private void handleResetForm() { clearForm(); }

    // ── SQL helpers ───────────────────────────────────────────────────────────
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
        Session session = Session.getInstance();
        List<Exhibition> all = exhibitionDao.findAll();
        // MEMBRE : voir toutes les expositions (colonne statut indique si réservé)
        exhibitionTable.setItems(FXCollections.observableArrayList(all));
    }

    /** Met à jour la galerie liée à une exposition existante */
    private void updateGalleryLink(String titre) throws SQLException {
        if (formGallery == null || formGallery.getValue() == null) return;
        try (Connection conn = ConnectionManager.getConnection()) {
            // Récupérer l'id de l'exposition par son titre (le nouveau titre après update)
            int idExpo;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_exposition FROM exposition WHERE titre=? LIMIT 1")) {
                ps.setString(1, titre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    idExpo = rs.getInt("id_exposition");
                }
            }
            conn.setAutoCommit(false);
            // Délier l'ancienne galerie de cette exposition
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE galerie SET id_exposition=NULL WHERE id_exposition=?")) {
                ps.setInt(1, idExpo); ps.executeUpdate();
            }
            // Lier la nouvelle galerie sélectionnée
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE galerie SET id_exposition=? WHERE nom=?")) {
                ps.setInt(1, idExpo); ps.setString(2, formGallery.getValue());
                ps.executeUpdate();
            }
            conn.commit();
        }
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
        if (formTitle==null||formTitle.getText().isBlank()) throw new IllegalArgumentException("Title required.");
        Exhibition e = new Exhibition();
        e.setTitle(formTitle.getText().trim());
        e.setTheme(formTheme!=null?formTheme.getText().trim():"");
        e.setDescription(formDescription!=null?formDescription.getText().trim():"");
        try {
            if (formStartDate!=null) e.setStartDate(LocalDate.parse(formStartDate.getText().trim()));
            if (formEndDate  !=null) e.setEndDate  (LocalDate.parse(formEndDate  .getText().trim()));
        } catch (DateTimeParseException ex) { throw new IllegalArgumentException("Dates : YYYY-MM-DD"); }
        return e;
    }
    private void fillForm(Exhibition e) {
        originalTitle = e.getTitle();
        if (formTitle      !=null) formTitle      .setText(e.getTitle()      !=null?e.getTitle()      :"");
        if (formTheme      !=null) formTheme      .setText(e.getTheme()      !=null?e.getTheme()      :"");
        if (formDescription!=null) formDescription.setText(e.getDescription()!=null?e.getDescription():"");
        if (formStartDate  !=null) formStartDate  .setText(e.getStartDate()  !=null?e.getStartDate().toString():"");
        if (formEndDate    !=null) formEndDate    .setText(e.getEndDate()    !=null?e.getEndDate().toString():"");
        if (formGallery!=null&&e.getGallery()!=null) formGallery.setValue(e.getGallery().getName());
    }
    private void clearForm() {
        originalTitle = null;
        if (formTitle      !=null) formTitle.clear();
        if (formTheme      !=null) formTheme.clear();
        if (formDescription!=null) formDescription.clear();
        if (formStartDate  !=null) formStartDate.clear();
        if (formEndDate    !=null) formEndDate.clear();
        if (formGallery    !=null) formGallery.setValue(null);
        if (statusLabel    !=null) statusLabel.setText("");
        if (reservesListView !=null) reservesListView.getItems().clear();
        if (reservesPanelTitle!=null) reservesPanelTitle.setText("");
    }
    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node!=null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setVisible(TableColumn<?,?> col, boolean v) { if (col!=null) col.setVisible(v); }
    private void setStatus(String msg, boolean err) {
        if (statusLabel!=null) { statusLabel.setText(msg);
            statusLabel.setStyle(err?"-fx-text-fill:red;":"-fx-text-fill:green;"); }
    }
    private void setReservation(String msg, boolean err) {
        if (reservationLabel!=null) { reservationLabel.setText(msg);
            reservationLabel.setStyle(err?"-fx-text-fill:red;":"-fx-text-fill:green;"); }
    }
}
