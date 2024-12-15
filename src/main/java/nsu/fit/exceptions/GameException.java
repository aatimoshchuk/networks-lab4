package nsu.fit.exceptions;

public class GameException extends Exception {
    private GameException(String message) {
        super(message);
    }

    public static GameException startingGameError() {
        return new GameException("Error while starting game");
    }
}
