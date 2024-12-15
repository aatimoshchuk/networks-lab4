package nsu.fit.model.communication.udp;

import nsu.fit.model.communication.Message;
import nsu.fit.protobuf.SnakesProto;
import nsu.fit.model.communication.udp.Socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.util.Arrays;

public class UDPSocketManager {
    private static final UDPSocketManager instance = new UDPSocketManager();
    private static final int BUFFER_SIZE = 1460;

    private UDPSocketManager() { }

    public static UDPSocketManager getInstance() {
        return instance;
    }

    public void send(DatagramSocket socket, Message message) throws IOException {
        byte[] buffer = message.getMessage().toByteArray();

        socket.send(new DatagramPacket(
                buffer,
                buffer.length,
                message.getSocket().address(),
                message.getSocket().port()));
    }

    public Message receive(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagramPacket);

        return new Message(
                SnakesProto.GameMessage.parseFrom(Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength())),
                new Socket(datagramPacket.getAddress(), datagramPacket.getPort()), 0);
    }
}
