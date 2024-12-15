package nsu.fit.model.communication;

import nsu.fit.model.GameConfig;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.converters.*;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.player.Role;
import nsu.fit.model.snake.Direction;
import nsu.fit.protobuf.SnakesProto;

import java.util.List;

public class GameMessageCreator {
    private static final GameMessageCreator instance = new GameMessageCreator();

    private GameMessageCreator() { }

    public static GameMessageCreator getInstance() { return instance;}

    public SnakesProto.GameMessage createGameStateMsg(int senderID, int receiverID, GameState gameState,
                                                      List<Player> players, long msgSeq) {
        SnakesProto.GameState protoGameState = GameStateConverter
                .getInstance()
                .gameStateToSnakesProto(gameState, players);

        return SnakesProto.GameMessage.newBuilder()
                .setState(SnakesProto.GameMessage.StateMsg.newBuilder().setState(protoGameState).build())
                .setMsgSeq(msgSeq)
                .setSenderId(senderID)
                .setReceiverId(receiverID)
                .build();
    }

    public SnakesProto.GameMessage createSteerMsg(Direction direction, int senderID, long msgSeq) {
        SnakesProto.Direction protoDirection = DirectionConverter.getInstance().directionToSnakesProto(direction);

        return SnakesProto.GameMessage.newBuilder()
                .setSteer(SnakesProto.GameMessage.SteerMsg.newBuilder().setDirection(protoDirection).build())
                .setSenderId(senderID)
                .setMsgSeq(msgSeq)
                .build();
    }

    public SnakesProto.GameMessage createAnnouncementMsg(GameConfig gameConfig, List<Player> players,
                                                         String gameName, boolean canJoin, long msgSeq) {
        SnakesProto.GameAnnouncement announcement = SnakesProto.GameAnnouncement.newBuilder()
                .setGameName(gameName)
                .setCanJoin(canJoin)
                .setConfig(GameConfigConverter.getInstance().gameConfigToSnakesProto(gameConfig))
                .setPlayers(PlayersConverter.getInstance().playersToSnakesProto(players))
                .build();

        return SnakesProto.GameMessage.newBuilder()
                .setAnnouncement(SnakesProto.GameMessage.AnnouncementMsg.newBuilder().addGames(announcement).build())
                .setMsgSeq(msgSeq)
                .build();
    }

    public SnakesProto.GameMessage createJoinMsg(String gameName, String playerNickname, Role role, long msgSeq) {
        SnakesProto.GameMessage.JoinMsg joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder()
                .setGameName(gameName)
                .setPlayerName(playerNickname)
                .setPlayerType(SnakesProto.PlayerType.HUMAN)
                .setRequestedRole(RoleConverter.getInstance().roleToSnakesProto(role))
                .build();

        return SnakesProto.GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(msgSeq)
                .build();
    }

    public SnakesProto.GameMessage createAckMsg(int senderID, int receiverID, long msgSeq) {
        return SnakesProto.GameMessage.newBuilder()
                .setAck(SnakesProto.GameMessage.AckMsg.newBuilder().build())
                .setMsgSeq(msgSeq)
                .setSenderId(senderID)
                .setReceiverId(receiverID)
                .build();
    }

    public SnakesProto.GameMessage createPingMsg(int senderId, int receiverId, long msgSeq) {
        return SnakesProto.GameMessage.newBuilder()
                .setPing(SnakesProto.GameMessage.PingMsg.newBuilder().build())
                .setMsgSeq(msgSeq)
                .setSenderId(senderId)
                .setReceiverId(receiverId)
                .build();
    }

    public SnakesProto.GameMessage createRoleChangedMsg(Role senderRole, Role receiverRole,
                                                        int senderID, int receiverID, long msgSeq) {
        SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(RoleConverter.getInstance().roleToSnakesProto(receiverRole))
                .setSenderRole(RoleConverter.getInstance().roleToSnakesProto(senderRole))
                .build();

        return SnakesProto.GameMessage.newBuilder()
                .setRoleChange(roleChangeMsg)
                .setMsgSeq(msgSeq)
                .setReceiverId(receiverID)
                .setSenderId(senderID)
                .build();
    }

    public SnakesProto.GameMessage createErrorMsg(String cause, long msgSeq) {
        return SnakesProto.GameMessage.newBuilder()
                .setError(SnakesProto.GameMessage.ErrorMsg.newBuilder().setErrorMessage(cause).build())
                .setMsgSeq(msgSeq)
                .build();
    }

    public SnakesProto.GameMessage createDiscoverMsg(long msgSeq) {
        return SnakesProto.GameMessage.newBuilder()
                .setDiscover(SnakesProto.GameMessage.DiscoverMsg.getDefaultInstance())
                .setMsgSeq(msgSeq)
                .build();
    }
}
