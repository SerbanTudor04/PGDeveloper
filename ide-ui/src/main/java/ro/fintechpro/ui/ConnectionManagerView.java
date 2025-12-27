package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.feather.Feather;
import ro.fintechpro.core.db.DataSourceManager;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;
import ro.fintechpro.ui.ide.MainIdeView;

import java.util.Optional;

public class ConnectionManagerView {

    private final ConfigService configService = new ConfigService();
    private final DataSourceManager dbManager = DataSourceManager.getInstance();
    private final ListView<ConnectionProfile> list = new ListView<>();

    public Parent getView(Stage stage) {
        list.getItems().setAll(configService.loadConnections());

        // --- 1. Top Toolbar (Like DataGrip's Action Bar) ---
        Button addBtn = new Button("New", new FontIcon(Feather.PLUS));
        addBtn.getStyleClass().add(Styles.SMALL);
        addBtn.setOnAction(e -> new ConnectionProfileDialog(configService, list).show());

        Button editBtn = new Button("Edit", new FontIcon(Feather.EDIT));
        editBtn.getStyleClass().add(Styles.SMALL);
        editBtn.setOnAction(e -> {
            ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) new ConnectionProfileDialog(configService, list, selected).show();
        });

        Button deleteBtn = new Button(null, new FontIcon(Feather.TRASH)); // Icon only
        deleteBtn.getStyleClass().addAll(Styles.SMALL, Styles.DANGER); // Red hover
        deleteBtn.setOnAction(e -> deleteSelected());

        ToolBar toolbar = new ToolBar(addBtn, editBtn, new Separator(), deleteBtn);

        // --- 2. The List Area ---
        list.setPlaceholder(new Label("No connections found. Click 'New' to add one."));
        list.getStyleClass().add(Styles.STRIPED); // Alternating row colors
        VBox.setVgrow(list, Priority.ALWAYS); // Fill available space

        // --- 3. The "Footer" (Connect Button) ---
        Button connectBtn = new Button("Connect to Database");
        connectBtn.setGraphic(new FontIcon(Feather.DATABASE));
        connectBtn.getStyleClass().addAll(Styles.SUCCESS, Styles.LARGE); // Green, Large
        connectBtn.setMaxWidth(Double.MAX_VALUE); // Stretch full width
        connectBtn.setOnAction(e -> handleConnect(stage, connectBtn));

        // --- 4. Layout Assembly ---
        BorderPane root = new BorderPane();

        // Header
        VBox topContainer = new VBox(
                new Label("Database Connections"), // Title
                toolbar
        );
        topContainer.getStyleClass().add(Styles.BG_DEFAULT);
        ((Label) topContainer.getChildren().get(0)).getStyleClass().add(Styles.TITLE_4);
        topContainer.setPadding(new Insets(10, 10, 0, 10));

        // Center
        VBox centerContainer = new VBox(5, list);
        centerContainer.setPadding(new Insets(10));

        // Bottom
        HBox bottomContainer = new HBox(connectBtn);
        bottomContainer.setPadding(new Insets(15));
        bottomContainer.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 1 0 0 0;"); // Top border

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

    private void handleConnect(Stage stage, Button btn) {
        ConnectionProfile selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a connection first.");
            return;
        }

        if (selected.getPassword() != null && !selected.getPassword().isEmpty()) {
            connectAsync(selected, selected.getPassword(), btn, stage);
        } else {
            askPasswordAndConnect(selected, stage, btn);
        }
    }

    private void askPasswordAndConnect(ConnectionProfile profile, Stage stage, Button sourceButton) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Authentication");
        dialog.setHeaderText("Password for " + profile.getUsername());
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(password -> connectAsync(profile, password, sourceButton, stage));
    }

    private void connectAsync(ConnectionProfile profile, String password, Button sourceButton, Stage stage) {
        String originalText = sourceButton.getText();
        sourceButton.setDisable(true);
        sourceButton.setText("Connecting...");
        sourceButton.setGraphic(new ProgressIndicator( -1.0 )); // Spinner

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

                    if (success) switchToIde(stage);
                    else showAlert("Connection Failed", "Could not reach database.");
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

    private void switchToIde(Stage stage) {
        MainIdeView ideView = new MainIdeView();
        stage.setScene(new Scene(ideView.getView(), 1200, 800)); // Bigger window for IDE
        stage.centerOnScreen();
        stage.setTitle("PgDeveloper - " + dbManager.getConnectionInfoOrName()); // Dynamic Title
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.initOwner(list.getScene().getWindow());
        alert.show();
    }
}