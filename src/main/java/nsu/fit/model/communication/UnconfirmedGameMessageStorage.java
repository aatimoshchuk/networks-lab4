package nsu.fit.model.communication;

import nsu.fit.model.communication.udp.Socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnconfirmedGameMessageStorage {
    private final List<Message> unconfirmedMessages = new ArrayList<>();

    public void addMessage(Message message) { unconfirmedMessages.add(message);}

    public void confirmMessage(long msgSeq) {
        unconfirmedMessages.removeIf(message -> message.getMessage().getMsgSeq() == msgSeq);
    }

    public void cancelConfirmation(int playerID) {
        unconfirmedMessages.removeIf(message -> message.getMessage().getReceiverId() == playerID);
    }

    public void replaceDestination(int oldReceiverID, Socket newSenderSocket) {
        unconfirmedMessages.replaceAll(message -> message.getMessage().getReceiverId() == oldReceiverID ?
                new Message(message.getMessage(), newSenderSocket) : message);
    }

    public List<Message> getUnconfirmedMessages() { return Collections.unmodifiableList(unconfirmedMessages);}
}
