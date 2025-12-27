package ro.fintechpro.ui.ide;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import ro.fintechpro.core.service.MetadataService;

import java.util.List;

public class SidebarView extends VBox {

    private final TreeView<String> treeView;
    private final TreeItem<String> rootItem;
    private final TextField searchField;

    public SidebarView() {
        this.setSpacing(5);
        this.setPadding(new Insets(5));
        this.getStyleClass().add("sidebar-container"); // Useful for CSS later

        // --- 1. Header (Title + Actions) ---
        Label title = new Label("Explorer");
        title.getStyleClass().add(Styles.TEXT_BOLD);

        Button refreshBtn = new Button(null, new FontIcon(Feather.REFRESH_CW));
        refreshBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        refreshBtn.setTooltip(new Tooltip("Reload Structure"));

        Button collapseBtn = new Button(null, new FontIcon(Feather.MINUS_SQUARE));
        collapseBtn.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        collapseBtn.setTooltip(new Tooltip("Collapse All"));
        collapseBtn.setOnAction(e -> collapseAll());

        HBox header = new HBox(10, title, new HBox(refreshBtn, collapseBtn));
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS); // Push buttons to right
        ((HBox)header.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 5, 5, 5));

        // --- 2. Search Bar ---
        searchField = new TextField();
        searchField.setPromptText("Search objects...");
        searchField.setLeft(new FontIcon(Feather.SEARCH)); // AtlantaFX feature
        searchField.getStyleClass().add(Styles.SMALL);

        // --- 3. The Tree ---
        rootItem = new TreeItem<>("Database", new FontIcon(Feather.DATABASE));
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.getStyleClass().add(Styles.DENSE);
        VBox.setVgrow(treeView, Priority.ALWAYS);

        // Custom Cell Factory for Professional Look
        treeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setGraphic(getTreeItem().getGraphic());
                    // Style specific levels if needed (e.g. bold schemas)
                    if (getTreeItem().getParent() == rootItem) {
                        getStyleClass().add(Styles.TEXT_BOLD);
                    }
                }
            }
        });

        this.getChildren().addAll(header, searchField, treeView);
    }

    /**
     * Thread-safe population of the tree.
     */
    public void populate(MetadataService metaService) {
        // Run on UI Thread to be safe / "Reliable"
        Platform.runLater(() -> {
            rootItem.getChildren().clear();

            try {
                List<String> schemas = metaService.getSchemas();

                for (String schema : schemas) {
                    TreeItem<String> schemaItem = new TreeItem<>(schema, new FontIcon(Feather.FOLDER));

                    // 1. Tables
                    List<MetadataService.DbObject> tables = metaService.getTables(schema);
                    if (!tables.isEmpty()) {
                        TreeItem<String> tablesGroup = new TreeItem<>("Tables (" + tables.size() + ")", new FontIcon(Feather.LIST));
                        for (MetadataService.DbObject table : tables) {
                            FontIcon icon = new FontIcon(Feather.LAYOUT);
                            icon.setIconColor(javafx.scene.paint.Color.web("#61afef")); // Light Blue for tables
                            tablesGroup.getChildren().add(new TreeItem<>(table.name(), icon));
                        }
                        schemaItem.getChildren().add(tablesGroup);
                    }

                    // 2. Procedures
                    List<String> procs = metaService.getProcedures(schema);
                    if (!procs.isEmpty()) {
                        TreeItem<String> procGroup = new TreeItem<>("Procedures", new FontIcon(Feather.PLAY_CIRCLE));
                        for (String proc : procs) {
                            procGroup.getChildren().add(new TreeItem<>(proc, new FontIcon(Feather.TERMINAL)));
                        }
                        schemaItem.getChildren().add(procGroup);
                    }

                    // 3. Functions
                    List<String> funcs = metaService.getFunctions(schema);
                    if (!funcs.isEmpty()) {
                        TreeItem<String> funcGroup = new TreeItem<>("Functions", new FontIcon(Feather.ACTIVITY));
                        for (String func : funcs) {
                            TreeItem<String> fItem = new TreeItem<>(func, new FontIcon(Feather.CODE));
                            schemaItem.getChildren().add(fItem);
                        }
                        // If you prefer grouping functions separately, add to schemaItem like above
                        // For now, let's group them to be clean:
                        TreeItem<String> fGroup = new TreeItem<>("Functions", new FontIcon(Feather.ACTIVITY));
                        for (String func : funcs) {
                            fGroup.getChildren().add(new TreeItem<>(func, new FontIcon(Feather.CODE)));
                        }
                        schemaItem.getChildren().add(fGroup);
                    }

                    rootItem.getChildren().add(schemaItem);
                }
            } catch (Exception e) {
                // Show error in the tree itself so user sees it
                TreeItem<String> errorItem = new TreeItem<>("Error loading: " + e.getMessage(), new FontIcon(Feather.ALERT_TRIANGLE));
                rootItem.getChildren().add(errorItem);
                e.printStackTrace();
            }
        });
    }

    private void collapseAll() {
        for (TreeItem<?> child : rootItem.getChildren()) {
            child.setExpanded(false);
        }
    }

    // Getter to hook up the Refresh button from the Main View if needed
    public Button getRefreshButton() {
        return (Button) ((HBox)((HBox)getChildren().get(0)).getChildren().get(1)).getChildren().get(0);
    }
}