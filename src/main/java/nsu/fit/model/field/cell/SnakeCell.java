package nsu.fit.model.field.cell;

public final class SnakeCell extends Cell {
    private final int playerID;

    public SnakeCell(int playerID) {
        this.playerID = playerID;
    }

    public int getPlayerID() { return playerID;}
}
