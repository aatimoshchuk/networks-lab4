package nsu.fit.events;

import nsu.fit.model.GameState;

public record RenderGameFieldEvent(GameState gameState) { }
