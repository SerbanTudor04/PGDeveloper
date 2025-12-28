package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane; // Changed from VBox to BorderPane
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.ui.components.CustomTitleBar; // Import CustomTitleBar

public class LoadingView {

    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Initializing...");

    // UPDATED: Accept Stage parameter
    public Parent getView(Stage stage) {
        // Use BorderPane as root to hold TitleBar at Top and Content in Center
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: -color-bg-default;");
        root.setPrefSize(800, 600);

        // --- Title Bar ---
        CustomTitleBar titleBar = new CustomTitleBar(stage, "PgDeveloper");
        root.setTop(titleBar);

        // --- Center Content ---
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);

        // 1. Icon / Logo
        FontIcon logo = new FontIcon(Feather.DATABASE);
        logo.setIconSize(64);
        logo.setIconColor(javafx.scene.paint.Color.web("#61afef"));

        // 2. Title
        Label title = new Label("PgDeveloper");
        title.getStyleClass().addAll(Styles.TITLE_1);

        // 3. Progress Bar
        progressBar.setPrefWidth(300);

        // 4. Status Text
        statusLabel.getStyleClass().add(Styles.TEXT_MUTED);
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        content.getChildren().addAll(logo, title, progressBar, statusLabel);
        root.setCenter(content);

        return root;
    }

    public void updateMessage(String message) {
        javafx.application.Platform.runLater(() -> statusLabel.setText(message));
    }
}