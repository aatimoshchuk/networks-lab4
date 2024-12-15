package nsu.fit.events;

import nsu.fit.model.GameState;
import nsu.fit.model.communication.player.PlayerInfo;

public record NewGameEvent(GameState gameState, String gameName, PlayerInfo playerInfo) {
}
