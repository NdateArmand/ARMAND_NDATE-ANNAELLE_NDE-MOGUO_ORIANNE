package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Session;
import com.project.artconnect.service.GalleryService;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class GalleryController implements RoleAware {

    // Tableau
    @FXML private TableView<Gallery>           galleryTable;
    @FXML private TableColumn<Gallery, String> nameColumn;
    @FXML private TableColumn<Gallery, String> addressColumn;
    @FXML private TableColumn<Gallery, String> ownerColumn;
    @FXML private TableColumn<Gallery, String> hoursColumn;
    @FXML private TableColumn<Gallery, String> phoneColumn;

    // Panneau expositions (tous rôles)
    @FXML private Label            detailLabel;
    @FXML private ListView<String> exhibitionList;

    // CRUD — ORGANISATEUR uniquement
    @FXML private GridPane   crudForm;
    @FXML private Label      crudTitle;
    @FXML private HBox       crudBox;
    @FXML private TextField  formName;
    @FXML private TextField  formOwner;
    @FXML private TextField  formAddress;
    @FXML private TextField  formPhone;
    @FXML private TextField  formOpening;
    @FXML private TextField  formClosing;
    @FXML private Label      statusLabel;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn   .setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        ownerColumn  .setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        phoneColumn  .setCellValueFactory(new PropertyValueFactory<>("contactPhone"));
        hoursColumn  .setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOpeningHours() != null ? cd.getValue().getOpeningHours() : "—"));

        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();

        if (session.isOrganisateur()) {
            setVisible(crudForm,  true);
            setVisible(crudBox,   true);
            setVisible(crudTitle, true);
            galleryTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) { fillForm(sel); loadExhibitions(sel); } });
        } else {
            setVisible(crudForm,  false);
            setVisible(crudBox,   false);
            setVisible(crudTitle, false);
            galleryTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) loadExhibitions(sel); });
        }

        refreshTable();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    @FXML private void handleCreate() {
        try {
            Gallery g = buildFromForm();
            saveGallery(g);
            setStatus("Gallery \"" + g.getName() + "\" added.", false);
            clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleUpdate() {
        Gallery sel = galleryTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a gallery to update.", true); return; }
        try {
            Gallery g = buildFromForm();
            updateGallery(g, sel.getName());
            setStatus("Gallery updated.", false); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleDelete() {
        Gallery sel = galleryTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a gallery to delete.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION,
                "Delete gallery \"" + sel.getName() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    deleteGallery(sel.getName());
                    setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML private void handleResetForm() { clearForm(); }

    // ── Expositions au clic sur une galerie ───────────────────────────────────
    private void loadExhibitions(Gallery gallery) {
        List<Exhibition> expos = galleryService.getExhibitionsByGallery(gallery);
        if (exhibitionList != null) {
            exhibitionList.setItems(FXCollections.observableArrayList(
                    expos.stream().map(e ->
                            e.getTitle() + "  (" + e.getStartDate() + " → " + e.getEndDate() + ")"
                    ).toList()));
        }
        if (detailLabel != null)
            detailLabel.setText(expos.isEmpty()
                    ? "No exhibitions for this gallery."
                    : expos.size() + " exhibition(s) — " + gallery.getName());
    }

    // ── SQL helpers ───────────────────────────────────────────────────────────
    private void saveGallery(Gallery g) throws SQLException {
        String sql = "INSERT INTO galerie (nom, adresse, nom_proprietaire, " +
                     "heure_ouverture, heure_fermeture) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, g.getName());
            ps.setString(2, g.getAddress());
            ps.setString(3, g.getOwnerName());
            ps.setString(4, formatTime(formOpening != null ? formOpening.getText().trim() : ""));
            ps.setString(5, formatTime(formClosing != null ? formClosing.getText().trim() : ""));
            ps.executeUpdate();
            conn.commit();
        }
    }

    private void updateGallery(Gallery g, String oldName) throws SQLException {
        String sql = "UPDATE galerie SET nom=?, adresse=?, nom_proprietaire=?, " +
                     "heure_ouverture=?, heure_fermeture=? WHERE nom=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setString(1, g.getName());
            ps.setString(2, g.getAddress());
            ps.setString(3, g.getOwnerName());
            ps.setString(4, formatTime(formOpening != null ? formOpening.getText().trim() : ""));
            ps.setString(5, formatTime(formClosing != null ? formClosing.getText().trim() : ""));
            ps.setString(6, oldName);
            ps.executeUpdate();
            conn.commit();
        }
    }

    private void deleteGallery(String name) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM galerie WHERE nom=?")) {
            conn.setAutoCommit(false);
            ps.setString(1, name);
            ps.executeUpdate();
            conn.commit();
        }
    }

    private void refreshTable() {
        galleryTable.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
        if (detailLabel  != null) detailLabel.setText("");
        if (exhibitionList != null) exhibitionList.getItems().clear();
    }

    private Gallery buildFromForm() {
        if (formName == null || formName.getText().isBlank())
            throw new IllegalArgumentException("Name is required.");
        Gallery g = new Gallery();
        g.setName        (formName   .getText().trim());
        g.setAddress     (formAddress != null ? formAddress.getText().trim() : "");
        g.setOwnerName   (formOwner   != null ? formOwner  .getText().trim() : "");
        g.setContactPhone(formPhone   != null ? formPhone  .getText().trim() : "");
        // Horaires : "09:00 - 19:00"
        String open  = formOpening != null ? formOpening.getText().trim() : "";
        String close = formClosing != null ? formClosing.getText().trim() : "";
        if (!open.isBlank() || !close.isBlank())
            g.setOpeningHours(open + " - " + close);
        return g;
    }

    private void fillForm(Gallery g) {
        if (formName    != null) formName   .setText(g.getName()         != null ? g.getName()         : "");
        if (formAddress != null) formAddress.setText(g.getAddress()      != null ? g.getAddress()      : "");
        if (formOwner   != null) formOwner  .setText(g.getOwnerName()    != null ? g.getOwnerName()    : "");
        if (formPhone   != null) formPhone  .setText(g.getContactPhone() != null ? g.getContactPhone() : "");
        // Décomposer "09:00 - 19:00"
        if (g.getOpeningHours() != null && g.getOpeningHours().contains(" - ")) {
            String[] parts = g.getOpeningHours().split(" - ", 2);
            if (formOpening != null) formOpening.setText(parts[0]);
            if (formClosing != null) formClosing.setText(parts[1]);
        }
    }

    /** Convertit "HH:MM" en "HH:MM:SS" pour MySQL TIME */
    private String formatTime(String t) {
        if (t == null || t.isBlank()) return null;
        return t.matches("\\d{2}:\\d{2}") ? t + ":00" : t;
    }

    private void clearForm() {
        if (formName    != null) formName.clear();
        if (formAddress != null) formAddress.clear();
        if (formOwner   != null) formOwner.clear();
        if (formPhone   != null) formPhone.clear();
        if (formOpening != null) formOpening.clear();
        if (formClosing != null) formClosing.clear();
        if (statusLabel != null) statusLabel.setText("");
    }

    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }

    private void setStatus(String msg, boolean err) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle(err ? "-fx-text-fill:red;" : "-fx-text-fill:green;");
        }
    }
}
