package nsu.fit.controllers;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nsu.fit.Main;
import nsu.fit.events.*;
import nsu.fit.events.switching.SwitchToAvailableGamesListEvent;
import nsu.fit.events.switching.SwitchToGameFieldEvent;
import nsu.fit.events.switching.SwitchToMainMenuEvent;
import nsu.fit.events.switching.SwitchToNewGameConfigEvent;
import nsu.fit.model.EventDispatcher;

import java.io.IOException;

public class MainController {
    private static final String MANE_MENU_FXML = "main_menu.fxml";
    private static final String NEW_GAME_CONFIG_FXML = "new_game_config.fxml";
    private static final String GAME_FIELD_FXML = "game_field.fxml";
    private static final String AVAILABLE_GAMES_LIST_FXML = "available_games_list.fxml";
    private static final MainController INSTANCE = new MainController();
    private final EventBus eventBus = new EventBus();
    private Stage stage;
    private Scene mainMenuScene;
    private Scene newGameConfigScene;
    private Scene gameFieldScene;
    private Scene availableGamesListScene;
    private GameFieldController gameFieldController;
    private AvailableGamesListController availableGamesListController;

    public MainController() { }

    public static MainController getInstance() {
        return INSTANCE;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(event -> {
            event.consume();
            exit(new ExitEvent());
        });
    }

    public void startApp() throws IOException {
        mainMenuScene = loadScene(MANE_MENU_FXML);
        newGameConfigScene = loadScene(NEW_GAME_CONFIG_FXML);
        gameFieldScene = loadScene(GAME_FIELD_FXML);
        availableGamesListScene = loadScene(AVAILABLE_GAMES_LIST_FXML);

        eventBus.register(this);
        eventBus.post(new SwitchToMainMenuEvent());

        stage.show();
    }

    @Subscribe
    public void switchToMainMenu(SwitchToMainMenuEvent switchToMainMenuEvent) {
        Platform.runLater(() -> stage.setScene(mainMenuScene));
    }

    @Subscribe
    public void switchToNewGameConfig(SwitchToNewGameConfigEvent switchToNewGameConfigEvent) {
        Platform.runLater(() -> stage.setScene(newGameConfigScene));
    }

    @Subscribe
    public void switchToAvailableGamesList(SwitchToAvailableGamesListEvent e) {
        try {
            EventDispatcher eventDispatcher = new EventDispatcher();
            eventDispatcher.register();
            eventDispatcher.startHandleAnnouncementMessages();

            availableGamesListController.setEventDispatcher(eventDispatcher);
            Platform.runLater(() -> stage.setScene(availableGamesListScene));
        } catch (IOException ignored) {

        }
    }

    @Subscribe
    public void switchToGameField(SwitchToGameFieldEvent e) {
        Platform.runLater(() -> stage.setScene(gameFieldScene));
    }

    @Subscribe
    public void newGame(NewGameEvent e) {
        gameFieldController.createNewGame(e.gameState(), e.playerInfo(), e.gameName());
    }

    @Subscribe
    public void joinGame(JoinGameEvent e) {
        gameFieldController.joinGame(e.eventDispatcher(), e.gameAnnouncement(), e.nickname(), e.isViewer());
    }

    @Subscribe
    public void exit(ExitEvent exitEvent) {
        stage.close();
        Platform.exit();
        System.exit(0);
    }

    @Subscribe
    public void updateAvailableList(UpdateAvailableGamesEvent e) {
        Platform.runLater(() -> availableGamesListController.updateListOfGames(e.availableGames()));
    }

    @Subscribe
    public void renderField(RenderGameFieldEvent e) {
        Platform.runLater(() -> gameFieldController.renderField(e.gameState()));
    }

    @Subscribe
    public void updateScores(UpdateGameScoresEvent e) {
        Platform.runLater(() -> gameFieldController.reloadGameScores());
    }

    @Subscribe
    public void startNewGameAnimation(StartNewGameAnimationEvent e){
        Platform.runLater(() -> gameFieldController.startAnimation(e.delay()));
    }

    private Scene loadScene(String fxmlSceneName) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlSceneName));
        Parent parent = loader.load();
        setController(loader);
        return new Scene(parent);
    }

    private void setController(FXMLLoader loader) {
        var controller = loader.getController();

        if (controller instanceof MainMenuController mainMenuController) {
            mainMenuController.setEventBus(eventBus);
        } else if (controller instanceof NewGameConfigController newGameConfigController) {
            newGameConfigController.setEventBus(eventBus);
        } else if (controller instanceof GameFieldController gameFieldController) {
            this.gameFieldController = gameFieldController;
            this.gameFieldController.setEventBus(eventBus);
        } else if (controller instanceof  AvailableGamesListController availableGamesListController) {
            this.availableGamesListController = availableGamesListController;
            this.availableGamesListController.setEventBus(eventBus);
        }
    }
}
