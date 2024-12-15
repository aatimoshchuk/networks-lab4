package nsu.fit.controllers;

import com.google.common.eventbus.EventBus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import nsu.fit.controllers.util.Informant;
import nsu.fit.controllers.util.InputValidator;
import nsu.fit.events.JoinGameEvent;
import nsu.fit.events.switching.SwitchToMainMenuEvent;
import nsu.fit.exceptions.InvalidInputException;
import nsu.fit.model.EventDispatcher;
import nsu.fit.model.GameAnnouncement;
import nsu.fit.controllers.util.ViewConstants;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AvailableGamesListController {
    @FXML
    private ListView<GameAnnouncement> gamesList;
    @FXML
    private TextField nicknameField;
    @FXML
    private CheckBox viewerModeCheckBox;
    private EventBus eventBus;
    private EventDispatcher eventDispatcher;
    private ScheduledExecutorService scheduler;
    private boolean isCellFactorySet = false;
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        eventDispatcher.setControllersEventBus(eventBus);
        this.eventDispatcher = eventDispatcher;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(eventDispatcher::removeExpiredGames, 1, 1, TimeUnit.SECONDS);
    }

    public void updateListOfGames(List<GameAnnouncement> gameAnnouncementList) {
        if (!isCellFactorySet) {
            gamesList.setCellFactory(list -> new ColorGames());
            isCellFactorySet = true;
        }

        ObservableList<GameAnnouncement> items = FXCollections.observableArrayList(gameAnnouncementList);
        gamesList.setItems(items);
    }

    @FXML
    private void chooseGame() {
        try {
            GameAnnouncement chosenGame = gamesList.getSelectionModel().getSelectedItem();

            if (chosenGame != null) {
                String nickname = nicknameField.getText();
                InputValidator.checkNickname(nickname);

                eventBus.post(new JoinGameEvent(
                        eventDispatcher,
                        chosenGame,
                        nickname,
                        viewerModeCheckBox.isSelected()));

                scheduler.shutdownNow();
            }
        } catch (InvalidInputException e) {
            Informant.inform(e.getError(), e.getMessage());
        }
    }

    @FXML
    private void discover() throws IOException {
        eventDispatcher.sendDiscoverMsg();
    }

    @FXML
    private void back() {
        scheduler.shutdownNow();
        eventBus.post(new SwitchToMainMenuEvent());
    }

    private static class ColorGames extends ListCell<GameAnnouncement> {
        @Override
        public void updateItem(GameAnnouncement item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setBackground(null);
            } else {
                setText(item.toString());
                styleCell();
            }
        }

        private void styleCell() {
            setTextFill(Color.web(ViewConstants.YELLOW_COLOR));
            setFont(ViewConstants.AVAILABLE_GAMES_FONT);
            setBackground(Background.fill(Color.web(ViewConstants.DARK_GREEN_COLOR)));
            setStyle("-fx-border-color: " + ViewConstants.BROWN_COLOR + "; -fx-border-width: 2px;");
        }
    }
}
