package nsu.fit.events.model;

import nsu.fit.model.communication.udp.Socket;

public record HandleDiscoverMsgEvent(Socket senderSocket) { }
