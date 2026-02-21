package main.modeles;

import jakarta.persistence.*;

@Entity
@Table(name = "mes_moderateurs")
public class MesModerateurs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // Constructeurs
    public MesModerateurs() {}

    public MesModerateurs(String username) {
        this.username = username;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}