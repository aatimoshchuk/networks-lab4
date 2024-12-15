package nsu.fit.model.communication.player;

import nsu.fit.model.communication.udp.Socket;

public class Player {
    private final PlayerInfo playerInfo;
    private final InetInfo inetInfo;

    public Player(PlayerInfo playerInfo, InetInfo inetInfo, Role role) {
        this.playerInfo = playerInfo;
        playerInfo.setRole(role);
        this.inetInfo = inetInfo;
    }

    public int getID() { return playerInfo.getId();}

    public Socket getSocket() { return inetInfo.getSocket();}

    public long getLastCommunicationTime() { return inetInfo.getLastCommunicationTime();}

    public void updateLastCommunicationTime() { inetInfo.updateLastCommunicationTime();}

    public PlayerInfo getPlayerInfo() { return playerInfo;}

    public Role getRole() { return playerInfo.getRole();}

    public void setRole(Role role) { playerInfo.setRole(role);}
}
