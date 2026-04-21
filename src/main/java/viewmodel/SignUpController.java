package viewmodel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.UserSession;

import java.util.prefs.Preferences;

public class SignUpController {

    private static final String EMAIL_REGEX    = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,6}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    public void createNewAccount(ActionEvent actionEvent) {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (!email.matches(EMAIL_REGEX)) {
            statusLabel.setText("Please enter a valid email address.");
            return;
        }
        if (!password.matches(PASSWORD_REGEX)) {
            statusLabel.setText("Password must be at least 8 characters and include uppercase, lowercase, a number, and a symbol.");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        Preferences prefs = Preferences.userRoot();
        prefs.put("USERNAME", email);
        prefs.put("PASSWORD", password);

        UserSession.getInstance(email, password, "USER");

        statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 13px;");
        statusLabel.setText("Account created! Redirecting to login...");

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goBack(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
            Stage window = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
