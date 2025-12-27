package ro.fintechpro.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;

public class ConnectionManagerView {

    private final ConfigService configService = new ConfigService();
    private final DataSourceManager dbManager = new DataSourceManager();
    private final ListView<ConnectionProfile> list = new ListView<>();

    public Parent getView(Stage stage) {
        // 1. Load existing profiles
        list.getItems().addAll(configService.loadConnections());

        // 2. "Connect" Button Logic
        Button connectBtn = new Button("Connect to Selected");
        connectBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                askPasswordAndConnect(selected, stage);
            }
        });

        // 3. "Add New" Button Logic
        Button addBtn = new Button("Add New Connection");
        addBtn.setOnAction(e -> showAddDialog(stage));

        // Layout
        VBox root = new VBox(10, new Label("Saved Connections:"), list, new HBox(10, connectBtn, addBtn));
        root.setPadding(new Insets(20));
        return root;
    }

    private void askPasswordAndConnect(ConnectionProfile profile, Stage stage) {
        // Simple Password Dialog
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Enter password for " + profile.getUsername());
        dialog.setContentText("Password:");

        dialog.showAndWait().ifPresent(password -> {
            try {
                // Try to connect
                dbManager.connect(
                        profile.getHost(),
                        profile.getPort(),
                        profile.getDatabase(),
                        profile.getUsername(),
                        password, // Password from dialog
                        profile.isUseSsl()
                );

                if (dbManager.testConnection()) {
                    // SUCCESS! Switch to IDE Mode
                    MainIdeView ideView = new MainIdeView();
                    stage.setScene(new Scene(ideView.getView(), 1024, 768));
                    stage.centerOnScreen();
                } else {
                    showAlert("Connection Failed", "Could not connect to database.");
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });
    }

    private void showAddDialog(Stage owner) {
        // This reuses your old logic but in a popup to save the profile
        AddConnectionDialog dialog = new AddConnectionDialog(configService, list);
        dialog.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}