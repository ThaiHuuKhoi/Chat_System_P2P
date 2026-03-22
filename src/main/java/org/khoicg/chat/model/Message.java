package org.khoicg.chat.model;

public class Message {
    private String type;
    private String senderId;
    private String content;
    /** Mã tin duy nhất (UUID) cho CHAT/GROUP_CHAT/FILE/RELAY/STORE_OFFLINE — ACK echo lại cùng id (3.6). */
    private String messageId;

    public Message() {}

    public Message(String type, String senderId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.content = content;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
