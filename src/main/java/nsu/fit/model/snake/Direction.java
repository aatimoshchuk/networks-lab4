package nsu.fit.model.snake;

import nsu.fit.model.Coordinate;

public enum Direction {
    UP(0),
    DOWN(1),
    LEFT(2),
    RIGHT(3);

    private static final int[] SHIFT_X = {0, 0, -1, 1};
    private static final int[] SHIFT_Y = {-1, 1, 0, 0};
    private final int directionIdentifier;

    Direction(int i) {
        directionIdentifier = i;
    }

    public Coordinate getShift() { return new Coordinate(SHIFT_X[directionIdentifier], SHIFT_Y[directionIdentifier]);}

    public Direction getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    public static Direction getDirectionFromShift(Coordinate coordinate) {
        if (coordinate.x() == 0 && coordinate.y() == -1) return UP;
        if (coordinate.x() == 1 && coordinate.y() ==  0) return RIGHT;
        if (coordinate.x() == 0 && coordinate.y() ==  1) return DOWN;
        if (coordinate.x() == -1 && coordinate.y() == 0) return LEFT;

        return null;
    }

    public Coordinate nextCoordinate(Coordinate oldCoordinate, int fieldWidth, int fieldHeight) {
        return new Coordinate(
                (oldCoordinate.x() + SHIFT_X[directionIdentifier] + fieldWidth) % fieldWidth,
                (oldCoordinate.y() + SHIFT_Y[directionIdentifier] + fieldHeight) % fieldHeight);
    }
}
