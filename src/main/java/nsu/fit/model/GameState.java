package nsu.fit.model;

import javafx.util.Pair;
import nsu.fit.model.communication.player.PlayerInfo;
import nsu.fit.model.field.FieldCreator;
import nsu.fit.model.field.cell.Cell;
import nsu.fit.model.field.cell.EmptyCell;
import nsu.fit.model.field.cell.FoodCell;
import nsu.fit.model.field.cell.SnakeCell;
import nsu.fit.model.snake.*;

import java.util.*;

public class GameState {
    private static final int SQUARE_SIZE = 5;
    private final GameConfig gameConfig;
    private final List<Snake> snakes = new ArrayList<>();
    private final List<PlayerInfo> playerInfos = new ArrayList<>();
    private final List<Coordinate> food = new ArrayList<>();
    private int stateOrder = 0;

    public GameState(GameConfig gameConfig) {
        this.gameConfig = gameConfig;
    }

    public GameConfig getGameConfig() { return gameConfig;}


    public List<Snake> getSnakes() { return Collections.unmodifiableList(snakes);}

    public List<Coordinate> getFood() { return Collections.unmodifiableList(food);}

    public List<PlayerInfo> getPlayers() { return Collections.unmodifiableList(playerInfos);}

    public int getStateOrder() {
        return stateOrder;
    }

    public void setStateOrder(int stateOrder) {
        this.stateOrder = stateOrder;
    }

    public void generateNewFood() {
        Cell[][] field = FieldCreator.getInstance().createField(this);
        int amountOfAliveSnakes = getAmountOfAliveSnakes();

        Random random = new Random();

        while (food.size() < gameConfig.foodStatic() + amountOfAliveSnakes) {
            int x = random.nextInt(gameConfig.width());
            int y = random.nextInt(gameConfig.height());

            if (field[x][y] instanceof EmptyCell) {
                field[x][y] = new FoodCell();
                food.add(new Coordinate(x, y));
            }
        }
    }

    public Pair<Coordinate, Direction> findFreeSquare() {
        Cell[][] field = FieldCreator.getInstance().createField(this);

        for (int x = 0; x < gameConfig.width(); x++) {
            for (int y = 0; y < gameConfig.height(); y++) {
                if (isSquareFree(field, x, y, gameConfig.width(), gameConfig.height())) {
                    int newSnakeHeadX = (x + SQUARE_SIZE / 2) % gameConfig.width();
                    int newSnakeHeadY = (y + SQUARE_SIZE / 2) % gameConfig.height();

                    if (field[newSnakeHeadX][newSnakeHeadY] instanceof EmptyCell) {
                        Coordinate newSnakeHead = new Coordinate(newSnakeHeadX, newSnakeHeadY);
                        Direction direction = chooseRandomDirection(newSnakeHead);

                        if (direction != null) {
                            return new Pair<>(newSnakeHead, direction);
                        }
                    }
                }
            }
        }

        return null;
    }

    public void addNewPlayer(PlayerInfo playerInfo, Coordinate snakeHead, Direction direction) {
        Snake snake = new Snake(snakeHead, direction, playerInfo.getId());
        snake.grow(gameConfig.width(), gameConfig.height());

        synchronized (snakes) {
            snakes.add(snake);
        }

        synchronized (playerInfos) {
            playerInfos.add(playerInfo);
        }
    }

    public void addFood(List<Coordinate> food) {
        this.food.addAll(food);
    }

    public void addSnakes(List<Snake> snakes) {
        this.snakes.addAll(snakes);
    }

    public void addPlayerInfos(List<PlayerInfo> playerInfosToAdd) {
        synchronized (playerInfos) {
            playerInfos.clear();
            playerInfos.addAll(playerInfosToAdd);
        }
    }

    public void handlePlayerLeave(int playerID) {
        synchronized (snakes) {
            snakes.stream()
                    .filter(snake -> snake.getPlayerID() == playerID).findAny()
                    .ifPresent(Snake::setZombie);

            synchronized (playerInfos) {
                playerInfos.removeIf(playerInfo -> playerInfo.getId() == playerID);
            }
        }
    }

