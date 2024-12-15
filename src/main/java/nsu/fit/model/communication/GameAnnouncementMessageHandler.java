package nsu.fit.model.communication;

import com.google.common.eventbus.EventBus;
import nsu.fit.events.model.HandleDiscoverMsgEvent;
import nsu.fit.events.model.HandleGameAnnouncementMsgEvent;
import nsu.fit.model.communication.converters.GameAnnouncementConverter;
import nsu.fit.model.communication.udp.MulticastMsgHandler;
import nsu.fit.model.communication.udp.Socket;
import nsu.fit.protobuf.SnakesProto;

import java.io.IOException;

public class GameAnnouncementMessageHandler extends Thread {
    private final EventBus eventBus;

    public GameAnnouncementMessageHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        try {
            MulticastMsgHandler multicastMsgHandler = MulticastMsgHandler.getInstance();

            while(!this.isInterrupted()) {
                Message message = multicastMsgHandler.receive();
                Socket senderSocket = message.getSocket();
                SnakesProto.GameMessage gameMessage = message.getMessage();

                if (gameMessage.hasAnnouncement() && !this.isInterrupted()) {
                    gameMessage.getAnnouncement().getGamesList().forEach(game ->
                            eventBus.post(new HandleGameAnnouncementMsgEvent(GameAnnouncementConverter
                                    .getInstance()
                                    .snakesProtoToGameAnnouncement(game, senderSocket))));

                    continue;
                }

                if (gameMessage.hasDiscover() && !this.isInterrupted()) {
                    eventBus.post(new HandleDiscoverMsgEvent(senderSocket));
                }
            }

            multicastMsgHandler.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
