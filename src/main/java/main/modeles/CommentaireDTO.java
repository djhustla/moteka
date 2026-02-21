// CommentaireDTO.java
package main.modeles;

import java.util.Date;

public class CommentaireDTO {
    private Long id;
    private String texte;
    private Date createdAt;
    private Long userId;
    private String username;
    private String userPhotoUrl;
    private Long messageId;

    public CommentaireDTO() {}

    public CommentaireDTO(Commentaire commentaire) {
        this.id = commentaire.getId();
        this.texte = commentaire.getTexte();
        this.createdAt = commentaire.getCreatedAt();
        this.userId = commentaire.getUser().getId();
        this.username = commentaire.getUser().getUsername();
        this.userPhotoUrl = commentaire.getUser().getPhotoUrl();
        this.messageId = commentaire.getMessage().getId();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
}