package nsu.fit.model.communication.converters;

import nsu.fit.model.GameConfig;
import nsu.fit.protobuf.SnakesProto;

public final class GameConfigConverter {
    private static final GameConfigConverter instance = new GameConfigConverter();

    private GameConfigConverter() { }

    public static GameConfigConverter getInstance() {
        return instance;
    }

    public SnakesProto.GameConfig gameConfigToSnakesProto(GameConfig gameConfig) {
        return SnakesProto.GameConfig.newBuilder()
                .setStateDelayMs(gameConfig.delay())
                .setFoodStatic(gameConfig.foodStatic())
                .setWidth(gameConfig.width())
                .setHeight(gameConfig.height())
                .build();
    }

    public GameConfig snakesProtoToGameConfig(SnakesProto.GameConfig gameConfig) {
        return new GameConfig(
                gameConfig.getWidth(),
                gameConfig.getHeight(),
                gameConfig.getFoodStatic(),
                gameConfig.getStateDelayMs());
    }
}
