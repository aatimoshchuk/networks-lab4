package nsu.fit.controllers;


import com.google.common.eventbus.EventBus;
import javafx.fxml.FXML;
import nsu.fit.events.ExitEvent;
import nsu.fit.events.switching.SwitchToAvailableGamesListEvent;
import nsu.fit.events.switching.SwitchToNewGameConfigEvent;

public class MainMenuController {
    private EventBus eventBus;

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @FXML
    private void createNewGame() {
        eventBus.post(new SwitchToNewGameConfigEvent());
    }

    @FXML
    private void joinGame() {
        eventBus.post(new SwitchToAvailableGamesListEvent());
    }

    @FXML
    private void exit() {
        eventBus.post(new ExitEvent());
    }
}
