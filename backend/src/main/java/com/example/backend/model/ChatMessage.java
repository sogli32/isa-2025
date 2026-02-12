package com.example.backend.model;

public class ChatMessage {
    
    public enum MessageType {
        JOIN,
        LEAVE,
        CHAT
    }
    
    private MessageType type;
    private String content;
    private String sender;
    private Long videoId;
    private String timestamp;
    
    public ChatMessage() {
    }
    
    public ChatMessage(MessageType type, String content, String sender, Long videoId) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.videoId = videoId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
    
    // Getters and Setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public Long getVideoId() {
        return videoId;
    }
    
    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
