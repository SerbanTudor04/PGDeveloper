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
                // 1. Create a NEW Stage
                Stage mainStage = new Stage();
//                String os = System.getProperty("os.name").toLowerCase();
//                mainStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
                mainStage.initStyle(javafx.stage.StageStyle.DECORATED);
//                if (os.contains("mac")) {
//                    // Mac: Native "Unified" look (Native traffic lights, content flows under them)
//                    mainStage.initStyle(javafx.stage.StageStyle.UNIFIED);
//                } else {
//                    // Windows/Linux: Completely custom (No native frame)
//                    mainStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
//                }

                // 2. Create the Scene
                Scene scene = new Scene(readyIde.getView(mainStage), 1200, 800);

                // 3. SET THE SCENE FIRST <--- This must happen before ResizeHelper
                mainStage.setScene(scene);
                MacWindowStyler.makeTitleBarTransparent(mainStage);

                // 3. Add ResizeHelper ONLY for Windows (Mac uses native resizing now)
                if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
                    mainStage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Switch back for Windows
                    ResizeHelper.addResizeListener(mainStage);
                }
                // 4. NOW Add the resize helper
                ResizeHelper.addResizeListener(mainStage);

                // 5. Show
                mainStage.centerOnScreen();
                mainStage.show();

                // Close the loading stage
                primaryStage.close();
            });
        });

        new Thread(initTask).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}