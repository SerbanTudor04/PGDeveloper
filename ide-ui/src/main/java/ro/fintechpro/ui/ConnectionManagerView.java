package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;

public class ConnectionManagerView {

    private final ConfigService configService = new ConfigService();
    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final ListView<ConnectionProfile> list = new ListView<>();
    private final Runnable onConnectSuccess;

    public ConnectionManagerView(Runnable onConnectSuccess) {
        this.onConnectSuccess = onConnectSuccess;
    }

    public Parent getView() {
        list.getItems().setAll(configService.loadConnections());

        // --- 1. Toolbar ---
        Button addBtn = new Button("New", new FontIcon(Feather.PLUS));
        addBtn.getStyleClass().add(Styles.SMALL);
        addBtn.setOnAction(e -> new ConnectionProfileDialog(configService, list).show());

        Button editBtn = new Button("Edit", new FontIcon(Feather.EDIT));
        editBtn.getStyleClass().add(Styles.SMALL);
        editBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) new ConnectionProfileDialog(configService, list, selected).show();
        });

        Button deleteBtn = new Button(null, new FontIcon(Feather.TRASH));
        deleteBtn.getStyleClass().addAll(Styles.SMALL, Styles.DANGER);
        deleteBtn.setOnAction(e -> deleteSelected());

        ToolBar toolbar = new ToolBar(addBtn, editBtn, new Separator(), deleteBtn);

        // --- 2. List Area ---
        list.setPlaceholder(new Label("No connections found. Click 'New' to add one."));
        list.getStyleClass().add(Styles.STRIPED);
        VBox.setVgrow(list, Priority.ALWAYS);

        // --- 3. Connect Button ---
        Button connectBtn = new Button("Connect to Database");
        connectBtn.setGraphic(new FontIcon(Feather.DATABASE));
        connectBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE);
        connectBtn.setMaxWidth(Double.MAX_VALUE);

        // FIX: We don't pass 'stage' here because it doesn't exist yet.
        // We will retrieve it inside handleConnect from the button itself.
        connectBtn.setOnAction(e -> handleConnect(connectBtn));

        // --- 4. Layout ---
        BorderPane root = new BorderPane();

        VBox topContainer = new VBox(new Label("Database Connections"), toolbar);
        topContainer.getStyleClass().add(Styles.BG_DEFAULT);
        ((Label) topContainer.getChildren().get(0)).getStyleClass().add(Styles.TITLE_4);
        topContainer.setPadding(new Insets(10, 10, 0, 10));

        VBox centerContainer = new VBox(5, list);
        centerContainer.setPadding(new Insets(10));

        HBox bottomContainer = new HBox(connectBtn);
        bottomContainer.setPadding(new Insets(15));
        bottomContainer.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 1 0 0 0;");

        root.setTop(topContainer);
        root.setCenter(centerContainer);
        root.setBottom(bottomContainer);

        return root;
    }

    // --- LOGIC ---

    private void deleteSelected() {
        ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?");
        confirm.initOwner(list.getScene().getWindow());
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                list.getItems().remove(selected);
                configService.saveAll(list.getItems());
            }
        });
    }

    private void handleConnect(Button btn) {
        ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a connection first.");
            return;
        }

        // Retrieve the stage from the button dynamically
        Stage stage = (Stage) btn.getScene().getWindow();

        if (selected.getPassword() != null && !selected.getPassword().isEmpty()) {
            connectAsync(selected, selected.getPassword(), btn);
        } else {
            askPasswordAndConnect(selected, stage, btn);
        }
    }

    private void askPasswordAndConnect(ConnectionProfile profile, Stage stage, Button sourceButton) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Password for " + profile.getUsername());
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(password -> connectAsync(profile, password, sourceButton));
    }

    private void connectAsync(ConnectionProfile profile, String password, Button sourceButton) {
        String originalText = sourceButton.getText();
        sourceButton.setDisable(true);
        sourceButton.setText("Connecting...");
        sourceButton.setGraphic(new ProgressIndicator(-1.0));

        new Thread(() -> {
            try {
                dbManager.connect(
                        profile.getHost(),
                        profile.getPort(),
                        profile.getDatabase(),
                        profile.getUsername(),
                        password,
                        profile.isUseSsl()
                );
                boolean success = dbManager.testConnection();

                Platform.runLater(() -> {
                    sourceButton.setDisable(false);
                    sourceButton.setText(originalText);
                    sourceButton.setGraphic(new FontIcon(Feather.DATABASE));

                    if (success) {
                        // FIX: Delegate transition to the Launcher via callback
                        if (onConnectSuccess != null) onConnectSuccess.run();
                    } else {
                        showAlert("Connection Failed", "Could not reach database.");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    sourceButton.setDisable(false);
                    sourceButton.setText(originalText);
                    sourceButton.setGraphic(new FontIcon(Feather.DATABASE));
                    showAlert("Error", ex.getMessage());
                });
            }
        }).start();
    }


    private void showAlert(String title, String content) {
        if (list.getScene() == null) return;
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.initOwner(list.getScene().getWindow());
        alert.show();
    }
}