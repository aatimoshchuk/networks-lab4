package nsu.fit.model.snake;

import nsu.fit.model.Coordinate;

public class SnakeSegment {
    private Direction direction;
    private Coordinate coordinate;
    public SnakeSegment(Direction direction, Coordinate coordinate) {
        this.direction = direction;
        this.coordinate = coordinate;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Coordinate getCoordinate() { return coordinate;}

    public Direction getDirection() { return direction;}

    public void replace(int fieldWidth, int fieldHeight) {
        coordinate = direction.nextCoordinate(coordinate, fieldWidth, fieldHeight);
    }
}
