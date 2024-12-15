package nsu.fit.model.communication.converters;

import nsu.fit.model.communication.player.InetInfo;
import nsu.fit.model.communication.player.Player;
import nsu.fit.model.communication.player.PlayerInfo;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.protobuf.SnakesProto;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

public class PlayersConverter {
    private final static PlayersConverter instance = new PlayersConverter();

    private PlayersConverter() {
    }

    public static PlayersConverter getInstance() {
        return instance;
    }

    public SnakesProto.GamePlayers playersToSnakesProto(List<Player> gamePlayers) {
        SnakesProto.GamePlayers.Builder builder = SnakesProto.GamePlayers.newBuilder();
        return builder.addAllPlayers(gamePlayers.stream().map(this::playerToSnakesProto).toList())
                .build();
    }

    public List<Player> snakesProtoToPlayers(SnakesProto.GamePlayers gamePlayers) {
        return gamePlayers.getPlayersList().stream().map(this::snakesProtoToPlayer).toList();
    }

    private Player snakesProtoToPlayer(SnakesProto.GamePlayer player) {
        PlayerInfo playerInfo = new PlayerInfo(player.getName(), player.getId(), player.getScore());
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(player.getIpAddress());
        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
        InetInfo inetInfo = new InetInfo(inetAddress, player.getPort(), Instant.now().toEpochMilli());
        return new Player(
                playerInfo,
                inetInfo,
                RoleConverter.getInstance().snakesProtoToRole(player.getRole()));
    }

    private SnakesProto.GamePlayer playerToSnakesProto(Player player) {
        SnakesProto.GamePlayer.Builder builder = SnakesProto.GamePlayer.newBuilder();
        builder.setName(player.getPlayerInfo().getNickname())
                .setId(player.getPlayerInfo().getId())
                .setPort(player.getSocket().port())
                .setRole(RoleConverter.getInstance().roleToSnakesProto(player.getRole()))
                .setScore(player.getPlayerInfo().getScore());

        if (player.getSocket().address() != null) {
            builder.setIpAddress(player.getSocket().address().getHostAddress());
        }
        return builder.build();
    }
}
