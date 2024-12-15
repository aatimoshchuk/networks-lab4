package nsu.fit.model.snake;

public record Collision(int killerID, int victimID) {
    public boolean isSuicide() {
        return killerID == victimID;
    }
}
