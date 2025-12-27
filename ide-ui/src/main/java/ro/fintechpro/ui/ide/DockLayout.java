package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

public class DockLayout extends BorderPane {

    public enum Location { LEFT, RIGHT, BOTTOM, CENTER }

    private final SplitPane horizontalSplit;
    private final SplitPane verticalSplit;
    private final Map<Location, TabPane> containers = new EnumMap<>(Location.class);

    // Format for dragging data
    private static final DataFormat DRAG_FORMAT = new DataFormat("application/x-dock-tab-id");

    // Temporary reference to the tab being dragged (since we can't serialize Tab easily)
    private static Tab currentDraggingTab = null;

    public DockLayout(Node editorArea) {
        setupContainer(Location.LEFT);
        setupContainer(Location.RIGHT);
        setupContainer(Location.BOTTOM);

        // Center is simpler (no dropping allowed usually, strictly for editor)
        TabPane centerPane = new TabPane();
        centerPane.getStyleClass().add(Styles.DENSE);
        containers.put(Location.CENTER, centerPane);

        verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().add(editorArea);
        VBox.setVgrow(verticalSplit, Priority.ALWAYS);

        horizontalSplit = new SplitPane();
        horizontalSplit.getItems().add(verticalSplit);
        VBox.setVgrow(horizontalSplit, Priority.ALWAYS);

        this.setCenter(horizontalSplit);
    }

    private void setupContainer(Location loc) {
        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        pane.getStyleClass().add(Styles.DENSE);

        // 1. VISUAL FEEDBACK ON DRAG
        pane.setOnDragEntered(e -> {
            if (e.getDragboard().hasContent(DRAG_FORMAT) && currentDraggingTab != null) {
                if (!pane.getStyleClass().contains("drop-target-active")) {
                    pane.getStyleClass().add("drop-target-active");
                }
            }
        });

        pane.setOnDragExited(e -> {
            pane.getStyleClass().remove("drop-target-active");
        });

        pane.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(DRAG_FORMAT) && currentDraggingTab != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        pane.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasContent(DRAG_FORMAT) && currentDraggingTab != null) {
                Tab tab = currentDraggingTab;
                if (tab.getTabPane() != pane) {
                    tab.getTabPane().getTabs().remove(tab);
                    pane.getTabs().add(tab);
                    pane.getSelectionModel().select(tab);
                }
                e.setDropCompleted(true);
                currentDraggingTab = null;
            } else {
                e.setDropCompleted(false);
            }
            pane.getStyleClass().remove("drop-target-active");
            e.consume();
        });

        pane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> c) -> {
            updateVisibility(loc);
        });

        containers.put(loc, pane);
    }

    public void dock(Node component, String title, Location location) {
        // FIX 1: Set text to empty string so it doesn't duplicate with the graphic
        Tab tab = new Tab("", component);
        tab.setClosable(location != Location.CENTER);

        // Custom Drag Handle (The visible title)
        Label dragHandle = new Label(title);
        dragHandle.getStyleClass().add(Styles.TEXT_BOLD);
        dragHandle.setMouseTransparent(false); // Ensure it receives clicks

        tab.setGraphic(dragHandle);

        dragHandle.setOnDragDetected(e -> {
            Dragboard db = dragHandle.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(DRAG_FORMAT, "tab-" + System.identityHashCode(tab));
            db.setContent(content);

            // Visual Drag Image (Optional: shows the label while dragging)
            db.setDragView(dragHandle.snapshot(null, null));

            currentDraggingTab = tab;
            e.consume();
        });

        // ... (ContextMenu code remains the same) ...
        ContextMenu menu = new ContextMenu();
        for (Location loc : Location.values()) {
            if (loc == location || loc == Location.CENTER) continue;
            MenuItem item = new MenuItem("Move to " + loc);
            item.setOnAction(e -> moveTab(tab, loc));
            menu.getItems().add(item);
        }
        tab.setContextMenu(menu);

        containers.get(location).getTabs().add(tab);
        containers.get(location).getSelectionModel().select(tab);
    }

    private void moveTab(Tab tab, Location newLoc) {
        tab.getTabPane().getTabs().remove(tab);
        containers.get(newLoc).getTabs().add(tab);
        containers.get(newLoc).getSelectionModel().select(tab);
    }

    private void updateVisibility(Location loc) {
        TabPane pane = containers.get(loc);
        boolean hasTabs = !pane.getTabs().isEmpty();

        switch (loc) {
            case LEFT -> toggleSplitItem(horizontalSplit, pane, hasTabs, 0);
            case RIGHT -> toggleSplitItem(horizontalSplit, pane, hasTabs, horizontalSplit.getItems().size());
            case BOTTOM -> toggleSplitItem(verticalSplit, pane, hasTabs, verticalSplit.getItems().size());
        }
    }

    private void toggleSplitItem(SplitPane split, Node node, boolean show, int index) {
        boolean currentlyShown = split.getItems().contains(node);
        if (show && !currentlyShown) {
            if (index >= split.getItems().size()) split.getItems().add(node);
            else split.getItems().add(index, node);

            if (split == horizontalSplit) split.setDividerPositions(0.2, 0.8);
            if (split == verticalSplit) split.setDividerPositions(0.7);
        } else if (!show && currentlyShown) {
            split.getItems().remove(node);
        }
    }
}