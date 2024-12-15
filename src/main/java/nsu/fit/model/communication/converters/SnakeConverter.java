package nsu.fit.model.communication.converters;

import nsu.fit.model.Coordinate;
import nsu.fit.model.snake.Direction;
import nsu.fit.model.snake.Snake;
import nsu.fit.model.snake.SnakeSegment;
import nsu.fit.model.snake.SnakeState;
import nsu.fit.protobuf.SnakesProto;

import java.util.List;
import java.util.Objects;

public class SnakeConverter {
    private static final SnakeConverter instance = new SnakeConverter();

    private SnakeConverter() {
    }

    static SnakeConverter getInstance() {
        return instance;
    }

    public Snake snakesProtoToSnake(SnakesProto.GameState.Snake snake, Coordinate fieldSize) {
        Coordinate head = CoordinateConverter.getInstance().snakesProtoToCoordinate(snake.getPoints(0));
        Direction headDirection = DirectionConverter.getInstance().snakesProtoToDirection(snake.getHeadDirection());
        Snake modelSnake = new Snake(head, headDirection, snake.getPlayerId());

        Coordinate lastSegment = head;

        for (int i = 1; i < snake.getPointsCount(); ++i) {
            Coordinate currSegment = CoordinateConverter.getInstance().snakesProtoToCoordinate(snake.getPoints(i));
            if (currSegment.x() == 0) {
                for (int j = 0; j < Math.abs(currSegment.y()); j++) {
                    Coordinate shift = new Coordinate(0, currSegment.y() > 0 ? 1 : -1);
                    modelSnake.addNewSegment(Objects.requireNonNull(
                            Direction.getDirectionFromShift(shift)),
                            fieldSize.x(),
                            fieldSize.y());
                    lastSegment = new Coordinate(
                            lastSegment.x(),
                            (lastSegment.y() + shift.y() + fieldSize.y()) % fieldSize.y());
                }
            } else if (currSegment.y() == 0) {
                for (int j = 0; j < Math.abs(currSegment.x()); j++) {
                    Coordinate shift = new Coordinate(currSegment.x() > 0 ? 1 : -1,0);
                    modelSnake.addNewSegment(Objects.requireNonNull(
                            Direction.getDirectionFromShift(shift)),
                            fieldSize.x(),
                            fieldSize.y());
                    lastSegment = new Coordinate(
                            (lastSegment.x() + shift.x() + fieldSize.x()) % fieldSize.x(),
                            lastSegment.y());
                }
            } else {
                return null;
            }
            lastSegment = currSegment;
        }

        if (SnakesStateConverter.snakesProtoToSnakesState(snake.getState()) == SnakeState.ZOMBIE) {
            modelSnake.setZombie();
        }

        return modelSnake;
    }

    public SnakesProto.GameState.Snake snakeToSnakesProto(Snake modelSnake) {
        SnakesProto.GameState.Snake.Builder snakeBuilder = SnakesProto.GameState.Snake.newBuilder();

        SnakesProto.GameState.Snake.SnakeState state = SnakesStateConverter.snakesStateToSnakesProto(modelSnake.getState());
        SnakesProto.Direction headDirection =
                DirectionConverter.getInstance().directionToSnakesProto(modelSnake.getHeadSegment().getDirection());

        snakeBuilder.setState(state).setHeadDirection(headDirection)
                .setPlayerId(modelSnake.getPlayerID());

        SnakesProto.GameState.Coord headCoord =
                CoordinateConverter.getInstance().coordinateToSnakeProto(modelSnake.getHeadSegment().getCoordinate());
        snakeBuilder.addPoints(headCoord);

        List<SnakeSegment> body = modelSnake.getBody();
        for (int i = 1; i < body.size(); ++i) {
            Coordinate shift = body.get(i).getDirection().getOpposite().getShift();
            snakeBuilder.addPoints(CoordinateConverter.getInstance().coordinateToSnakeProto(shift));
        }
        return snakeBuilder.build();
    }
}
