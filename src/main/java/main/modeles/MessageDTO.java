package main.modeles;

import java.util.Date;
import java.util.List;

public class MessageDTO {
    private Long id;
    private String intituleMessage;
    private Date heureMessage;
    private String username;
    private String photoUrl;
    private int nombreCommentaires; // ✅ NOUVEAU : Nombre de commentaires

    public MessageDTO() {}

    // Constructeur qui convertit un Message en DTO
    public MessageDTO(Message message) {
        this.id = message.getId();
        this.intituleMessage = message.getIntituleMessage();
        this.heureMessage = message.getHeureMessage();
        this.username = message.getUser().getUsername();
        this.photoUrl = message.getUser().getPhotoUrl();
        this.nombreCommentaires = message.getNombreCommentaires(); // ✅ NOUVEAU
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntituleMessage() { return intituleMessage; }
    public void setIntituleMessage(String intituleMessage) { this.intituleMessage = intituleMessage; }

    public Date getHeureMessage() { return heureMessage; }
    public void setHeureMessage(Date heureMessage) { this.heureMessage = heureMessage; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    // ✅ NOUVEAU : Getter et Setter pour le nombre de commentaires
    public int getNombreCommentaires() { return nombreCommentaires; }
    public void setNombreCommentaires(int nombreCommentaires) {
        this.nombreCommentaires = nombreCommentaires;
    }

    // ✅ NOUVEAU : Méthode utilitaire pour créer un DTO avec des détails supplémentaires
    public static MessageDTO avecDetails(Message message, List<CommentaireDTO> commentaires) {
        MessageDTO dto = new MessageDTO(message);
        // Si on veut inclure les commentaires détaillés plus tard
        return dto;
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "id=" + id +
                ", intituleMessage='" + intituleMessage + '\'' +
                ", heureMessage=" + heureMessage +
                ", username='" + username + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", nombreCommentaires=" + nombreCommentaires +
                '}';
    }
}