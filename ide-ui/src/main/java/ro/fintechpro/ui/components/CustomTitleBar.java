package ro.fintechpro.ui.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
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
        this.stage.setMaximized(false); // Reset to ensure correct state tracking

        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(0, 10, 0, 10)); // Zero vertical padding, let height control it
        this.getStyleClass().add("custom-title-bar");

        // Exact height of standard macOS title bar is usually 28px-38px depending on OS version
        this.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0; -fx-min-height: 38px; -fx-max-height: 38px;");

        HBox windowControls = createWindowControls();
        HBox titleSection = createTitleSection(titleText);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            // --- MAC NATIVE MODE ---
            // We do NOT add any buttons here. The real OS buttons are floating above us.

            // 1. Create a "Spacer" that is exactly the width of the traffic lights
            Region trafficLightSpacer = new Region();
            trafficLightSpacer.setMinWidth(70); // 70px is standard space for traffic lights

            // 2. Add content.
            // Logic: [Empty Space for Lights] [Title] [Spacer] [Other Toolbar Items]
            this.getChildren().addAll(trafficLightSpacer, titleSection, spacer);

            // 3. Make background transparent?
            // Usually, you want this CustomTitleBar to have the same background color
            // as your Sidebar, creating that seamless "Sidebar extends to top" look.
            this.setStyle("-fx-background-color: transparent;");
        } else {
            // Windows: Title Left, Controls Right
            this.getChildren().addAll(titleSection, spacer, windowControls);
        }

        // Window Dragging
        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        this.setOnMouseDragged(event -> {
            if (!stage.isMaximized() && !stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) toggleMaximize();
        });
    }

    private HBox createTitleSection(String titleText) {
        FontIcon appIcon = new FontIcon(Feather.DATABASE);
        appIcon.setIconColor(Color.web("#61afef"));
        appIcon.setIconSize(14);

        Label title = new Label(titleText);
        title.getStyleClass().add(Styles.TEXT_BOLD);
        title.setStyle("-fx-font-size: 12px;");

        HBox box = new HBox(8, appIcon, title);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox createWindowControls() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return createMacTrafficLights();
        } else {
            return createWindowsControls();
        }
    }

    // --- PIXEL PERFECT MAC CONTROLS ---
    private HBox createMacTrafficLights() {
        Button closeBtn = new MacButton("#FF5F57", "#E0443E", "M 0,0 L 7,7 M 7,0 L 0,7"); // X shape
        Button minBtn = new MacButton("#FEBC2E", "#D99E28", "M 0,4 L 8,4"); // Minus shape
        Button maxBtn = new MacButton("#28C840", "#1AAB29", "M 0,5 L 3,8 L 3,5 L 5,5 L 5,3 L 8,3 L 5,0 M 0,5 L 0,8 L 8,8 L 8,0"); // Arrows (Fullscreen)

        closeBtn.setOnAction(e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        minBtn.setOnAction(e -> stage.setIconified(true));
        maxBtn.setOnAction(e -> toggleMaximize());

        HBox controls = new HBox(8, closeBtn, minBtn, maxBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        // Native behavior: Symbols only appear when hovering ANYWHERE on the control set
        controls.setOnMouseEntered(e -> {
            ((MacButton) closeBtn).showIcon(true);
            ((MacButton) minBtn).showIcon(true);
            ((MacButton) maxBtn).showIcon(true);
        });

        controls.setOnMouseExited(e -> {
            ((MacButton) closeBtn).showIcon(false);
            ((MacButton) minBtn).showIcon(false);
            ((MacButton) maxBtn).showIcon(false);
        });

        return controls;
    }

    // Inner class to handle specific Mac button drawing
    private static class MacButton extends Button {
        private final SVGPath icon;

        public MacButton(String color, String borderColor, String svgContent) {
            // 1. Circle Shape
            this.setMinSize(12, 12);
            this.setMaxSize(12, 12);

            // 2. CSS Styling for color and border
            this.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-background-radius: 50%; " +
                            "-fx-border-color: " + borderColor + "; " +
                            "-fx-border-radius: 50%; " +
                            "-fx-border-width: 0.5px; " +
                            "-fx-padding: 0;"
            );

            // 3. The Icon (Hidden by default)
            icon = new SVGPath();
            icon.setContent(svgContent);
            icon.setFill(Color.rgb(0, 0, 0, 0.5)); // Dark semi-transparent
            icon.setStroke(Color.rgb(0, 0, 0, 0.5));
            icon.setStrokeWidth(1);
            icon.setOpacity(0); // Invisible initially

            // Center the icon using a StackPane-like layout logic within the button
            this.setGraphic(icon);
            this.setAlignment(Pos.CENTER);
        }

        public void showIcon(boolean show) {
            icon.setOpacity(show ? 1.0 : 0.0);
        }
    }

    // --- WINDOWS CONTROLS (unchanged) ---
    private HBox createWindowsControls() {
        Button closeBtn = new Button();
        Button minBtn = new Button();
        Button maxBtn = new Button();

        minBtn.setGraphic(new FontIcon(Feather.MINUS));
        maxBtn.setGraphic(new FontIcon(Feather.SQUARE));
        closeBtn.setGraphic(new FontIcon(Feather.X));

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

    private void toggleMaximize() {
        boolean max = !stage.isMaximized();
        stage.setMaximized(max);
        // Windows taskbar fix (only needed for Windows)
        if (max && !System.getProperty("os.name").toLowerCase().contains("mac")) {
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    }
}