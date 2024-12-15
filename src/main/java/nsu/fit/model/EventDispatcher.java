package nsu.fit.model;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.util.Pair;
import nsu.fit.controllers.util.Informant;
import nsu.fit.events.RenderGameFieldEvent;
import nsu.fit.events.StartNewGameAnimationEvent;
import nsu.fit.events.UpdateAvailableGamesEvent;
import nsu.fit.events.UpdateGameScoresEvent;
import nsu.fit.events.model.*;
import nsu.fit.events.switching.SwitchToGameFieldEvent;
import nsu.fit.exceptions.GameException;
import nsu.fit.model.communication.*;
import nsu.fit.model.communication.player.InetInfo;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.player.PlayerInfo;
import nsu.fit.model.communication.player.Role;
import nsu.fit.model.communication.udp.MulticastConfig;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.model.communication.udp.UnicastMsgHandler;
import nsu.fit.model.snake.Direction;
import nsu.fit.protobuf.SnakesProto;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EventDispatcher {
    private static final long GAME_ANNOUNCEMENT_TTL = 2000;
    private final EventBus modelEventBus = new EventBus();
    private final UnicastMsgHandler unicastMsgHandler;
    private final InetAddress multicastAddress;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<GameAnnouncement, Long> gamesRepository = new HashMap<>();
    private GameAnnouncementMessageHandler gameAnnouncementMessageHandler;
    private GameState gameState;
    private PlayerInfo currPlayerInfo;
    private String gameName;
    private CommunicationManager communicationManager;
    private GameMessageHandler gameMessageHandler;
    private EventBus controllersEventBus;
    public EventDispatcher() throws IOException {
        gameAnnouncementMessageHandler = new GameAnnouncementMessageHandler(modelEventBus);
        multicastAddress = InetAddress.getByName(MulticastConfig.MULTICAST_ADDRESS);
        unicastMsgHandler = UnicastMsgHandler.getInstance();
    }

    public void register() { modelEventBus.register(this);}

    public void startHandleAnnouncementMessages() { gameAnnouncementMessageHandler.start();}

    public GameState getGameState() { return gameState;}

    public void changeGameState() { communicationManager.nextGameState(currPlayerInfo, gameState);}

    public PlayerInfo getCurrPlayerInfo() { return currPlayerInfo;}

    public List<PlayerInfo> getCurrentPlayers() {
        return gameState
                .getPlayers()
                .stream()
                .sorted(Comparator.comparing(PlayerInfo::getScore).reversed().thenComparing(PlayerInfo::getNickname))
                .toList();
    }

    public void setControllersEventBus(EventBus controllersEventBus) {
        this.controllersEventBus = controllersEventBus;
    }

    public void createNewGame(GameState gameState, PlayerInfo playerInfo, String gameName) throws IOException, GameException {
        this.gameState = gameState;
        this.currPlayerInfo = playerInfo;
        this.gameName = gameName;

        if (!addFirstPlayer(gameState, playerInfo)) {
            throw GameException.startingGameError();
        }

        setNewGameCommunicationManager(gameState, playerInfo);
        startGameMessageHandler(gameState);

        scheduler.scheduleAtFixedRate(this::multicastAnnounceGame, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(
                this::sendPingMessagesToPlayers,
                0,
                communicationManager.getPingInterval() / 2,
                TimeUnit.MILLISECONDS);

        startPlayersExpiredDetectors(communicationManager);
    }

    public void joinGame(GameAnnouncement gameAnnouncement, String nickname, boolean isViewer)
            throws IOException, InterruptedException {
        gameAnnouncementMessageHandler.interrupt();

        Role role = isViewer ? Role.VIEWER : Role.NORMAL;

        this.gameName = gameAnnouncement.gameName();
        GameConfig gameConfig = gameAnnouncement.gameConfig();
        this.gameState = new GameState(gameConfig);
        this.communicationManager = CommunicationManager.createManager(gameConfig.delay(), modelEventBus);

        if (startJoining(gameAnnouncement, nickname, role)) {
            startGameMessageHandler(gameState);
            startPlayersExpiredDetectors(communicationManager);
        }
    }

    public void removeExpiredGames() {
        synchronized (gamesRepository) {
            long now = Instant.now().toEpochMilli();

            Set<Map.Entry<GameAnnouncement, Long>> gamesToDelete = gamesRepository
                    .entrySet()
                    .stream()
                    .filter(entry -> now - entry.getValue() > GAME_ANNOUNCEMENT_TTL)
                    .collect(Collectors.toSet());

            gamesToDelete.forEach(entry -> gamesRepository.remove(entry.getKey()));

            if (!gamesToDelete.isEmpty()) {
                controllersEventBus.post(new UpdateAvailableGamesEvent(gamesRepository.keySet().stream().toList()));
            }
        }
    }

    public void end() {
        stopScheduler();
        communicationManager.close(currPlayerInfo);
        gameMessageHandler.interrupt();
        gameAnnouncementMessageHandler.interrupt();
        modelEventBus.unregister(this);
    }

    public void stopScheduler() {
        scheduler.shutdownNow();
        scheduler.close();
    }

    public void steerSnake(Direction newDirection) {
        communicationManager.steer(newDirection, currPlayerInfo, gameState);
    }

    public void multicastAnnounceGame() {
        communicationManager.multicastGameAnnounce(gameState, currPlayerInfo, gameName);
    }

    private void sendPingMessagesToPlayers() {
        communicationManager.sendPingMessagesToPlayers(currPlayerInfo);
    }

    public void sendDiscoverMsg() throws IOException {
        SnakesProto.GameMessage gameMessage = GameMessageCreator.getInstance().createDiscoverMsg(0);
        unicastMsgHandler.send(new Message(gameMessage, new Socket(multicastAddress, MulticastConfig.MULTICAST_PORT)));
    }

    @Subscribe
    public void handleJoinMsg(HandleJoinMsgEvent e) {
        communicationManager.handleJoinMessage(e.joinMsg(), e.senderSocket(), e.msgSeq(), gameState, currPlayerInfo);
    }

    @Subscribe
    public void handleDiscoverMsg(HandleDiscoverMsgEvent e) {
        if (communicationManager != null) {
            communicationManager.unicastGameAnnounce(e.senderSocket(), currPlayerInfo, gameState, gameName);
        }
    }

    @Subscribe
    public void handleGameStateMsg(HandleGameStateMsgEvent e) {
        if (communicationManager.handleGameStateMessage(gameState, e.newGameState(), e.players(), e.senderSocket(),
                currPlayerInfo, e.msgSeq())) {
            gameState = e.newGameState();
            controllersEventBus.post(new RenderGameFieldEvent(gameState));
            controllersEventBus.post(new UpdateGameScoresEvent());
        }
    }

    @Subscribe
    public void handleAckMsg(HandleAckMsgEvent e) {
        communicationManager.handleAckMsg(e.senderID(), e.msgSeq());
    }

    @Subscribe
    public void handlePingMsg(HandlePingMsgEvent e) {
        communicationManager.handlePingMsg(e.senderSocket(), currPlayerInfo, e.msgSeq());
    }

    @Subscribe
    public void handleSteerMsg(HandleSteerMsgEvent e) {
        communicationManager.handleSteerMsg(gameState, e.newDirection(), e.senderSocket(), currPlayerInfo, e.msgSeq());
    }

    @Subscribe
    void handleRoleChangeMsg(HandleRoleChangeMsgEvent e) {
        communicationManager.handleRoleChangeMsg(e.senderRole(), e.receiverRole(), e.senderID(), e.receiverID(),
                e.msgSeq(), currPlayerInfo, e.senderSocket());
    }

    @Subscribe
    public void handleGameAnnouncement(HandleGameAnnouncementMsgEvent e) {
        synchronized (gamesRepository) {
            gamesRepository.put(e.gameAnnouncement(), Instant.now().toEpochMilli());
            if (controllersEventBus != null) {
                controllersEventBus.post(new UpdateAvailableGamesEvent(gamesRepository.keySet().stream().toList()));
            }
        }
    }

    @Subscribe
    public void handleErrorMsg(HandleErrorMsgEvent e) {
        communicationManager.handleErrorMsg(e.senderSocket(), currPlayerInfo, e.msgSeq());
    }

    @Subscribe
    public void startMasterRoutine(StartMasterRoutineEvent e) {
        gameAnnouncementMessageHandler = new GameAnnouncementMessageHandler(modelEventBus);
        gameAnnouncementMessageHandler.start();

        controllersEventBus.post(new StartNewGameAnimationEvent(gameState.getGameConfig().delay()));

        scheduler.scheduleAtFixedRate(
                this::sendPingMessagesToPlayers,
                0,
                communicationManager.getPingInterval() / 2,
                TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::multicastAnnounceGame, 1, 1, TimeUnit.SECONDS);
    }

    private void resendUnconfirmedMessages() {
        communicationManager.resendUnconfirmed();
    }

    private void findExpired() {
        communicationManager.findExpired(currPlayerInfo, gameState);
    }

    private boolean startJoining(GameAnnouncement gameAnnouncement, String nickname, Role role) throws IOException {
        communicationManager.sendJoinMsg(gameAnnouncement, nickname, role);
        int id = communicationManager.receiveAskMsgAfterJoinMsg(role);

        if (id == -1) {
            Informant.error("Can't join this game");
            return false;
        }

        this.currPlayerInfo = new PlayerInfo(nickname, id);
        controllersEventBus.post(new SwitchToGameFieldEvent());

        return true;
    }

    private boolean addFirstPlayer(GameState gameState, PlayerInfo playerInfo) {
        gameState.generateNewFood();
        Pair<Coordinate, Direction> newSnakeHead = gameState.findFreeSquare();

        if (newSnakeHead != null) {
            gameState.addNewPlayer(playerInfo, newSnakeHead.getKey(), newSnakeHead.getValue());
        } else {
            return false;
        }

        return true;
    }

    private void setNewGameCommunicationManager(GameState gameState, PlayerInfo playerInfo) throws IOException {
        CommunicationManager communicationManager =
                CommunicationManager.createManager(gameState.getGameConfig().delay(), modelEventBus);

        Player master = new Player(
                playerInfo,
                new InetInfo(null, 0, 0),
                Role.MASTER);

        communicationManager.addMaster(master);
        this.communicationManager = communicationManager;
    }

    private void startGameMessageHandler(GameState gameState) {
        this.gameMessageHandler = new GameMessageHandler(modelEventBus, gameState.getGameConfig());
        this.gameMessageHandler.start();
    }

    private void startPlayersExpiredDetectors(CommunicationManager communicationManager) {
        scheduler.scheduleAtFixedRate(
                this::resendUnconfirmedMessages,
                0,
                communicationManager.getPingInterval() / 2,
                TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(
                this::findExpired,
                0,
                communicationManager.getDelay() / 2,
                TimeUnit.MILLISECONDS);
    }
}
