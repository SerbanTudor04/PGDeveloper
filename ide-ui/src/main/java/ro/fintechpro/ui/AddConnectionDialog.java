package ro.fintechpro.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ro.fintechpro.core.model.ConnectionProfile;
import ro.fintechpro.core.service.ConfigService;

public class AddConnectionDialog {

    private final ConfigService configService;
    private final ListView<ConnectionProfile> parentList;
    private final Stage stage;

    // Constructor accepts the service (to save to disk) and the list (to update UI immediately)
    public AddConnectionDialog(ConfigService configService, ListView<ConnectionProfile> parentList) {
        this.configService = configService;
        this.parentList = parentList;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL); // Blocks the main window while open
        this.stage.setTitle("Add New Connection");
    }

    public void show() {
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Production DB");

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("5432");
        TextField dbField = new TextField("postgres");
        TextField userField = new TextField("postgres");

        // NEW PASSWORD FIELD
        PasswordField passField = new PasswordField();
        passField.setPromptText("Optional (Saved insecurely)");

        CheckBox sslBox = new CheckBox("Use SSL");

        Button saveBtn = new Button("Save Profile");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            if (validate(nameField, hostField, userField)) {

                // Update constructor call to include password
                ConnectionProfile profile = new ConnectionProfile(
                        nameField.getText(),
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        dbField.getText(),
                        userField.getText(),
                        passField.getText(), // <--- Pass the password
                        sslBox.isSelected()
                );

                configService.saveConnection(profile);
                parentList.getItems().add(profile);
                stage.close();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> stage.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Profile Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Host IP:"), 0, 1);      grid.add(hostField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);         grid.add(portField, 1, 2);
        grid.add(new Label("Database:"), 0, 3);     grid.add(dbField, 1, 3);
        grid.add(new Label("Username:"), 0, 4);     grid.add(userField, 1, 4);

        // Add Password to the Grid
        grid.add(new Label("Password:"), 0, 5);     grid.add(passField, 1, 5);

        grid.add(new Label("Security:"), 0, 6);     grid.add(sslBox, 1, 6);

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 1, 7); // <--- Moved down one row

        Scene scene = new Scene(grid, 400, 400); // <--- Increased height slightly
        stage.setScene(scene);
        stage.show();
    }

    private boolean validate(TextField name, TextField host, TextField user) {
        if (name.getText().trim().isEmpty()) {
            showAlert("Profile Name is required.");
            return false;
        }
        if (host.getText().trim().isEmpty()) {
            showAlert("Host IP is required.");
            return false;
        }
        if (user.getText().trim().isEmpty()) {
            showAlert("Username is required.");
            return false;
        }
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.show();
    }
}