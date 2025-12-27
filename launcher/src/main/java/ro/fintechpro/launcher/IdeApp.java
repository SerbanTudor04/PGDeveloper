package ro.fintechpro.launcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ro.fintechpro.ui.MainWindow; // Import from ide-ui module


public class IdeApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow.getView(), 800, 600);

        primaryStage.setTitle("PgDeveloper");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}