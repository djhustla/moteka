package main.modeles;

import java.util.Date;

public class ConversationDTO {
    private Long id;
    private Long otherUserId;
    private String otherUsername;
    private String otherUserPhoto;
    private Date lastMessageAt;
    private long unreadCount;
    private String lastMessagePreview;

    public ConversationDTO() {}

    public ConversationDTO(Conversation conv, Long currentUserId, long unreadCount) {
        this.id = conv.getId();
        User otherUser = conv.getOtherUser(currentUserId);
        this.otherUserId = otherUser.getId();
        this.otherUsername = otherUser.getUsername();
        this.otherUserPhoto = otherUser.getPhotoUrl();
        this.lastMessageAt = conv.getLastMessageAt();
        this.unreadCount = unreadCount;

        // Dernier message (si existe)
        if (!conv.getMessages().isEmpty()) {
            MessagePrive last = conv.getMessages().get(conv.getMessages().size() - 1);
            this.lastMessagePreview = last.getContent().length() > 50
                    ? last.getContent().substring(0, 50) + "..."
                    : last.getContent();
        }
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }

    public String getOtherUserPhoto() { return otherUserPhoto; }
    public void setOtherUserPhoto(String otherUserPhoto) { this.otherUserPhoto = otherUserPhoto; }

    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
}