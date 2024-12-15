package nsu.fit.model.communication.player;

public class PlayerInfo {
    private final String nickname;
    private final int id;
    private static int idCounter = 0;
    private int score = 0;
    private Role role = Role.NORMAL;

    public PlayerInfo(String nickname) {
        this.nickname = nickname;
        id = ++idCounter;
    }

    public PlayerInfo(String nickname, int id, int score) {
        this.nickname = nickname;
        this.id = id;
        this.score = score;
    }

    public PlayerInfo(String nickname, int id) {
        this.nickname = nickname;
        this.id = id;
    }

    public int getId() { return id;}

    public String getNickname() { return nickname;}

    public int getScore() { return score;}

    public void increaseScore() { score++;}

    public Role getRole() { return role;}

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString(){
        String role = switch (this.role){
            case DEPUTY -> "DEPUTY";
            case MASTER -> "MASTER";
            case NORMAL -> "NORMAL";
            case VIEWER -> "VIEWER";
        };
        if (this.role == Role.VIEWER){
            return nickname + " [" + role + "] ";
        }
        return nickname + " [" + role + "] " + ": " + score + " points";
    }

    @Override
    public boolean equals(Object o){
        if (o == this) {
            return true;
        }
        if (o instanceof PlayerInfo playerInfo) {
            return playerInfo.id == this.id;
        } else {
            return false;
        }
    }
}
