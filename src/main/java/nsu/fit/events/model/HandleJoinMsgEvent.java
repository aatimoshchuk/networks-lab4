package nsu.fit.events.model;

import nsu.fit.model.communication.udp.Socket;
import nsu.fit.protobuf.SnakesProto;

public record HandleJoinMsgEvent(SnakesProto.GameMessage.JoinMsg joinMsg, Socket senderSocket, long msgSeq) { }
