package nsu.fit.events.model;

import nsu.fit.model.communication.player.Role;
import nsu.fit.model.communication.udp.Socket;

public record HandleRoleChangeMsgEvent(Role senderRole, Role receiverRole, int senderID, int receiverID,
                                       Socket senderSocket, long msgSeq) { }
