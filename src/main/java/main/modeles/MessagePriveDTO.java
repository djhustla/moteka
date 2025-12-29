package main.modeles;

import java.util.Date;

public class MessagePriveDTO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderPhoto;
    private String content;
    private Date sentAt;
    private boolean isRead;

    public MessagePriveDTO() {}

    public MessagePriveDTO(MessagePrive message) {
        this.id = message.getId();
        this.senderId = message.getSender().getId();
        this.senderUsername = message.getSender().getUsername();
        this.senderPhoto = message.getSender().getPhotoUrl();
        this.content = message.getContent();
        this.sentAt = message.getSentAt();
        this.isRead = message.isRead();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderPhoto() { return senderPhoto; }
    public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}