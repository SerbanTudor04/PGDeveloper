package ro.fintechpro.ui.components;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class CustomTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;

    public CustomTitleBar(Stage stage, String titleText) {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.getStyleClass().add("custom-title-bar");
        this.setStyle("-fx-background-color: -color-bg-default; -fx-border-color: -color-border-default; -fx-border-width: 0 0 1 0;");
        this.setMinHeight(40);

        // 1. Icon & Title
        FontIcon appIcon = new FontIcon(Feather.DATABASE);
        appIcon.setIconColor(javafx.scene.paint.Color.web("#61afef"));

        Label title = new Label(titleText);
        title.getStyleClass().add(Styles.TEXT_BOLD);
        title.setStyle("-fx-font-size: 12px;");

        HBox leftSection = new HBox(10, appIcon, title);
        leftSection.setAlignment(Pos.CENTER_LEFT);

        // 2. Spacer (pushes controls to the right)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Window Controls (Minimize, Maximize, Close)
        Button minBtn = new Button(null, new FontIcon(Feather.MINUS));
        minBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = new Button(null, new FontIcon(Feather.SQUARE));
        maxBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        maxBtn.setOnAction(e -> {
            stage.setMaximized(!stage.isMaximized());
        });

        Button closeBtn = new Button(null, new FontIcon(Feather.X));
        closeBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
        closeBtn.setOnAction(e -> stage.close());

        HBox controls = new HBox(5, minBtn, maxBtn, closeBtn);
        controls.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(leftSection, spacer, controls);

        // 4. Enable Dragging
        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        this.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }
}