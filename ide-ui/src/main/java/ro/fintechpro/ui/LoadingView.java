package ro.fintechpro.ui;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoadingView {

    private final ProgressBar progressBar = new ProgressBar();
    private final Label statusLabel = new Label("Initializing...");

    public Parent getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: -color-bg-default;");
        root.setPrefSize(800, 600); // Match your window size

        // 1. Icon / Logo
        FontIcon logo = new FontIcon(Feather.DATABASE);
        logo.setIconSize(64);
        logo.setIconColor(javafx.scene.paint.Color.web("#61afef")); // Branding Blue

        // 2. Title
        Label title = new Label("PgDeveloper");
        title.getStyleClass().addAll(Styles.TITLE_1);

        // 3. Progress Bar
        progressBar.setPrefWidth(300);

        // 4. Status Text
        statusLabel.getStyleClass().add(Styles.TEXT_MUTED);
        statusLabel.setTextAlignment(TextAlignment.CENTER);

        root.getChildren().addAll(logo, title, progressBar, statusLabel);
        return root;
    }

    public void updateMessage(String message) {
        // Ensure UI updates happen on the FX thread
        javafx.application.Platform.runLater(() -> statusLabel.setText(message));
    }
}