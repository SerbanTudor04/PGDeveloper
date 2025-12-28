package ro.fintechpro.launcher;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import atlantafx.base.theme.NordDark;
import ro.fintechpro.ui.ConnectionManagerView;
import ro.fintechpro.ui.LoadingView;
import ro.fintechpro.ui.MainIdeView;
import ro.fintechpro.ui.util.MacWindowStyler;
import ro.fintechpro.ui.util.ResizeHelper;

public class IdeApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        showConnectionManager();
        primaryStage.setTitle("PgDeveloper");
        primaryStage.show();
    }

    public void showConnectionManager() {
        ConnectionManagerView connectionView = new ConnectionManagerView(this::showLoadingScreen);
        primaryStage.setScene(new Scene(connectionView.getView(), 900, 650));
        primaryStage.centerOnScreen();
    }

    public void showLoadingScreen() {
        LoadingView loadingView = new LoadingView();
        primaryStage.setScene(new Scene(loadingView.getView(), 1200, 800));
        primaryStage.centerOnScreen();

        // 1. Create the Task. Note it returns 'MainIdeView', not 'Parent'
        Task<MainIdeView> initTask = new Task<>() {
            @Override
            protected MainIdeView call() throws Exception {
                // Initialize the View Controller (lightweight)
                MainIdeView ide = new MainIdeView();

                // CALL THE HEAVY PRELOAD METHOD
                // This blocks this background thread until DB is fully indexed
                ide.preload(this::updateMessage);

                return ide;
            }
        };

        // 2. Bind messages to UI
        initTask.messageProperty().addListener((obs, old, msg) -> loadingView.updateMessage(msg));
        initTask.exceptionProperty().addListener((obs, old, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                loadingView.updateMessage("Error: " + ex.getMessage());
            }
        });

        // 3. When done, show the UI
        initTask.setOnSucceeded(e -> {
            loadingView.updateMessage("Building User Interface...");
            MainIdeView readyIde = initTask.getValue();

            Platform.runLater(() -> {
                Stage mainStage = new Stage();
                mainStage.setTitle("PgDeveloper"); // <--- MUST BE SET FIRST
                mainStage.initStyle(javafx.stage.StageStyle.DECORATED);
                // -------------------------------------

                Scene scene = new Scene(readyIde.getView(mainStage), 1200, 800);
                mainStage.setScene(scene);

                // Apply Mac styling (Transparency)
                MacWindowStyler.makeTitleBarTransparent(mainStage);

                // Add resize helper for Windows (Mac uses native now)
                if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
                    mainStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
                    ResizeHelper.addResizeListener(mainStage);
                } else {
                    // For Mac, we still want the resize helper if using DECORATED but hidden titlebar?
                    // Actually standard decorated windows resize fine.
                    // If you want standard Mac resizing, do nothing here.
                    // If you want custom behavior, keep ResizeHelper.
                    // Assuming standard behavior is fine since we kept StageStyle.DECORATED:
                    // (No ResizeHelper needed for Mac in this specific config)
                }

                mainStage.centerOnScreen();
                mainStage.show();

                primaryStage.close();
            });
        });

        new Thread(initTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}