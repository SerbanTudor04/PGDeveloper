package ro.fintechpro.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;

import java.util.Optional;

public class ConnectionManagerView {

    private final ConfigService configService = new ConfigService();
    private final DataSourceManager dbManager = new DataSourceManager();
    private final ListView<ConnectionProfile> list = new ListView<>();

    public Parent getView(Stage stage) {
        // 1. Load existing profiles
        list.getItems().setAll(configService.loadConnections());

        // 2. CONNECT Button
        Button connectBtn = new Button("Connect");
        connectBtn.setStyle("-fx-font-weight: bold; -fx-base: #b3ffcc;");
        connectBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // If password exists, skip the prompt
                if (selected.getPassword() != null && !selected.getPassword().isEmpty()) {
                    connectDirectly(selected, stage);
                } else {
                    askPasswordAndConnect(selected, stage);
                }
            } else {
                showAlert("Selection Required", "Please select a connection.");
            }
        });

        // 3. ADD Button
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> new ConnectionProfileDialog(configService, list).show());

        // 4. EDIT Button
        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                new ConnectionProfileDialog(configService, list, selected).show();
            } else {
                showAlert("Selection Required", "Please select a connection to edit.");
            }
        });

        // 5. DELETE Button
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-text-fill: red;");
        deleteBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?");
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        list.getItems().remove(selected);
                        configService.saveAll(list.getItems());
                    }
                });
            } else {
                showAlert("Selection Required", "Please select a connection to delete.");
            }
        });

        // Layout
        HBox buttons = new HBox(10, connectBtn, addBtn, editBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, new Label("Saved Connections:"), list, buttons);
        root.setPadding(new Insets(20));
        return root;
    }

    // --- HELPER METHODS ---

    private void connectDirectly(ConnectionProfile profile, Stage stage) {
        try {
            dbManager.connect(
                    profile.getHost(),
                    profile.getPort(),
                    profile.getDatabase(),
                    profile.getUsername(),
                    profile.getPassword(),
                    profile.isUseSsl()
            );

            if (dbManager.testConnection()) {
                switchToIde(stage);
            } else {
                showAlert("Connection Failed", "Saved credentials failed. Check your network or password.");
            }
        } catch (Exception ex) {
            showAlert("Error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void askPasswordAndConnect(ConnectionProfile profile, Stage stage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Enter password for " + profile.getUsername());
        dialog.setContentText("Password:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            try {
                dbManager.connect(
                        profile.getHost(),
                        profile.getPort(),
                        profile.getDatabase(),
                        profile.getUsername(),
                        password,
                        profile.isUseSsl()
                );

                if (dbManager.testConnection()) {
                    switchToIde(stage);
                } else {
                    showAlert("Connection Failed", "Authentication failed.");
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });
    }

    private void switchToIde(Stage stage) {
        MainIdeView ideView = new MainIdeView();
        stage.setScene(new Scene(ideView.getView(), 1024, 768));
        stage.centerOnScreen();
        stage.setTitle("PgDeveloper - IDE Mode");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR); // Or WARNING/INFORMATION depending on context
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}