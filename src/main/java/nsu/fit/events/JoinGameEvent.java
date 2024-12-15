package nsu.fit.events;

import nsu.fit.model.EventDispatcher;
import nsu.fit.model.GameAnnouncement;

public record JoinGameEvent(EventDispatcher eventDispatcher, GameAnnouncement gameAnnouncement, String nickname,
                            boolean isViewer) { }
