package nsu.fit.model.communication.udp;

import nsu.fit.model.communication.Message;

import java.io.IOException;
import java.net.DatagramSocket;

public class UnicastMsgHandler {
    private final DatagramSocket datagramSocket;
    private static UnicastMsgHandler instance;
    private UnicastMsgHandler(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public static UnicastMsgHandler getInstance() throws IOException {
        if (instance == null) {
            instance = new UnicastMsgHandler(new DatagramSocket());
        }

        return instance;
    }

    public void send(Message message) throws IOException {
        UDPSocketManager.getInstance().send(datagramSocket, message);
    }

    public Message receive() throws IOException {
        return  UDPSocketManager.getInstance().receive(datagramSocket);
    }

    public void close() {
        instance = null;
        datagramSocket.close();
    }
}
