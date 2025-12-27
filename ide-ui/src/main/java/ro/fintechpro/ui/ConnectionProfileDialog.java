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

import java.util.List;

public class ConnectionProfileDialog {

    private final ConfigService configService;
    private final ListView<ConnectionProfile> parentList;
    private final Stage stage;
    private final ConnectionProfile existingProfile; // Null if adding new

    // Constructor for ADDING
    public ConnectionProfileDialog(ConfigService configService, ListView<ConnectionProfile> parentList) {
        this(configService, parentList, null);
    }

    // Constructor for EDITING
    public ConnectionProfileDialog(ConfigService configService, ListView<ConnectionProfile> parentList, ConnectionProfile existingProfile) {
        this.configService = configService;
        this.parentList = parentList;
        this.existingProfile = existingProfile;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        this.stage.setTitle(existingProfile == null ? "Add New Connection" : "Edit Connection");
    }

    public void show() {
        TextField nameField = new TextField();
        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("5432");
        TextField dbField = new TextField("postgres");
        TextField userField = new TextField("postgres");
        PasswordField passField = new PasswordField();
        CheckBox sslBox = new CheckBox("Use SSL");

        // PRE-FILL if editing
        if (existingProfile != null) {
            nameField.setText(existingProfile.getName());
            hostField.setText(existingProfile.getHost());
            portField.setText(String.valueOf(existingProfile.getPort()));
            dbField.setText(existingProfile.getDatabase());
            userField.setText(existingProfile.getUsername());
            passField.setText(existingProfile.getPassword());
            sslBox.setSelected(existingProfile.isUseSsl());
        }

        Button saveBtn = new Button(existingProfile == null ? "Save" : "Update");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(e -> {
            if (validate(nameField, hostField, userField)) {

                ConnectionProfile newProfile = new ConnectionProfile(
                        nameField.getText(),
                        hostField.getText(),
                        Integer.parseInt(portField.getText()),
                        dbField.getText(),
                        userField.getText(),
                        passField.getText(),
                        sslBox.isSelected()
                );

                if (existingProfile == null) {
                    // ADD MODE: Add to list
                    parentList.getItems().add(newProfile);
                } else {
                    // EDIT MODE: Replace in list
                    int index = parentList.getItems().indexOf(existingProfile);
                    if (index >= 0) {
                        parentList.getItems().set(index, newProfile);
                    }
                }

                // SAVE ALL changes to disk
                configService.saveAll(parentList.getItems());
                stage.close();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> stage.close());

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Name:"), 0, 0);     grid.add(nameField, 1, 0);
        grid.add(new Label("Host:"), 0, 1);     grid.add(hostField, 1, 1);
        grid.add(new Label("Port:"), 0, 2);     grid.add(portField, 1, 2);
        grid.add(new Label("DB:"), 0, 3);       grid.add(dbField, 1, 3);
        grid.add(new Label("User:"), 0, 4);     grid.add(userField, 1, 4);
        grid.add(new Label("Pass:"), 0, 5);     grid.add(passField, 1, 5);
        grid.add(new Label("SSL:"), 0, 6);      grid.add(sslBox, 1, 6);

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttons, 1, 7);

        stage.setScene(new Scene(grid, 400, 420));
        stage.show();
    }

    private boolean validate(TextField name, TextField host, TextField user) {
        if (name.getText().trim().isEmpty() || host.getText().trim().isEmpty() || user.getText().trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Name, Host, and User are required.");
            alert.show();
            return false;
        }
        return true;
    }
}