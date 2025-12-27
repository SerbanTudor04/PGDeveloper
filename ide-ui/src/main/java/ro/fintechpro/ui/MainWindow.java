package ro.fintechpro.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ro.fintechpro.core.db.DataSourceManager;

public class MainWindow {

    private final DataSourceManager dbManager = new DataSourceManager();
    private final Label statusLabel = new Label("Status: Disconnected");

    public Parent getView() {
        // 1. Create Input Fields
        TextField hostField = new TextField("10.18.0.2");
        hostField.setPromptText("Host");

        TextField portField = new TextField("5432"); // Added Port just in case
        portField.setPrefWidth(60);

        TextField dbField = new TextField("postgres");
        dbField.setPromptText("Database");

        TextField userField = new TextField("moonelis_acccount");
        userField.setPromptText("User");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        // 2. SSL Checkbox
        CheckBox sslBox = new CheckBox("SSL");
        sslBox.setSelected(true); // Default to true since your server requires it

        // 3. Create Connect Button
        Button connectBtn = new Button("Connect");
        connectBtn.setOnAction(e -> {
            try {
                statusLabel.setText("Status: Connecting...");

                int port = Integer.parseInt(portField.getText());

                // Call the Updated Core Method
                dbManager.connect(
                        hostField.getText(),
                        port,
                        dbField.getText(),      // Now using the custom DB name
                        userField.getText(),
                        passField.getText(),
                        sslBox.isSelected()     // Passing the SSL flag
                );

                if (dbManager.testConnection()) {
                    statusLabel.setText("Status: Connected Successfully!");
                    statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("Status: Connection Failed (Check credentials)");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }

            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                ex.printStackTrace();
            }
        });

        // 4. Layout (Using FlowPane for automatic wrapping)
        FlowPane controls = new FlowPane(10, 10);
        controls.setPadding(new Insets(10));
        controls.getChildren().addAll(
                new Label("Host:"), hostField,
                new Label("Port:"), portField,
                new Label("DB:"), dbField,
                new Label("User:"), userField,
                new Label("Pass:"), passField,
                sslBox,
                connectBtn
        );

        VBox root = new VBox(20, controls, statusLabel);
        root.setPadding(new Insets(20));

        return root;
    }
}