package nsu.fit.events;

import nsu.fit.model.GameAnnouncement;

import java.util.List;

public record UpdateAvailableGamesEvent(List<GameAnnouncement> availableGames) { }
