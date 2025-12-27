package ro.fintechpro.ui.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private final Stage stage;

    public CustomTitleBar(Stage stage, String titleText) {
        this.stage = stage;
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.getStyleClass().add("custom-title-bar");

        // Base styling
        this.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0; -fx-min-height: 40px;");

        // --- 1. Create Components ---
        HBox windowControls = createWindowControls();
        HBox titleSection = createTitleSection(titleText);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- 2. OS Specific Layout ---
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            // Mac: Controls Left | Title Center (or Left after controls) | Spacer
            this.getChildren().addAll(windowControls, titleSection, spacer);
            HBox.setMargin(windowControls, new Insets(0, 15, 0, 0)); // Space after traffic lights
        } else {
            // Windows/Linux: Title Left | Spacer | Controls Right
            this.getChildren().addAll(titleSection, spacer, windowControls);
        }

        // --- 3. Enable Dragging ---
        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // Handle dragging (disable if maximized)
        this.setOnMouseDragged(event -> {
            if (!stage.isMaximized() && !stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // Double-click to Maximize/Restore
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private HBox createTitleSection(String titleText) {
        FontIcon appIcon = new FontIcon(Feather.DATABASE);
        appIcon.setIconColor(javafx.scene.paint.Color.web("#61afef"));

        Label title = new Label(titleText);
        title.getStyleClass().add(Styles.TEXT_BOLD);
        title.setStyle("-fx-font-size: 12px;");

        HBox box = new HBox(10, appIcon, title);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createWindowControls() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac = os.contains("mac");

        Button closeBtn = new Button();
        Button minBtn = new Button();
        Button maxBtn = new Button();

        if (isMac) {
            // --- MAC STYLE (Traffic Lights) ---
            configureMacButton(closeBtn, "#ff5f56", "#e0443e"); // Red
            configureMacButton(minBtn, "#ffbd2e", "#dea123");   // Yellow
            configureMacButton(maxBtn, "#27c93f", "#1aab29");   // Green

            // Mac standard order: Close, Min, Max
            HBox controls = new HBox(8, closeBtn, minBtn, maxBtn);
            controls.setAlignment(Pos.CENTER_LEFT);

            closeBtn.setOnAction(e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
            minBtn.setOnAction(e -> stage.setIconified(true));
            maxBtn.setOnAction(e -> toggleMaximize());

            // Hover logic to show icons inside circles (optional, purely aesthetic)
            controls.setOnMouseEntered(e -> {
                closeBtn.setGraphic(new FontIcon(Feather.X));
                minBtn.setGraphic(new FontIcon(Feather.MINUS));
                maxBtn.setGraphic(new FontIcon(Feather.MAXIMIZE_2));
            });
            controls.setOnMouseExited(e -> {
                closeBtn.setGraphic(null);
                minBtn.setGraphic(null);
                maxBtn.setGraphic(null);
            });

            return controls;
        } else {
            // --- WINDOWS STYLE ---
            minBtn.setGraphic(new FontIcon(Feather.MINUS));
            maxBtn.setGraphic(new FontIcon(Feather.SQUARE));
            closeBtn.setGraphic(new FontIcon(Feather.X));

            // Styles
            minBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
            maxBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
            closeBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);

            minBtn.setOnAction(e -> stage.setIconified(true));
            maxBtn.setOnAction(e -> toggleMaximize());
            closeBtn.setOnAction(e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));

            HBox controls = new HBox(0, minBtn, maxBtn, closeBtn);
            controls.setAlignment(Pos.CENTER_RIGHT);
            return controls;
        }
    }

    private void configureMacButton(Button btn, String color, String hoverColor) {
        btn.getStyleClass().addAll("mac-window-btn");
        btn.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%; -fx-min-width: 12px; -fx-min-height: 12px; -fx-max-width: 12px; -fx-max-height: 12px;");
        // Simple hover effect embedded or do it in CSS
    }

    private void toggleMaximize() {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        } else {
            // Check OS again
            if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
                // Windows Fix: Prevent covering taskbar
                javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
            }
            stage.setMaximized(true);
        }
    }
}