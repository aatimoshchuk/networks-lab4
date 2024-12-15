package nsu.fit.model.communication.udp;

import java.net.InetAddress;

public record Socket(InetAddress address, int port) { }
