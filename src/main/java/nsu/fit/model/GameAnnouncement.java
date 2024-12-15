package nsu.fit.model;

import nsu.fit.model.communication.udp.Socket;

public record GameAnnouncement(Socket senderSocket, String gameName, int countOfPlayers, GameConfig gameConfig,
                               boolean canJoin) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GameAnnouncement that = (GameAnnouncement) o;
        return gameName.equals(that.gameName);
    }

    @Override
    public String toString() {
        return String.format("Name: %s\nPlayers: %d, Width: %d, Height: %d, Food: %d, Delay: %d",
                gameName,
                countOfPlayers,
                gameConfig().width(),
                gameConfig().height(),
                gameConfig().foodStatic(),
                gameConfig.delay());
    }
}
