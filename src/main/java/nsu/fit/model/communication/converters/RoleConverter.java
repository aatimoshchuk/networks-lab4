package nsu.fit.model.communication.converters;

import nsu.fit.model.communication.player.Role;
import nsu.fit.protobuf.SnakesProto;

public class RoleConverter {
    private final static RoleConverter instance = new RoleConverter();

    private RoleConverter() {
    }

    public static RoleConverter getInstance() {
        return instance;
    }

    public Role snakesProtoToRole(SnakesProto.NodeRole role) {
        return switch (role) {
            case MASTER -> Role.MASTER;
            case NORMAL -> Role.NORMAL;
            case VIEWER -> Role.VIEWER;
            case DEPUTY -> Role.DEPUTY;
        };
    }

    public SnakesProto.NodeRole roleToSnakesProto(Role role) {
        return switch (role) {
            case MASTER -> SnakesProto.NodeRole.MASTER;
            case NORMAL -> SnakesProto.NodeRole.NORMAL;
            case VIEWER -> SnakesProto.NodeRole.VIEWER;
            case DEPUTY -> SnakesProto.NodeRole.DEPUTY;
        };
    }
}
