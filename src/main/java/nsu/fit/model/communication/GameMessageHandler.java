package nsu.fit.model.communication;

import com.google.common.eventbus.EventBus;
import nsu.fit.events.model.*;
import nsu.fit.model.GameConfig;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.converters.DirectionConverter;
import nsu.fit.model.communication.converters.GameStateConverter;
import nsu.fit.model.communication.converters.PlayersConverter;
import nsu.fit.model.communication.converters.RoleConverter;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.player.Role;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.model.communication.udp.UnicastMsgHandler;
import nsu.fit.model.snake.Direction;
import nsu.fit.protobuf.SnakesProto;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameMessageHandler extends Thread {
    private final EventBus modelEventBus;
    private final GameConfig gameConfig;

    public GameMessageHandler(EventBus modelEventBus, GameConfig gameConfig) {
        this.modelEventBus = modelEventBus;
        this.gameConfig = gameConfig;
    }


    @Override
    public void run() {
        try {
            UnicastMsgHandler unicastMsgHandler = UnicastMsgHandler.getInstance();

            while (!this.isInterrupted()) {
                Message message = UnicastMsgHandler.getInstance().receive();
                SnakesProto.GameMessage gameMessage = message.getMessage();
                Socket senderSocket = message.getSocket();

                if (gameMessage.hasSteer() && !this.isInterrupted()) {
                    SnakesProto.GameMessage.SteerMsg steerMsg = gameMessage.getSteer();
                    Direction newDirection = DirectionConverter
                            .getInstance()
                            .snakesProtoToDirection(steerMsg.getDirection());
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleSteerMsgEvent(newDirection,
                            senderSocket, gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasState() && !this.isInterrupted()) {
                    SnakesProto.GameState snakesProtoState = gameMessage.getState().getState();
                    GameState newState = GameStateConverter
                            .getInstance()
                            .snakesProtoToGameState(snakesProtoState, gameConfig);
                    List<Player> players = PlayersConverter
                            .getInstance()
                            .snakesProtoToPlayers(snakesProtoState.getPlayers());

                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleGameStateMsgEvent(newState, players,
                            senderSocket, gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasJoin() && !this.isInterrupted()) {
                    SnakesProto.GameMessage.JoinMsg joinMsg = gameMessage.getJoin();
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleJoinMsgEvent(joinMsg, senderSocket,
                            gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasPing() && !this.isInterrupted()) {
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandlePingMsgEvent(senderSocket,
                            gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasAck() && !this.isInterrupted()) {
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleAckMsgEvent(gameMessage.getSenderId(),
                            gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasError() && !this.isInterrupted()) {
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleErrorMsgEvent(senderSocket,
                            gameMessage.getMsgSeq())));
                    continue;
                }

                if (gameMessage.hasRoleChange() && !this.isInterrupted()) {
                    SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = gameMessage.getRoleChange();
                    Role senderRole = RoleConverter.getInstance().snakesProtoToRole(roleChangeMsg.getSenderRole());
                    Role receiverRole = RoleConverter.getInstance().snakesProtoToRole(roleChangeMsg.getReceiverRole());
                    CompletableFuture.runAsync(() -> modelEventBus.post(new HandleRoleChangeMsgEvent(senderRole,
                            receiverRole, gameMessage.getSenderId(), gameMessage.getReceiverId(), senderSocket,
                            gameMessage.getMsgSeq())));
                }
            }
        } catch (IOException ignored) { }
    }
}
