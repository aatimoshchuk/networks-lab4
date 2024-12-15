package nsu.fit.model.communication.player;

import kotlin.jvm.Synchronized;
import nsu.fit.model.communication.udp.Socket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PlayersStorage {
    private final List<Player> players = new ArrayList<>();

    public List<Player> getPlayers() { return Collections.unmodifiableList(players);}

    public void setPlayers(List<Player> players) {
        this.players.clear();
        this.players.addAll(players);
    }

    public void addPlayer(Player player) { players.add(player);}

    public void deletePlayer(Player player) { players.remove(player);}

    public void deletePlayers(List<Player> playersToDelete) {players.removeAll(playersToDelete);}

    public Optional<Player> findPlayerByID(int playerID) {
        return players.stream()
                .filter(player -> player.getID() == playerID)
                .findAny();
    }

    public Optional<Player> findPlayerBySocket(Socket socket) {
        return players.stream()
                .filter(player -> socket.equals(player.getSocket()))
                .findAny();
    }

    public Optional<Player> findPlayerByRole(Role role) {
        return players.stream()
                .filter(player -> role.equals(player.getRole()))
                .findAny();
    }

    public boolean isItCurrentRole(Role role, int playerID) {
        synchronized (this) {
            Player player = this.findPlayerByID(playerID).orElse(null);

            if (player == null) {
                return false;
            }

            return player.getRole() == role;
        }
    }

    public List<Player> findForLastCommunicationPlayer(long minDiffWithNow, int currID) {
        long instant = Instant.now().toEpochMilli();

        return players.stream()
                .filter(player -> player.getPlayerInfo().getId() != currID)
                .filter(player -> player.getLastCommunicationTime() != -1
                        && instant - player.getLastCommunicationTime() > minDiffWithNow)
                .toList();
    }
}
