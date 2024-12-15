package nsu.fit.model.communication.udp;

import nsu.fit.model.communication.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastMsgHandler {
    private static final String NETWORK_INTERFACE = "wlan0";
    private final MulticastSocket socket;
    private static MulticastMsgHandler instance;
    private MulticastMsgHandler(MulticastSocket socket) {
        this.socket = socket;
    }

    public static MulticastMsgHandler getInstance() throws IOException {
        if (instance == null) {
            MulticastSocket socket = new MulticastSocket(MulticastConfig.MULTICAST_PORT);
            socket.joinGroup(
                    new InetSocketAddress(MulticastConfig.MULTICAST_ADDRESS, 0),
                    NetworkInterface.getByName(NETWORK_INTERFACE));
            instance = new MulticastMsgHandler(socket);
        }

        return instance;
    }

    public Message receive() throws IOException {
        return UDPSocketManager.getInstance().receive(socket);
    }

    public void close() {
        instance = null;
        socket.close();
    }
}