    public List<Integer> change() {
        detectAteFood();
        List<Integer> removedPlayers = detectCollisions();
        generateNewFood();

        stateOrder++;

        return removedPlayers;
    }

    public void steerSnake(int snakeID, Direction newDirection) {
        findSnake(snakeID).ifPresent(snake -> snake.turn(newDirection));
    }

    private Optional<Snake> findSnake(int snakeID) {
        synchronized (snakes) {
            return snakes.stream().filter(snake -> snake.getPlayerID() == snakeID).findFirst();
        }
    }

    private void detectAteFood() {
        int fieldWidth = gameConfig.width();
        int fieldHeight = gameConfig.height();

        synchronized (snakes) {
            Set<Coordinate> ateFood = new HashSet<>();

            for (Snake snake : snakes) {
                snake.replaceAll(fieldWidth, fieldHeight);

                SnakeSegment head = snake.getHeadSegment();

                if (food.contains(head.getCoordinate())) {
                    snake.grow(fieldWidth, fieldHeight);
                    ateFood.add(head.getCoordinate());

                    increasePlayerScore(snake.getPlayerID());
                }
            }

            food.removeAll(ateFood);
        }
    }

    private void increasePlayerScore(int playerID) {
        PlayerInfo playerInfo = findPlayerByID(playerID).orElse(null);

        if (playerInfo != null) {
            playerInfo.increaseScore();
        }
    }

    private Optional<PlayerInfo> findPlayerByID(int playerID) {
        synchronized (playerInfos) {
            return playerInfos.stream().filter(player -> player.getId() == playerID).findFirst();
        }
    }

    private List<Integer> detectCollisions() {
        List<Collision> collisions = findMurders();
        List<Integer> ids = new ArrayList<>();

        for (Collision collision : collisions) {
            ids.add(collision.victimID());

            if (!collision.isSuicide()) {
                increasePlayerScore(collision.killerID());
            }

            removeSnake(collision.victimID());
        }

        return ids;
    }

    private List<Collision> findMurders() {
        synchronized (snakes) {
            List<Collision> collisions = new ArrayList<>();

            snakes.forEach(snake -> {
                for (Snake otherSnake : snakes) {
                    if (snake.isSuicide()) {
                        collisions.add(new Collision(snake.getPlayerID(), snake.getPlayerID()));
                        break;
                    }

                    if (otherSnake.getPlayerID() != snake.getPlayerID()
                            && otherSnake.isCollision(snake.getHeadSegment().getCoordinate())) {
                        collisions.add(new Collision(otherSnake.getPlayerID(), snake.getPlayerID()));
                    }
                }
            });

            return collisions;
        }
    }

    private void removeSnake(int playerID) {
        synchronized (snakes) {
            Snake snake = snakes.stream().filter(s -> s.getPlayerID() == playerID).findFirst().orElse(null);

            if (snake != null) {
                List<SnakeSegment> body = snake.getBody();
                Random random = new Random();

                for (int i = 1; i < body.size(); i++) {
                    if (random.nextBoolean()) {
                        food.add(body.get(i).getCoordinate());
                    }
                }

                snakes.remove(snake);
            }
        }
    }

    private int getAmountOfAliveSnakes() {
        synchronized (snakes) {
            int amount = 0;

            for (Snake snake : snakes) {
                if (snake.getState() == SnakeState.ALIVE) {
                    amount++;
                }
            }

            return amount;
        }
    }

    private boolean isSquareFree(Cell[][] field, int startX, int startY, int width, int height) {
        for (int dx = 0; dx < SQUARE_SIZE; dx++) {
            for (int dy = 0; dy < SQUARE_SIZE; dy++) {
                int currX = (startX + dx) % width;
                int currY = (startY + dy) % height;

                if ((field[currX][currY] instanceof SnakeCell)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Direction chooseRandomDirection(Coordinate snakeHead) {
        List<Direction> availableDirections = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            Coordinate nextCoordinate = direction.nextCoordinate(snakeHead, gameConfig.width(), gameConfig.height());

            if (!food.contains(nextCoordinate)) {
                availableDirections.add(direction);
            }
        }

        Random random = new Random();

        if (!availableDirections.isEmpty()) {
            return availableDirections.get(random.nextInt(availableDirections.size()));
        }

        return null;
    }
}
