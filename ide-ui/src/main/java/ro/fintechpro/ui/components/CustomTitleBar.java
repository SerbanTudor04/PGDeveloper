package ro.fintechpro.ui.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
        this.stage.setMaximized(false);

        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(0, 10, 0, 10));
        this.getStyleClass().add("custom-title-bar");

        // Default height for Mac title bars is approx 28-30px, but 38px is comfortable for IDEs
        this.setMinHeight(38);
        this.setMaxHeight(38);

        HBox windowControls = createWindowControls();
        HBox titleSection = createTitleSection(titleText);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            // --- MAC MODE ---
            // 1. Spacer for Traffic Lights (approx 70-80px)
            Region trafficLightSpacer = new Region();
            trafficLightSpacer.setMinWidth(80);

            // 2. Add children: [TrafficSpacer] [Title/Breadcrumbs] [Spacer] [Controls(empty)]
            this.getChildren().addAll(trafficLightSpacer, titleSection, spacer);

            // 3. Style: Blend with window background
            this.setStyle("-fx-background-color: -color-bg-default; -fx-border-width: 0 0 1 0; -fx-border-color: -color-border-default;");
        } else {
            // --- WINDOWS MODE ---
            this.getChildren().addAll(titleSection, spacer, windowControls);
            this.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");
        }

        setupDragHandlers();
    }

    // ... (Keep createTitleSection, createWindowControls, MacButton inner class, setupDragHandlers as before)

    private void setupDragHandlers() {
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

    // ... (Keep toggleMaximize and other helpers)
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
        // Return empty for Mac, buttons for Windows
        if (os.contains("mac")) return new HBox();

        // ... (Windows controls logic)
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
        if (max && !System.getProperty("os.name").toLowerCase().contains("mac")) {
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    }
}