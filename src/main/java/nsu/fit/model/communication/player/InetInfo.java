package nsu.fit.model.communication.player;

import nsu.fit.model.communication.udp.Socket;

import java.net.InetAddress;
import java.time.Instant;

public class InetInfo {
    private final Socket socket;
    private long lastCommunicationTime;

    public InetInfo(InetAddress inetAddress, int port, long lastCommunicationTime) {
        this.socket = new Socket(inetAddress, port);
        this.lastCommunicationTime = lastCommunicationTime;
    }

    public InetInfo(Socket socket, long lastCommunicationTime) {
        this.socket = socket;
        this.lastCommunicationTime = lastCommunicationTime;
    }

    public long getLastCommunicationTime() { return lastCommunicationTime;}

    public Socket getSocket() { return socket;}

    public void updateLastCommunicationTime() {
        lastCommunicationTime = Instant.now().toEpochMilli();
    }
}
