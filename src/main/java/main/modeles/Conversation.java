package main.modeles;


import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<MessagePrive> messages = new ArrayList<>();

    // Constructeurs
    public Conversation() {}

    public Conversation(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }

    // Méthode utile pour récupérer l'autre utilisateur
    public User getOtherUser(Long currentUserId) {
        return user1.getId().equals(currentUserId) ? user2 : user1;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser1() { return user1; }
    public void setUser1(User user1) { this.user1 = user1; }

    public User getUser2() { return user2; }
    public void setUser2(User user2) { this.user2 = user2; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public List<MessagePrive> getMessages() { return messages; }
    public void setMessages(List<MessagePrive> messages) { this.messages = messages; }
}
