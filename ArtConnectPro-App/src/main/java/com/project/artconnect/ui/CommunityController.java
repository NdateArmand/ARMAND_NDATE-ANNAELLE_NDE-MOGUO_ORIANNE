package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Session;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class CommunityController implements RoleAware {

    @FXML private TableView<CommunityMember>           memberTable;
    @FXML private TableColumn<CommunityMember, String> nameColumn;
    @FXML private TableColumn<CommunityMember, String> emailColumn;
    @FXML private TableColumn<CommunityMember, String> cityColumn;
    @FXML private TableColumn<CommunityMember, String> membershipColumn;
    @FXML private GridPane        crudForm;
    @FXML private Label           crudTitle;
    @FXML private HBox            crudBox;
    @FXML private Button          btnAdd;
    @FXML private Button          btnDelete;
    @FXML private TextField       formName;
    @FXML private TextField       formEmail;
    @FXML private TextField       formCity;
    @FXML private ComboBox<String> formMembership;
    @FXML private TextField       formPhone;
    @FXML private TextField       formYear;
    @FXML private Label           statusLabel;

    private final CommunityService communityService = ServiceProvider.getCommunityService();
    private String originalName = null; // nom original avant modification

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        if (membershipColumn != null)
            membershipColumn.setCellValueFactory(new PropertyValueFactory<>("membershipType"));
        if (formMembership != null)
            formMembership.setItems(FXCollections.observableArrayList("standard","premium","étudiant"));
        applyRole();
    }

    @Override
    public void applyRole() {
        Session session = Session.getInstance();
        clearForm();

        if (session.isOrganisateur()) {
            // CRUD complet + liste complète
            setVisible(memberTable, true);
            setVisible(crudForm,    true);
            setVisible(crudBox,     true);
            setVisible(crudTitle,   true);
            setVisible(btnAdd,      true);
            setVisible(btnDelete,   true);
            if (crudTitle != null) crudTitle.setText("Add / Edit Member");
            if (formName  != null) formName.setEditable(true);
            memberTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, sel) -> { if (sel != null) fillForm(sel); });
            refreshTable();

        } else if (session.isMembre()) {
            // Formulaire visible, table masquée, profil propre uniquement
            setVisible(memberTable, false);
            setVisible(crudForm,    true);
            setVisible(crudBox,     true);
            setVisible(crudTitle,   true);
            setVisible(btnAdd,      false);
            setVisible(btnDelete,   false);
            if (crudTitle != null) crudTitle.setText("My Profile");
            if (formName  != null) formName.setEditable(false);

            refreshTable();
            // Pré-charger le profil du membre connecté
            memberTable.getItems().stream()
                    .filter(m -> m.getName().equals(session.getDisplayName()))
                    .findFirst().ifPresent(this::fillForm);

        } else {
            // ARTISTE ou PUBLIC : cet onglet ne devrait pas être visible (masqué par MainController)
            setVisible(crudForm,    false);
            setVisible(crudBox,     false);
            setVisible(crudTitle,   false);
            setVisible(memberTable, false);
        }
    }

    @FXML private void handleCreate() {
        if (!Session.getInstance().isOrganisateur()) { setStatus("Accès refusé.", true); return; }
        try { saveMember(buildFromForm()); setStatus("Member added.", false); clearForm(); refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleUpdate() {
        Session session = Session.getInstance();
        try {
            CommunityMember m = buildFromForm();
            if (session.isMembre()) m.setName(session.getDisplayName());
            if (m.getName() == null || m.getName().isBlank()) {
                setStatus("Erreur : nom introuvable. Reconnectez-vous.", true); return;
            }
            // Passer l'ancien nom pour le WHERE (permet de changer le nom)
            String oldName = (session.isMembre() || originalName == null) ? m.getName() : originalName;
            updateMember(m, oldName);
            setStatus("Profile updated.", false);
            if (session.isOrganisateur()) refreshTable();
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML private void handleDelete() {
        if (!Session.getInstance().isOrganisateur()) { setStatus("Accès refusé.", true); return; }
        CommunityMember sel = memberTable.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Select a member.", true); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel.getName() + "\"?",
                ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { deleteMember(sel.getName()); setStatus("Deleted.", false); clearForm(); refreshTable();
                } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML private void handleResetForm() {
        clearForm();
        Session s = Session.getInstance();
        if (s.isMembre()) memberTable.getItems().stream()
                .filter(m -> m.getName().equals(s.getDisplayName()))
                .findFirst().ifPresent(this::fillForm);
    }

    private void saveMember(CommunityMember m) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO MEMBRE_COMMUNAUTE (nom,email,annee_naissance,telephone,ville,type_adhesion) VALUES(?,?,?,?,?,?)")) {
            conn.setAutoCommit(false);
            ps.setString(1,m.getName()); ps.setString(2,m.getEmail());
            ps.setObject(3,m.getBirthYear(),Types.INTEGER); ps.setString(4,m.getPhone());
            ps.setString(5,m.getCity()); ps.setString(6,m.getMembershipType());
            ps.executeUpdate(); conn.commit();
        }
    }
    private void updateMember(CommunityMember m) throws SQLException {
        updateMember(m, m.getName());
    }

    private void updateMember(CommunityMember m, String oldName) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE MEMBRE_COMMUNAUTE SET nom=?,email=?,annee_naissance=?,telephone=?,ville=?,type_adhesion=? WHERE nom=?")) {
            conn.setAutoCommit(false);
            ps.setString(1,m.getName()); // nouveau nom
            ps.setString(2,m.getEmail()); ps.setObject(3,m.getBirthYear(),Types.INTEGER);
            ps.setString(4,m.getPhone()); ps.setString(5,m.getCity());
            ps.setString(6,m.getMembershipType());
            ps.setString(7,oldName); // WHERE nom=ancien_nom
            ps.executeUpdate(); conn.commit();
        }
    }
    private void deleteMember(String name) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM MEMBRE_COMMUNAUTE WHERE nom=?")) {
            conn.setAutoCommit(false); ps.setString(1,name); ps.executeUpdate(); conn.commit();
        }
    }
    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }
    private CommunityMember buildFromForm() {
        CommunityMember m = new CommunityMember();
        // Le nom peut être vide si champ non-éditable (membre) — sera forcé dans handleUpdate
        if (formName != null && !formName.getText().isBlank())
            m.setName(formName.getText().trim());
        else
            m.setName(Session.getInstance().getDisplayName());
        m.setEmail         (formEmail!=null?formEmail.getText().trim():"");
        m.setCity          (formCity !=null?formCity .getText().trim():"");
        m.setPhone         (formPhone!=null?formPhone.getText().trim():"");
        m.setMembershipType(formMembership!=null?formMembership.getValue():"standard");
        if (formYear!=null&&!formYear.getText().isBlank()) m.setBirthYear(Integer.parseInt(formYear.getText().trim()));
        return m;
    }
    private void fillForm(CommunityMember m) {
        originalName = m.getName();
        if (formName  !=null) formName .setText(m.getName() !=null?m.getName() :"");
        if (formEmail !=null) formEmail.setText(m.getEmail()!=null?m.getEmail():"");
        if (formCity  !=null) formCity .setText(m.getCity() !=null?m.getCity() :"");
        if (formPhone !=null) formPhone.setText(m.getPhone()!=null?m.getPhone():"");
        if (formYear  !=null) formYear .setText(m.getBirthYear()!=null?m.getBirthYear().toString():"");
        if (formMembership!=null&&m.getMembershipType()!=null) formMembership.setValue(m.getMembershipType());
    }
    private void clearForm() {
        originalName = null;
        if (formName  !=null) formName.clear();
        if (formEmail !=null) formEmail.clear();
        if (formCity  !=null) formCity.clear();
        if (formPhone !=null) formPhone.clear();
        if (formYear  !=null) formYear.clear();
        if (statusLabel!=null) statusLabel.setText("");
    }
    private void setVisible(javafx.scene.Node node, boolean v) {
        if (node!=null) { node.setVisible(v); node.setManaged(v); }
    }
    private void setStatus(String msg, boolean err) {
        if (statusLabel!=null) { statusLabel.setText(msg);
            statusLabel.setStyle(err?"-fx-text-fill:red;":"-fx-text-fill:green;"); }
    }
}
