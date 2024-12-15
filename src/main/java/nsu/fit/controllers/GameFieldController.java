package nsu.fit.controllers;

import com.google.common.eventbus.EventBus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nsu.fit.controllers.util.Informant;
import nsu.fit.controllers.util.ViewConstants;
import nsu.fit.events.switching.SwitchToMainMenuEvent;
import nsu.fit.exceptions.GameException;
import nsu.fit.model.EventDispatcher;
import nsu.fit.model.GameAnnouncement;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.player.PlayerInfo;
import nsu.fit.model.field.FieldCreator;
import nsu.fit.model.field.cell.Cell;
import nsu.fit.model.field.cell.EmptyCell;
import nsu.fit.model.field.cell.FoodCell;
import nsu.fit.model.field.cell.SnakeCell;
import nsu.fit.model.snake.Direction;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

public class GameFieldController {
    private final RandomColorGenerator randomColorGenerator = new RandomColorGenerator();
    private final HashMap<Integer, Color> snakesColors = new HashMap<>();
    @FXML
    private Label gameNameLabel;
    @FXML
    private Canvas gameField;
    @FXML
    private ListView<PlayerInfo> gameScoresList;
    private EventDispatcher eventDispatcher;
    private EventBus eventBus;
    private Timeline animation;
    public void createNewGame(GameState gameState, PlayerInfo playerInfo, String gameName) {
        try {
            gameNameLabel.setText(gameName);

            eventDispatcher = new EventDispatcher();
            eventDispatcher.register();
            eventDispatcher.startHandleAnnouncementMessages();
            eventDispatcher.setControllersEventBus(eventBus);
            eventDispatcher.createNewGame(gameState, playerInfo, gameName);

            renderField(gameState);
            gameScoresList.setCellFactory(list -> new ColorRectCell());
            startAnimation(gameState.getGameConfig().delay());
        } catch (IOException | GameException e) {
            Informant.error(GameException.startingGameError().getMessage());
        }
    }

    public void joinGame(EventDispatcher eventDispatcher, GameAnnouncement gameAnnouncement, String nickname,
                         boolean isViewer) {
        try {
            gameNameLabel.setText(gameAnnouncement.gameName());
            this.eventDispatcher = eventDispatcher;
            gameScoresList.setCellFactory(list -> new ColorRectCell());
            eventDispatcher.joinGame(gameAnnouncement, nickname, isViewer);
        } catch (IOException | InterruptedException e) {
            Informant.error(GameException.startingGameError().getMessage());
        }
    }

    public void setEventBus(EventBus eventBus) { this.eventBus = eventBus;}

    public void renderField(GameState gameState) {
        int width = gameState.getGameConfig().width();
        int height = gameState.getGameConfig().height();

        double gameFieldWidth = gameField.getWidth();
        double gameFieldHeight = gameField.getHeight();

        double cellSize = Math.min(gameFieldWidth / width, gameFieldHeight / height);
        Cell[][] field = FieldCreator.getInstance().createField(gameState);

        GraphicsContext graphicsContext = gameField.getGraphicsContext2D();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Cell cell = field[i][j];

                if (cell instanceof EmptyCell) {
                    graphicsContext.setFill((i + j) % 2 == 0 ? Color.web(ViewConstants.BLACK_COLOR) :
                            Color.web(ViewConstants.DARK_GRAY_COLOR));
                } else if (cell instanceof FoodCell) {
                    graphicsContext.setFill(Color.web(ViewConstants.RED_COLOR));
                } else if (cell instanceof SnakeCell snakeCell) {
                    colorSnake(snakeCell, graphicsContext);
                }
                graphicsContext.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);

            }
        }
    }

    public void reloadGameScores() {
        ObservableList<PlayerInfo> playerInfos = FXCollections.observableList(eventDispatcher.getCurrentPlayers());
        gameScoresList.setItems(playerInfos);
    }

    public void startAnimation(int delay) {
        stopAnimation();
        animation = new Timeline(new KeyFrame(Duration.millis(delay), ae -> {
            eventDispatcher.changeGameState();
            reloadGameScores();
            renderField(eventDispatcher.getGameState());
        }));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    @FXML
    private void steerSnake(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case W -> eventDispatcher.steerSnake(Direction.UP);
            case A -> eventDispatcher.steerSnake(Direction.LEFT);
            case S -> eventDispatcher.steerSnake(Direction.DOWN);
            case D -> eventDispatcher.steerSnake(Direction.RIGHT);
        }
    }

    @FXML
    private void endGame() {
        stopAnimation();

        eventDispatcher.end();
        eventDispatcher = null;
        eventBus.post(new SwitchToMainMenuEvent());
    }

    private void colorSnake(SnakeCell snakeCell, GraphicsContext graphicsContext) {
        Color random = randomColorGenerator.generateRandomColor();
        Color current = snakesColors.putIfAbsent(snakeCell.getPlayerID(), random);

        graphicsContext.setFill(Objects.requireNonNullElse(current, random));
    }

    private void stopAnimation() {
        if (animation != null) {
            animation.stop();
        }
    }

    private static class RandomColorGenerator {
        public Color generateRandomColor() {
            Random random = new Random();

            return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        }
    }

    private class ColorRectCell extends ListCell<PlayerInfo> {
        @Override
        public void updateItem(PlayerInfo item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setBackground(null);
            } else {
                setText(item.toString());
                styleCell(item.getId());
            }
        }

        private void styleCell(int itemID) {
            Rectangle rect = new Rectangle(10, 10);
            rect.setFill(snakesColors.get(itemID));
            setGraphic(rect);

            if (itemID == eventDispatcher.getCurrPlayerInfo().getId()) {
                setBackground(Background.fill(Color.web(ViewConstants.LIGHT_GREEN_COLOR)));
            } else {
                setBackground(Background.fill(Color.web(ViewConstants.YELLOW_COLOR)));
            }
        }
    }
}
