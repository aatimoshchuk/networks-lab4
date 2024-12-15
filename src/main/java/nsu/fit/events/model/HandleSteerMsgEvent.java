package nsu.fit.events.model;

import nsu.fit.model.communication.udp.Socket;
import nsu.fit.model.snake.Direction;

public record HandleSteerMsgEvent(Direction newDirection, Socket senderSocket, long msgSeq) { }

