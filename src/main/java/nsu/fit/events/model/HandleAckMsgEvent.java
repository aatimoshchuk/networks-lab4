package nsu.fit.events.model;

public record HandleAckMsgEvent(int senderID, long msgSeq) { }
