package ro.fintechpro.ui.util;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResizeHelper {

    public static void addResizeListener(Stage stage) {
        ResizeListener listener = new ResizeListener(stage);
        Scene scene = stage.getScene();

        // Essential: The listeners must be on the Scene to capture events at the window edges
        scene.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        scene.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener);
    }

    static class ResizeListener implements EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
        private final int border = 5; // Pixel width of the resize border
        private double startX = 0;
        private double startY = 0;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            EventType<? extends MouseEvent> mouseEventType = mouseEvent.getEventType();
            Scene scene = stage.getScene();

            double mouseX = mouseEvent.getSceneX();
            double mouseY = mouseEvent.getSceneY();
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(mouseEventType)) {
                if (mouseX < border && mouseY < border) cursorEvent = Cursor.NW_RESIZE;
                else if (mouseX < border && mouseY > sceneHeight - border) cursorEvent = Cursor.SW_RESIZE;
                else if (mouseX > sceneWidth - border && mouseY < border) cursorEvent = Cursor.NE_RESIZE;
                else if (mouseX > sceneWidth - border && mouseY > sceneHeight - border) cursorEvent = Cursor.SE_RESIZE;
                else if (mouseX < border) cursorEvent = Cursor.W_RESIZE;
                else if (mouseX > sceneWidth - border) cursorEvent = Cursor.E_RESIZE;
                else if (mouseY < border) cursorEvent = Cursor.N_RESIZE;
                else if (mouseY > sceneHeight - border) cursorEvent = Cursor.S_RESIZE;
                else cursorEvent = Cursor.DEFAULT;

                scene.setCursor(cursorEvent);

            } else if (MouseEvent.MOUSE_EXITED.equals(mouseEventType) || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)) {
                scene.setCursor(Cursor.DEFAULT);

            } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType)) {
                startX = stage.getWidth() - mouseEvent.getScreenX();
                startY = stage.getHeight() - mouseEvent.getScreenY();

            } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType)) {
                if (!Cursor.DEFAULT.equals(cursorEvent)) {
                    if (!Cursor.W_RESIZE.equals(cursorEvent) && !Cursor.E_RESIZE.equals(cursorEvent)) {
                        double minHeight = stage.getMinHeight() > (border * 2) ? stage.getMinHeight() : (border * 2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.N_RESIZE.equals(cursorEvent) || Cursor.NE_RESIZE.equals(cursorEvent)) {
                            if (stage.getHeight() > minHeight || mouseEvent.getScreenY() < 0) {
                                double newHeight = stage.getY() - mouseEvent.getScreenY() + stage.getHeight();
                                stage.setHeight(newHeight);
                                stage.setY(mouseEvent.getScreenY());
                            }
                        } else {
                            if (stage.getHeight() > minHeight || mouseEvent.getScreenY() - stage.getY() > 0) {
                                stage.setHeight(mouseEvent.getScreenY() - stage.getY());
                            }
                        }
                    }

                    if (!Cursor.N_RESIZE.equals(cursorEvent) && !Cursor.S_RESIZE.equals(cursorEvent)) {
                        double minWidth = stage.getMinWidth() > (border * 2) ? stage.getMinWidth() : (border * 2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.W_RESIZE.equals(cursorEvent) || Cursor.SW_RESIZE.equals(cursorEvent)) {
                            if (stage.getWidth() > minWidth || mouseEvent.getScreenX() < 0) {
                                double newWidth = stage.getX() - mouseEvent.getScreenX() + stage.getWidth();
                                stage.setWidth(newWidth);
                                stage.setX(mouseEvent.getScreenX());
                            }
                        } else {
                            if (stage.getWidth() > minWidth || mouseEvent.getScreenX() - stage.getX() > 0) {
                                stage.setWidth(mouseEvent.getScreenX() - stage.getX());
                            }
                        }
                    }
                }
            }
        }
    }
}