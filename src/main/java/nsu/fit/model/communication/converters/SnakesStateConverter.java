package nsu.fit.model.communication.converters;

import nsu.fit.model.snake.SnakeState;
import nsu.fit.protobuf.SnakesProto;

public class SnakesStateConverter {
    private final static SnakesStateConverter instance = new SnakesStateConverter();

    private SnakesStateConverter() {
    }

    public SnakesStateConverter getInstance() {
        return instance;
    }

    public static SnakeState snakesProtoToSnakesState(SnakesProto.GameState.Snake.SnakeState snakeState) {
        return switch (snakeState) {
            case ALIVE -> SnakeState.ALIVE;
            case ZOMBIE -> SnakeState.ZOMBIE;
        };
    }

    public static SnakesProto.GameState.Snake.SnakeState snakesStateToSnakesProto(SnakeState modelState) {
        return switch (modelState) {
            case ALIVE -> SnakesProto.GameState.Snake.SnakeState.ALIVE;
            case ZOMBIE -> SnakesProto.GameState.Snake.SnakeState.ZOMBIE;
        };
    }
}
