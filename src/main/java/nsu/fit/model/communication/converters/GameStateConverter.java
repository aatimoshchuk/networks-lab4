package nsu.fit.model.communication.converters;

import nsu.fit.model.Coordinate;
import nsu.fit.model.GameConfig;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.player.PlayerInfo;
import nsu.fit.model.snake.Snake;
import nsu.fit.protobuf.SnakesProto;

import java.util.List;

public class GameStateConverter {
    private static final GameStateConverter instance = new GameStateConverter();
    private GameStateConverter(){ }

    public static GameStateConverter getInstance(){
        return instance;
    }

    public GameState snakesProtoToGameState(SnakesProto.GameState gameState, GameConfig gameConfig){

        Coordinate fieldSize = new Coordinate(gameConfig.width(), gameConfig.height());

        List<Snake> snakes = gameState.getSnakesList().stream()
                .map(snake -> SnakeConverter.getInstance().snakesProtoToSnake(snake, fieldSize)).toList();

        List<Coordinate> food = gameState.getFoodsList().stream()
                .map(CoordinateConverter.getInstance()::snakesProtoToCoordinate).toList();

        List<PlayerInfo> players = PlayersConverter.getInstance().snakesProtoToPlayers(gameState.getPlayers())
                .stream().map(Player::getPlayerInfo).toList();

        GameState modelGameState = new GameState(gameConfig);
        modelGameState.addFood(food);
        modelGameState.addSnakes(snakes);
        modelGameState.addPlayerInfos(players);
        modelGameState.setStateOrder(gameState.getStateOrder());

        return modelGameState;
    }

    public SnakesProto.GameState gameStateToSnakesProto(GameState modelState, List<Player> gamePlayers){
        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();
        return builder.setStateOrder(modelState.getStateOrder())
                .setPlayers(PlayersConverter.getInstance().playersToSnakesProto(gamePlayers))
                .addAllFoods(modelState.getFood().stream().map(CoordinateConverter.getInstance()::coordinateToSnakeProto).toList())
                .addAllSnakes(modelState.getSnakes().stream().map(SnakeConverter.getInstance()::snakeToSnakesProto).toList())
                .build();
    }
}
