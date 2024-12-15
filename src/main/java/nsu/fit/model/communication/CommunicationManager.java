package nsu.fit.model.communication;

import com.google.common.eventbus.EventBus;
import javafx.util.Pair;
import nsu.fit.events.model.StartMasterRoutineEvent;
import nsu.fit.model.Coordinate;
import nsu.fit.model.GameAnnouncement;
import nsu.fit.model.GameState;
import nsu.fit.model.communication.converters.RoleConverter;
import nsu.fit.model.communication.player.*;
import nsu.fit.model.communication.udp.MulticastConfig;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.model.communication.udp.UnicastMsgHandler;
import nsu.fit.model.snake.Direction;
import nsu.fit.protobuf.SnakesProto;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CommunicationManager {
    private static final int ATTEMPTS_TO_JOIN = 100;
    private final UnicastMsgHandler unicastMsgHandler;
    private final Socket multicastSocket;
    private final long delay;
    private final long pingInterval;

    private final Map<Integer, Long> lastSteerMsgSeq = new HashMap<>();
    private final EventBus modelEventBus;
    private final PlayersStorage playersStorage = new PlayersStorage();
    private final AtomicInteger msgSeq = new AtomicInteger(0);
    private final UnconfirmedGameMessageStorage unconfirmedGameMessageStorage = new UnconfirmedGameMessageStorage();
    private CommunicationManager(UnicastMsgHandler unicastMsgHandler, InetAddress multicastInetAddress, int delay,
                                 EventBus modelEventBus) {
        this.unicastMsgHandler = unicastMsgHandler;
        this.multicastSocket = new Socket(multicastInetAddress, MulticastConfig.MULTICAST_PORT);
        this.delay = delay;
        this.pingInterval = delay / 10;
        this.modelEventBus = modelEventBus;
    }

    public long getDelay() { return delay;}

    public long getPingInterval() { return pingInterval;}

    public static CommunicationManager createManager(int delay, EventBus modelEventBus) throws IOException {
        return new CommunicationManager(
                UnicastMsgHandler.getInstance(),
                InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS),
                delay,
                modelEventBus);
    }

    public void close(PlayerInfo currPlayerInfo) {
        synchronized (playersStorage) {
            Player master = playersStorage.findPlayerByRole(Role.MASTER).orElse(null);

            if (master != null) {
                if (master.getID() == currPlayerInfo.getId()) {
                    Player deputy = playersStorage.findPlayerByRole(Role.DEPUTY).orElse(null);

                    if (deputy != null) {
                        deputy.setRole(Role.MASTER);
                        sendRoleChangeMsg(
                                Role.VIEWER,
                                Role.MASTER,
                                currPlayerInfo.getId(),
                                deputy.getID(),
                                deputy.getSocket());
                    }
                } else {
                    sendRoleChangeMsg(
                            Role.VIEWER,
                            Role.MASTER,
                            currPlayerInfo.getId(),
                            master.getID(),
                            master.getSocket());
                }
            }
        }
        unicastMsgHandler.close();
    }

    public void nextGameState(PlayerInfo currPlayerInfo, GameState gameState) {
        if (playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())) {
            List<Integer> ids = gameState.change();

            synchronized (playersStorage) {
                handleDeadPlayers(currPlayerInfo, ids);
                gameState.addPlayerInfos(playersStorage.getPlayers().stream().map(Player::getPlayerInfo).toList());
                playersStorage.getPlayers().forEach(player -> {
                    if (player.getID() != currPlayerInfo.getId()) {
                        sendGameStateMsg(currPlayerInfo.getId(), player.getID(), gameState, player);
                    }
                });
            }
        }
    }

    public void steer(Direction newDirection, PlayerInfo currPlayerInfo, GameState gameState) {
        synchronized (playersStorage) {
            Player master = playersStorage.findPlayerByRole(Role.MASTER).orElse(null);
            if (master != null) {
                if (master.getID() == currPlayerInfo.getId()) {
                    gameState.steerSnake(currPlayerInfo.getId(), newDirection);
                } else {
                    sendSteerMsg(newDirection, currPlayerInfo, master);
                }
            }
        }
    }

    public void addMaster(Player master) {
        synchronized (playersStorage) {
            if (playersStorage.findPlayerByRole(Role.MASTER).isEmpty()
                    && playersStorage.findPlayerByID(master.getID()).isEmpty()) {
                master.setRole(Role.MASTER);
                playersStorage.addPlayer(master);
            }
        }
    }

    public void multicastGameAnnounce(GameState gameState, PlayerInfo playerInfo, String gameName) {
        if (gameState != null && playersStorage.isItCurrentRole(Role.MASTER, playerInfo.getId())) {
            sendGameAnnouncementMsg(multicastSocket, gameState, gameName);
        }
    }

    public void unicastGameAnnounce(Socket socket, PlayerInfo currPlayerInfo, GameState gameState, String gameName) {
        if (gameState != null && playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())) {
            sendGameAnnouncementMsg(socket, gameState, gameName);
        }
    }

    public void resendUnconfirmed() {
        synchronized (unconfirmedGameMessageStorage) {
            unconfirmedGameMessageStorage.getUnconfirmedMessages().forEach(message -> {
                long instant = Instant.now().toEpochMilli();

                if (instant - message.getSentAt() > pingInterval) {
                    if (message.getSocket().port() == 0) {
                        unconfirmedGameMessageStorage.confirmMessage(message.getMessage().getMsgSeq());
                    } else {
                        CompletableFuture.runAsync(() -> sendMessage(message));
                    }
                }
            });
        }
    }

    public void findExpired(PlayerInfo currPlayer, GameState gameState) {
        synchronized (playersStorage) {
            playersStorage.findForLastCommunicationPlayer(delay, currPlayer.getId()).forEach(expiredPlayer -> {
                gameState.handlePlayerLeave(expiredPlayer.getID());
                this.handleExpire(expiredPlayer, currPlayer);
            });
        }
    }

    public void sendJoinMsg(GameAnnouncement gameAnnouncement, String nickname, Role role) {
        SnakesProto.GameMessage joinMsg = GameMessageCreator.getInstance().createJoinMsg(
                gameAnnouncement.gameName(),
                nickname,
                role,
                nextMsgSeq());

        Message message = new Message(joinMsg, gameAnnouncement.senderSocket());

        synchronized (unconfirmedGameMessageStorage) {
            unconfirmedGameMessageStorage.addMessage(message);
        }

        sendMessage(message);
    }

    public void handleJoinMessage(SnakesProto.GameMessage.JoinMsg joinMsg, Socket senderSocket, long msgSeq,
                                  GameState gameState, PlayerInfo currPlayerInfo) {

        Role role = RoleConverter.getInstance().snakesProtoToRole(joinMsg.getRequestedRole());

        if (role != Role.NORMAL && role != Role.VIEWER) {
            return;
        }

        synchronized (playersStorage) {
            Player sender = playersStorage.findPlayerBySocket(senderSocket).orElse(null);

            if (sender != null) {
                sendAckMsg(currPlayerInfo.getId(), sender.getID(), senderSocket, msgSeq);
                return;
            }

            if (playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())) {
                Player newPlayer = new Player(
                        new PlayerInfo(joinMsg.getPlayerName()),
                        new InetInfo(senderSocket, -1),
                        role);

                if (role != Role.VIEWER) {
                    Pair<Coordinate, Direction> freeSquare = gameState.findFreeSquare();

                    if (freeSquare != null) {
                        gameState.addNewPlayer(newPlayer.getPlayerInfo(), freeSquare.getKey(), freeSquare.getValue());

                        if (playersStorage.findPlayerByRole(Role.DEPUTY).isEmpty()) {
                            newPlayer.setRole(Role.DEPUTY);
                        }
                    } else {
                        sendErrorMsg("Can't find free area", newPlayer.getSocket());
                        return;
                    }
                }

                playersStorage.addPlayer(newPlayer);

                sendAckMsg(currPlayerInfo.getId(), newPlayer.getID(), senderSocket, msgSeq);
            }
        }
    }

    public int receiveAskMsgAfterJoinMsg(Role role) throws IOException {
        synchronized (playersStorage) {
            Message response = unicastMsgHandler.receive();
            SnakesProto.GameMessage gameMsg = response.getMessage();

            for (int i = 0; i < ATTEMPTS_TO_JOIN; i++) {
                if (gameMsg.hasAck()) {
                    synchronized (unconfirmedGameMessageStorage) {
                        unconfirmedGameMessageStorage.confirmMessage(gameMsg.getMsgSeq());
                    }
                    Player master = new Player(
                            new PlayerInfo("", gameMsg.getSenderId()),
                            new InetInfo(response.getSocket(), -1),
                            Role.MASTER);

                    Player currPlayer = new Player(
                            new PlayerInfo("", gameMsg.getReceiverId()),
                            new InetInfo(InetAddress.getLocalHost(), 0, -1),
                            role);

                    addMaster(master);

                    if (!currPlayer.equals(master)) {
                        playersStorage.addPlayer(currPlayer);
                    }

                    return gameMsg.getReceiverId();
                }
                if (gameMsg.hasError()) {
                    handleErrorMsg(response.getSocket(), new PlayerInfo("", 0), gameMsg.getMsgSeq());
                    return -1;
                }
            }
            return -1;
        }
    }

    public void sendPingMessagesToPlayers(PlayerInfo currPlayer) {
        synchronized (playersStorage) {
            if (!playersStorage.isItCurrentRole(Role.MASTER, currPlayer.getId())) {
                Player master = playersStorage.findPlayerByRole(Role.MASTER).orElse(null);

                if (master != null && master.getLastCommunicationTime() > pingInterval) {
                    sendPingMsg(currPlayer.getId(), master.getID(), master.getSocket());
                }
            } else {
                List<Player> pingMsgReceivers = playersStorage.findForLastCommunicationPlayer(
                        pingInterval,
                        currPlayer.getId());

                for (Player pingMsgReceiver : pingMsgReceivers) {
                    if (pingMsgReceiver.getID() != currPlayer.getId()) {
                        sendPingMsg(currPlayer.getId(), pingMsgReceiver.getID(), pingMsgReceiver.getSocket());
                    }
                }
            }
        }
    }

    private void sendGameAnnouncementMsg(Socket socket, GameState gameState, String gameName) {
        CompletableFuture.runAsync(() -> {
            synchronized (playersStorage) {
                SnakesProto.GameMessage announcementMsg = GameMessageCreator.getInstance().createAnnouncementMsg(
                        gameState.getGameConfig(),
                        playersStorage.getPlayers(),
                        gameName,
                        true,
                        nextMsgSeq());

                sendMessage(new Message(announcementMsg, socket));
            }
        });
    }

    private void sendPingMsg(int senderID, int receiverID, Socket socket) {
        CompletableFuture.runAsync(() -> {
            synchronized (playersStorage) {
                SnakesProto.GameMessage pingMsg = GameMessageCreator.getInstance().createPingMsg(
                        senderID,
                        receiverID,
                        nextMsgSeq());

                Message message = new Message(pingMsg, socket);

                synchronized (unconfirmedGameMessageStorage) {
                    unconfirmedGameMessageStorage.addMessage(message);
                }

                sendMessage(message);
            }
        });
    }

    public void handlePingMsg(Socket senderSocket, PlayerInfo currPlayerInfo, long messageSeq) {
        synchronized (playersStorage) {
            Player gamePlayer = playersStorage.findPlayerBySocket(senderSocket).orElse(null);

            if (gamePlayer != null) {
                gamePlayer.updateLastCommunicationTime();
                sendAckMsg(currPlayerInfo.getId(), gamePlayer.getID(), senderSocket, messageSeq);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void sendRoleChangeMsg(Role senderRole, Role receiverRole, int senderID, int receiverID,
                                   Socket socketToSend) {
        SnakesProto.GameMessage roleChangeMsg = GameMessageCreator.getInstance().createRoleChangedMsg(
                senderRole,
                receiverRole,
                senderID,
                receiverID,
                nextMsgSeq());

        Message message = new Message(roleChangeMsg, socketToSend);

        synchronized (unconfirmedGameMessageStorage) {
            unconfirmedGameMessageStorage.addMessage(message);
        }

        sendMessage(message);
    }

    public void handleRoleChangeMsg(Role senderRole, Role receiverRole,
                                    int senderId, int receiverId, long messageSeq, PlayerInfo currPlayerInfo,
                                    Socket senderSocket) {
        synchronized (playersStorage) {
            if (playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())
                    && playersStorage.isItCurrentRole(Role.DEPUTY, senderId) && senderRole == Role.VIEWER) {
                playersStorage.findPlayerByRole(Role.NORMAL).ifPresent(p -> {
                    p.setRole(Role.DEPUTY);
                    sendRoleChangeMsg(Role.MASTER, Role.DEPUTY, currPlayerInfo.getId(), p.getID(), p.getSocket());
                });
            } else if (playersStorage.isItCurrentRole(Role.DEPUTY, currPlayerInfo.getId())
                    && receiverRole == Role.MASTER) {
                modelEventBus.post(new StartMasterRoutineEvent());

                playersStorage.findPlayerByRole(Role.NORMAL).ifPresent(p -> {
                    p.setRole(Role.DEPUTY);
                    sendRoleChangeMsg(Role.MASTER, Role.DEPUTY, currPlayerInfo.getId(), p.getID(), p.getSocket());
                });

            }

            playersStorage.findPlayerByID(senderId).ifPresent(p -> {
                p.setRole(senderRole);
                p.updateLastCommunicationTime();
            });

            playersStorage.findPlayerByID(receiverId).ifPresent(p -> p.setRole(receiverRole));
            sendAckMsg(currPlayerInfo.getId(), senderId, senderSocket, messageSeq);
        }
    }

    private void sendAckMsg(int senderID, int receiverID, Socket socketToSend, long messageSeq) {
        CompletableFuture.runAsync(() -> {
            SnakesProto.GameMessage ackMsg = GameMessageCreator.getInstance().createAckMsg(
                    senderID,
                    receiverID,
                    messageSeq);

            sendMessage(new Message(ackMsg, socketToSend));
        });
    }

    public void handleAckMsg(int senderId, long messageSeq) {
        synchronized (unconfirmedGameMessageStorage) {
            unconfirmedGameMessageStorage.confirmMessage(messageSeq);
        }
        synchronized (playersStorage) {
            playersStorage.findPlayerByID(senderId).ifPresent(Player::updateLastCommunicationTime);
        }
    }

    private void sendGameStateMsg(int senderID, int receiverID, GameState gameState, Player player) {
        CompletableFuture.runAsync(() -> {
            synchronized (playersStorage) {
                SnakesProto.GameMessage gameStateMsg = GameMessageCreator.getInstance().createGameStateMsg(
                        senderID,
                        receiverID,
                        gameState,
                        playersStorage.getPlayers(),
                        nextMsgSeq());

                Message message = new Message(gameStateMsg, player.getSocket());

                synchronized (unconfirmedGameMessageStorage) {
                    unconfirmedGameMessageStorage.addMessage(message);
                }
                sendMessage(message);
            }
        });
    }

    public boolean handleGameStateMessage(GameState currentGameState, GameState newState, List<Player> players,
                                          Socket senderSocket, PlayerInfo currPlayerInfo, long msgSeq) {
        synchronized (playersStorage) {
            Player maybeMaster = playersStorage.findPlayerBySocket(senderSocket).orElse(null);
            if (maybeMaster == null) {
                return false;
            }

            maybeMaster.updateLastCommunicationTime();

            if (newState.getStateOrder() > currentGameState.getStateOrder()) {
                playersStorage.setPlayers(players.stream().peek(Player::updateLastCommunicationTime).toList());
                Player master = playersStorage.findPlayerByRole(Role.MASTER).orElse(null);
                if (master == null) {
                    return false;
                }
                playersStorage.deletePlayer(master);
                playersStorage.addPlayer(new Player(master.getPlayerInfo(), new InetInfo(senderSocket, Instant.now().toEpochMilli()), Role.MASTER
                        ));
                sendAckMsg(currPlayerInfo.getId(), master.getID(), senderSocket, msgSeq);
                return true;
            }
            players.stream().filter(p -> p.getRole() == Role.MASTER).findAny().ifPresent(m ->  sendAckMsg(currPlayerInfo.getId(), m.getID(), senderSocket, msgSeq));
            return false;
        }
    }

    private void sendSteerMsg(Direction newDirection, PlayerInfo currPlayerInfo, Player master) {
        CompletableFuture.runAsync(() -> {
            synchronized (playersStorage) {
                SnakesProto.GameMessage steerMsg = GameMessageCreator.getInstance().createSteerMsg(
                        newDirection,
                        currPlayerInfo.getId(),
                        nextMsgSeq());

                Message message = new Message(steerMsg, master.getSocket());

                synchronized (unconfirmedGameMessageStorage) {
                    unconfirmedGameMessageStorage.addMessage(message);
                }
                sendMessage(message);
            }
        });
    }

    public void handleSteerMsg(GameState gameState, Direction newDirection,
                               Socket senderSocket, PlayerInfo currPlayerInfo, long msgSeq) {
        synchronized (playersStorage) {
            if (playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())) {
                Player gamePlayer = playersStorage.findPlayerBySocket(senderSocket).orElse(null);

                if (gamePlayer != null) {
                    Long lastMsgSeq = lastSteerMsgSeq.get(gamePlayer.getID());

                    if (lastMsgSeq != null && msgSeq < lastMsgSeq) {
                        return;
                    }

                    lastSteerMsgSeq.put(gamePlayer.getID(), msgSeq);
                    gameState.steerSnake(gamePlayer.getID(), newDirection);
                    gamePlayer.updateLastCommunicationTime();

                    sendAckMsg(currPlayerInfo.getId(), gamePlayer.getID(), senderSocket, msgSeq);
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void sendErrorMsg(String cause, Socket toSendSocket) {
        CompletableFuture.runAsync(() -> {
            SnakesProto.GameMessage errorMsg = GameMessageCreator.getInstance().createErrorMsg(
                    cause,
                    nextMsgSeq());

            Message message = new Message(errorMsg, toSendSocket);

            synchronized (unconfirmedGameMessageStorage) {
                unconfirmedGameMessageStorage.addMessage(message);
            }

            sendMessage(message);
        });
    }

    public void handleErrorMsg(Socket senderSocket, PlayerInfo me, long messageSeq) {
        synchronized (playersStorage) {
            Player player = playersStorage.findPlayerBySocket(senderSocket).orElse(null);
            if (player != null) {
                player.updateLastCommunicationTime();
                sendAckMsg(me.getId(), player.getID(), senderSocket, messageSeq);
            }
        }
    }

    private int nextMsgSeq() { return msgSeq.incrementAndGet();}

    private void sendMessage(Message message) {
        try {
            unicastMsgHandler.send(message);
            message.setSentAt(Instant.now().toEpochMilli());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeadPlayers(PlayerInfo currPlayerInfo, List<Integer> deadPlayersIDs) {
        synchronized (playersStorage) {
            for (int id : deadPlayersIDs) {
                if (currPlayerInfo.getId() != id) {
                    Player player = playersStorage.findPlayerByID(id).orElse(null);

                    if (player == null) {
                        continue;
                    }

                    if (player.getRole() == Role.DEPUTY) {
                        playersStorage.findPlayerByRole(Role.NORMAL).ifPresent(p -> {
                            p.setRole(Role.DEPUTY);
                            sendRoleChangeMsg(
                                    Role.MASTER,
                                    Role.DEPUTY,
                                    currPlayerInfo.getId(),
                                    p.getID(),
                                    p.getSocket());
                        });
                    }

                    player.setRole(Role.VIEWER);
                    sendRoleChangeMsg(Role.MASTER, Role.VIEWER, currPlayerInfo.getId(), id, player.getSocket());
                }
            }
        }
    }

    private void handleExpire(Player expiredPlayer, PlayerInfo currPlayerInfo) {
        synchronized (playersStorage) {
            if (playersStorage.isItCurrentRole(Role.MASTER, currPlayerInfo.getId())) {
                synchronized (unconfirmedGameMessageStorage) {
                    unconfirmedGameMessageStorage.cancelConfirmation(expiredPlayer.getID());
                }

                if (expiredPlayer.getRole() == Role.DEPUTY) {
                    playersStorage.findPlayerByRole(Role.NORMAL).ifPresent(p -> {
                        p.setRole(Role.DEPUTY);
                        sendRoleChangeMsg(Role.MASTER, Role.DEPUTY, currPlayerInfo.getId(), p.getID(), p.getSocket());
                    });
                }
                playersStorage.deletePlayer(expiredPlayer);
                return;
            }

            if (playersStorage.isItCurrentRole(Role.NORMAL, currPlayerInfo.getId())
                    && expiredPlayer.getRole() == Role.MASTER) {
                Player deputy = playersStorage.findPlayerByRole(Role.DEPUTY).orElse(null);

                if (deputy != null) {
                    synchronized (unconfirmedGameMessageStorage) {
                        unconfirmedGameMessageStorage.replaceDestination(expiredPlayer.getID(), deputy.getSocket());
                    }
                    deputy.setRole(Role.MASTER);
                    playersStorage.deletePlayer(expiredPlayer);
                }

                return;
            }

            if (playersStorage.isItCurrentRole(Role.DEPUTY, currPlayerInfo.getId())
                    && expiredPlayer.getRole() == Role.MASTER) {
                synchronized (unconfirmedGameMessageStorage) {
                    unconfirmedGameMessageStorage.cancelConfirmation(expiredPlayer.getID());
                }
                Player currPlayer = playersStorage.findPlayerByID(currPlayerInfo.getId()).orElse(null);

                if (currPlayer != null) {
                    currPlayer.setRole(Role.MASTER);
                    playersStorage.findPlayerByRole(Role.NORMAL).ifPresent(p -> p.setRole(Role.DEPUTY));
                    playersStorage.getPlayers().forEach(player -> {
                        if (player.getID() != currPlayerInfo.getId()) {
                            sendRoleChangeMsg(Role.MASTER, player.getRole(), currPlayerInfo.getId(), player.getID(),
                                    player.getSocket());
                            player.updateLastCommunicationTime();
                        }
                    });
                    playersStorage.deletePlayer(expiredPlayer);
                    modelEventBus.post(new StartMasterRoutineEvent());
                }
            }
        }
    }
}
