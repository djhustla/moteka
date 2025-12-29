package main.modeles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intituleMessage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date heureMessage = new Date(); // heure automatique

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // ✅ NOUVEAU : Relation avec les commentaires
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Commentaire> commentaires = new ArrayList<>();

    // =====================
    // Constructeurs
    // =====================
    public Message() {}

    public Message(String intituleMessage, User user) {
        this.intituleMessage = intituleMessage;
        this.user = user;
    }

    // =====================
    // Getters / Setters
    // =====================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntituleMessage() { return intituleMessage; }
    public void setIntituleMessage(String intituleMessage) { this.intituleMessage = intituleMessage; }

    public Date getHeureMessage() { return heureMessage; }
    public void setHeureMessage(Date heureMessage) { this.heureMessage = heureMessage; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // ✅ NOUVEAU : Getters et Setters pour les commentaires
    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires;
    }

    // ✅ NOUVEAU : Méthode utilitaire pour ajouter un commentaire
    public void ajouterCommentaire(Commentaire commentaire) {
        if (this.commentaires == null) {
            this.commentaires = new ArrayList<>();
        }
        commentaire.setMessage(this);
        this.commentaires.add(commentaire);
    }

    // ✅ NOUVEAU : Méthode utilitaire pour compter les commentaires
    public int getNombreCommentaires() {
        return this.commentaires != null ? this.commentaires.size() : 0;
    }
}