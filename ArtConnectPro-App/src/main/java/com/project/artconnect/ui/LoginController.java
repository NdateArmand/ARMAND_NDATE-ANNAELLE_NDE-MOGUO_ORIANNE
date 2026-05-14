package com.project.artconnect.ui;

import com.project.artconnect.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private Runnable onLoginSuccess;
    private final AuthService authService = new AuthService();

    /** Appelé par MainController pour recevoir le callback après login réussi */
    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pwd   = passwordField.getText();

        if (email.isBlank() || pwd.isBlank()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        if (authService.login(email, pwd)) {
            // Login réussi → appeler le callback et fermer
            if (onLoginSuccess != null) onLoginSuccess.run();
        } else {
            errorLabel.setText("Email ou mot de passe incorrect.");
            passwordField.clear();
        }
    }

    /** Continuer sans connexion → fermer simplement la popup */
    @FXML
    private void handleSkip() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }
}
