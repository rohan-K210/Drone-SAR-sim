package com.dronesar.model;

import com.dronesar.model.enums.SignalType;

import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates a network transmission message exchanged between UAVs, Base Towers,
 * and Ground Rescue Teams (OOP: Encapsulation).
 */
public class Message {

    private final String messageId;
    private final String senderId;
    private final String receiverId;
    private final SignalType signalType;
    private final String payload;
    private final long timestamp;
    private final int priority; // 1 (low) to 5 (critical emergency)

    public Message(String messageId, String senderId, String receiverId, SignalType signalType, String payload, int priority) {
        this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "senderId cannot be null");
        this.receiverId = Objects.requireNonNull(receiverId, "receiverId cannot be null");
        this.signalType = Objects.requireNonNull(signalType, "signalType cannot be null");
        this.payload = payload != null ? payload : "";
        this.timestamp = Instant.now().toEpochMilli();
        this.priority = Math.max(1, Math.min(5, priority));
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public String getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s (%s)",
                signalType, senderId, receiverId, payload, messageId);
    }
}
