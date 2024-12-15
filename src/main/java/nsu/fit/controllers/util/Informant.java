package nsu.fit.controllers.util;

import javafx.scene.control.Alert;
import nsu.fit.exceptions.InputError;

public class Informant {
    public static void inform(InputError error, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Warning: " + error);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void error(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }
}
