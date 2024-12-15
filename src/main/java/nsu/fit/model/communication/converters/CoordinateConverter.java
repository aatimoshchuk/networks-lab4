package nsu.fit.model.communication.converters;

import nsu.fit.model.Coordinate;
import nsu.fit.protobuf.SnakesProto;

public class CoordinateConverter {
    private final static CoordinateConverter instance = new CoordinateConverter();

    private CoordinateConverter() {
    }

    static CoordinateConverter getInstance() {
        return instance;
    }

    Coordinate snakesProtoToCoordinate(SnakesProto.GameState.Coord coord) {
        return new Coordinate(coord.getX(), coord.getY());
    }

    SnakesProto.GameState.Coord coordinateToSnakeProto(Coordinate coordinate) {
        return SnakesProto.GameState.Coord.newBuilder().setX(coordinate.x())
                .setY(coordinate.y())
                .build();
    }
}
