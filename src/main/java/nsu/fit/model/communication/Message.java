package nsu.fit.model.communication;

import nsu.fit.model.communication.udp.Socket;
import nsu.fit.protobuf.SnakesProto;



public class Message {
    private final SnakesProto.GameMessage message;
    private final Socket socket;
    private  long sentAt;

    public Message(SnakesProto.GameMessage message, Socket socket) {
        this.message = message;
        this.socket = socket;
        sentAt = -1;
    }

    public Message(SnakesProto.GameMessage message, Socket socket, long sentAt) {
        this.message = message;
        this.socket = socket;
        this.sentAt = sentAt;
    }

    public SnakesProto.GameMessage getMessage() {
        return message;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSentAt(long sentAt) {
        this.sentAt = sentAt;
    }

    public long getSentAt() { return sentAt;}
}
