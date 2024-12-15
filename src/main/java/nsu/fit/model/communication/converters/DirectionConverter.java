package nsu.fit.model.communication.converters;

import nsu.fit.model.snake.Direction;
import nsu.fit.protobuf.SnakesProto;

public class DirectionConverter {
    private static final DirectionConverter instance = new DirectionConverter();

    private DirectionConverter() { }

    public static DirectionConverter getInstance() { return instance;}

    public Direction snakesProtoToDirection(SnakesProto.Direction direction) {
        return switch (direction) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case LEFT -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
        };
    }

    public SnakesProto.Direction directionToSnakesProto(Direction direction) {
        return switch (direction) {
            case UP -> SnakesProto.Direction.UP;
            case DOWN -> SnakesProto.Direction.DOWN;
            case LEFT -> SnakesProto.Direction.LEFT;
            case RIGHT -> SnakesProto.Direction.RIGHT;
        };
    }
}
