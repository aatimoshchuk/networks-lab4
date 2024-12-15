package nsu.fit.model.snake;

import nsu.fit.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

public class Snake {
    private final int playerID;
    private final List<SnakeSegment> body = new ArrayList<>();
    private SnakeState state = SnakeState.ALIVE;

    public Snake(Coordinate headCoordinate, Direction headDirection, int playerID) {
        this.playerID = playerID;
        body.add(new SnakeSegment(headDirection, headCoordinate));
    }

    public List<SnakeSegment> getBody() { return body;}

    public int getPlayerID() { return playerID;}

    public SnakeState getState() { return state;}

    public SnakeSegment getHeadSegment() { return body.get(0);}

    public void  addNewSegment(Direction direction, int fieldWidth, int fieldHeight) {
        SnakeSegment lastSegment = body.get(body.size() - 1);
        Coordinate newCoordinate = direction.nextCoordinate(lastSegment.getCoordinate(), fieldWidth, fieldHeight);
        body.add(new SnakeSegment(direction.getOpposite(), newCoordinate));
    }

    public void grow(int fieldWidth, int fieldHeight) {
        Direction oppositeLastSegmentDirection = body.get(body.size() - 1).getDirection().getOpposite();
        addNewSegment(oppositeLastSegmentDirection, fieldWidth, fieldHeight);
    }

    public void turn(Direction newDirection) {
        if (state == SnakeState.ALIVE) {
            if (body.get(1).getDirection() == newDirection.getOpposite()) {
                return;
            }
        } else {
            return;
        }

        SnakeSegment head = body.get(0);
        head.setDirection(newDirection);
    }

    public void setZombie() { state = SnakeState.ZOMBIE;}

    public void replaceAll(int fieldWidth, int fieldHeight) {
        for (var segment : body) {
            segment.replace(fieldWidth, fieldHeight);
        }

        for (int i = body.size() - 1; i > 0; i--) {
            body.get(i).setDirection(body.get(i - 1).getDirection());
        }
    }

    public boolean isSuicide() {
        Coordinate head = getHeadSegment().getCoordinate();

        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i).getCoordinate())) {
                return true;
            }
        }

        return false;
    }

    public boolean isCollision(Coordinate coordinate) {
        return body.stream().anyMatch(snakeSegment -> snakeSegment.getCoordinate().equals(coordinate));
    }
}
