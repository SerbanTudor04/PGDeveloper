package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;

public class DockLayout extends BorderPane {

    public enum Location { LEFT, RIGHT, BOTTOM, CENTER }

    private final SplitPane horizontalSplit;
    private final SplitPane verticalSplit;

    // The actual containers for each region
    private final Map<Location, TabPane> containers = new EnumMap<>(Location.class);

    public DockLayout(Node editorArea) {
        // 1. Initialize Containers (TabPanes)
        setupContainer(Location.LEFT);
        setupContainer(Location.RIGHT);
        setupContainer(Location.BOTTOM);

        // CENTER is special (Editor)
        TabPane centerPane = new TabPane();
        centerPane.getStyleClass().add(Styles.DENSE); // Compact tabs
        containers.put(Location.CENTER, centerPane);

        // 2. Build the Split Hierarchy
        // Structure:  [ LEFT | [ CENTER / BOTTOM ] | RIGHT ]

        // Vertical Split: Center (Top) / Bottom
        verticalSplit = new SplitPane();
        verticalSplit.setOrientation(Orientation.VERTICAL);
        verticalSplit.getItems().add(editorArea); // Start with just editor
        VBox.setVgrow(verticalSplit, Priority.ALWAYS);

        // Horizontal Split: Left / VerticalGroup / Right
        horizontalSplit = new SplitPane();
        horizontalSplit.getItems().add(verticalSplit); // Start with just middle
        VBox.setVgrow(horizontalSplit, Priority.ALWAYS);

        this.setCenter(horizontalSplit);
    }

    private void setupContainer(Location loc) {
        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        pane.getStyleClass().add(Styles.DENSE);

        // Listener: If empty, hide the region. If has tabs, show it.
        pane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> c) -> {
            updateVisibility(loc);
        });

        containers.put(loc, pane);
    }

    /**
     * Adds a generic component (Node) to a specific dock region.
     */
    public void dock(Node component, String title, Location location) {
        Tab tab = new Tab(title, component);
        tab.setClosable(location != Location.CENTER); // Editor might be closable, tools usually hide

        // CONTEXT MENU: Right-click to move tabs
        ContextMenu menu = new ContextMenu();
        for (Location loc : Location.values()) {
            if (loc == location || loc == Location.CENTER) continue; // Skip current & center
            MenuItem item = new MenuItem("Move to " + loc);
            item.setOnAction(e -> moveTab(tab, loc));
            menu.getItems().add(item);
        }
        tab.setContextMenu(menu);

        containers.get(location).getTabs().add(tab);
        containers.get(location).getSelectionModel().select(tab);
    }

    private void moveTab(Tab tab, Location newLoc) {
        // Remove from old parent
        tab.getTabPane().getTabs().remove(tab);

        // Update context menu for new location
        ContextMenu menu = new ContextMenu();
        for (Location loc : Location.values()) {
            if (loc == newLoc || loc == Location.CENTER) continue;
            MenuItem item = new MenuItem("Move to " + loc);
            item.setOnAction(e -> moveTab(tab, loc));
            menu.getItems().add(item);
        }
        tab.setContextMenu(menu);

        // Add to new parent
        containers.get(newLoc).getTabs().add(tab);
        containers.get(newLoc).getSelectionModel().select(tab);
    }

    private void updateVisibility(Location loc) {
        TabPane pane = containers.get(loc);
        boolean hasTabs = !pane.getTabs().isEmpty();

        switch (loc) {
            case LEFT -> toggleSplitItem(horizontalSplit, pane, hasTabs, 0);
            case RIGHT -> toggleSplitItem(horizontalSplit, pane, hasTabs, horizontalSplit.getItems().size()); // End
            case BOTTOM -> toggleSplitItem(verticalSplit, pane, hasTabs, verticalSplit.getItems().size()); // End
        }
    }

    private void toggleSplitItem(SplitPane split, Node node, boolean show, int index) {
        boolean currentlyShown = split.getItems().contains(node);

        if (show && !currentlyShown) {
            if (index >= split.getItems().size()) {
                split.getItems().add(node);
            } else {
                split.getItems().add(index, node);
            }
            // Set reasonable default divider positions
            if (split == horizontalSplit) split.setDividerPositions(0.2, 0.8);
            if (split == verticalSplit) split.setDividerPositions(0.7);
        }
        else if (!show && currentlyShown) {
            split.getItems().remove(node);
        }
    }
}