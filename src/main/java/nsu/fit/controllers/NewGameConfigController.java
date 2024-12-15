package nsu.fit.controllers;


import com.google.common.eventbus.EventBus;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import nsu.fit.events.NewGameEvent;
import nsu.fit.events.switching.SwitchToGameFieldEvent;
import nsu.fit.exceptions.InvalidInputException;
import nsu.fit.controllers.util.Informant;
import nsu.fit.controllers.util.InputValidator;
import nsu.fit.events.switching.SwitchToMainMenuEvent;
import nsu.fit.model.GameConfig;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.player.PlayerInfo;

public class NewGameConfigController {
    private EventBus eventBus;
    @FXML
    private TextField widthField;
    @FXML
    private TextField heightField;
    @FXML
    private TextField foodStaticField;
    @FXML
    private TextField delayField;
    @FXML
    private TextField gameNameField;
    @FXML
    private TextField nicknameField;

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @FXML
    private void back() {
        eventBus.post(new SwitchToMainMenuEvent());
    }

    @FXML
    private void startGame() {
        try {
            int width = InputValidator.checkWidth(widthField.getText());
            int height = InputValidator.checkHeight(heightField.getText());
            int foodStatic = InputValidator.checkFoodStatic(foodStaticField.getText());
            int delay = InputValidator.checkDelay(delayField.getText());

            String nickname = nicknameField.getText();
            InputValidator.checkNickname(nickname);

            String gameName = gameNameField.getText();
            InputValidator.checkGameName(gameName);

            eventBus.post(new NewGameEvent(
                    new GameState(new GameConfig(width, height, foodStatic, delay)),
                    gameName,
                    new PlayerInfo(nickname)));
            eventBus.post(new SwitchToGameFieldEvent());
        } catch (InvalidInputException e) {
            Informant.inform(e.getError(), e.getMessage());
        }
    }
}
