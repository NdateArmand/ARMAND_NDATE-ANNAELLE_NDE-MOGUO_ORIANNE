package com.project.artconnect.ui;

import com.project.artconnect.model.Session;
import com.project.artconnect.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab     communityTab;
    @FXML private Button  btnConnexion;
    @FXML private Button  btnDeconnexion;
    @FXML private Label   statusLabel;

    // Controllers injectés par JavaFX via fx:id sur les fx:include
    // Convention : fx:id="artistsTab" => @FXML ArtistController artistsTabController
    @FXML private ArtistController     artistsTabController;
    @FXML private ArtworkController    artworksTabController;
    @FXML private ExhibitionController exhibitionsTabController;
    @FXML private WorkshopController   workshopsTabController;
    @FXML private CommunityController  communityTabContentController;
    @FXML private GalleryController    galleriesTabController;

    private Tab savedCommunityTab;

    @FXML
    public void initialize() {
        savedCommunityTab = communityTab;
        applyRoleConfig();
    }

    public void applyRoleConfig() {
        Session session = Session.getInstance();

        boolean loggedIn = session.isLoggedIn();
        setNodeVisible(btnConnexion,   !loggedIn);
        setNodeVisible(btnDeconnexion,  loggedIn);

        boolean showCommunity  = session.isOrganisateur() || session.isMembre();
        boolean alreadyPresent = mainTabPane.getTabs().contains(savedCommunityTab);
        if (showCommunity && !alreadyPresent)  mainTabPane.getTabs().add(savedCommunityTab);
        else if (!showCommunity && alreadyPresent) mainTabPane.getTabs().remove(savedCommunityTab);

        if (statusLabel != null) {
            String label = switch (session.getRole()) {
                case PUBLIC       -> "Mode visiteur (non connecté)";
                case MEMBRE       -> "Membre : "       + session.getDisplayName();
                case ARTISTE      -> "Artiste : "      + session.getDisplayName();
                case ORGANISATEUR -> "Organisateur : " + session.getDisplayName();
            };
            statusLabel.setText("ArtConnect Pro  |  " + label);
        }

        // Notifier chaque controller d'onglet
        if (artistsTabController          != null) artistsTabController         .applyRole();
        if (artworksTabController         != null) artworksTabController        .applyRole();
        if (exhibitionsTabController      != null) exhibitionsTabController     .applyRole();
        if (workshopsTabController        != null) workshopsTabController       .applyRole();
        if (communityTabContentController != null) communityTabContentController.applyRole();
        if (galleriesTabController         != null) galleriesTabController        .applyRole();
    }

    @FXML private void handleLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/project/artconnect/ui/LoginView.fxml"));
            Stage loginStage = new Stage();
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.setTitle("Connexion");
            loginStage.setResizable(false);
            loginStage.setScene(new Scene(loader.load()));
            LoginController lc = loader.getController();
            lc.setOnLoginSuccess(() -> { loginStage.close(); applyRoleConfig(); });
            loginStage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        new AuthService().logout();
        applyRoleConfig();
        mainTabPane.getSelectionModel().selectFirst();
    }

    @FXML private void handleExit() { Platform.exit(); }

    private void setNodeVisible(Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
}
