package nsu.fit.model.communication.converters;

import nsu.fit.model.GameAnnouncement;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.protobuf.SnakesProto;

public final class GameAnnouncementConverter {
    private static final GameAnnouncementConverter instance = new GameAnnouncementConverter();
    private GameAnnouncementConverter() { }

    public static GameAnnouncementConverter getInstance() {
        return instance;
    }

    public GameAnnouncement snakesProtoToGameAnnouncement(SnakesProto.GameAnnouncement gameAnnouncement,
                                                          Socket senderSocket) {
        return new GameAnnouncement(
                senderSocket,
                gameAnnouncement.getGameName(),
                gameAnnouncement.getPlayers().getPlayersCount(),
                GameConfigConverter.getInstance().snakesProtoToGameConfig(gameAnnouncement.getConfig()),
                gameAnnouncement.getCanJoin());
    }
}
