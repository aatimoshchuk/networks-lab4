package nsu.fit.model.field;

import nsu.fit.model.Coordinate;
import nsu.fit.model.GameConfig;
import nsu.fit.model.GameState;
import nsu.fit.model.field.cell.Cell;
import nsu.fit.model.field.cell.EmptyCell;
import nsu.fit.model.field.cell.FoodCell;
import nsu.fit.model.field.cell.SnakeCell;
import nsu.fit.model.snake.Snake;
import nsu.fit.model.snake.SnakeSegment;

import java.util.List;

public class FieldCreator {
    private static final FieldCreator instance = new FieldCreator();
    private FieldCreator() { }

    public static FieldCreator getInstance() { return instance;}

    public Cell[][] createField(GameState gameState) {
        int width = gameState.getGameConfig().width();
        int height = gameState.getGameConfig().height();

        Cell[][] field = new Cell[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                field[i][j] = new EmptyCell();
            }
        }

        List<Snake> snakes = gameState.getSnakes();

        snakes.forEach(snake -> {
            List<SnakeSegment> body = snake.getBody();
            body.forEach(snakeSegment -> {
                int x = snakeSegment.getCoordinate().x();
                int y = snakeSegment.getCoordinate().y();

                field[x][y] = new SnakeCell(snake.getPlayerID());
            });
        });

        List<Coordinate> foodCoordinates = gameState.getFood();

        foodCoordinates.forEach(food -> {
            int x = food.x();
            int y = food.y();

            field[x][y] = new FoodCell();
        });

        return field;
    }
}
