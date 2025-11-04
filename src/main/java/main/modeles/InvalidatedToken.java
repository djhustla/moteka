package main.modeles;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime invalidatedAt; // Date à laquelle on a fait logout

    public InvalidatedToken() {}

    public InvalidatedToken(String token) {
        this.token = token;
        this.invalidatedAt = LocalDateTime.now();
    }

    // getters et setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(LocalDateTime invalidatedAt) { this.invalidatedAt = invalidatedAt; }
}
