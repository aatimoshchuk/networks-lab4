package nsu.fit.events.model;

import nsu.fit.model.GameState;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.udp.Socket;

import java.util.List;

public record HandleGameStateMsgEvent(GameState newGameState, List<Player> players, Socket senderSocket, long msgSeq) {}
