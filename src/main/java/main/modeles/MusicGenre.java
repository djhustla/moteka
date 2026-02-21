package main.modeles;


import jakarta.persistence.*;

@Entity
@Table(name = "music_genre")
public class MusicGenre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    // Constructeurs
    public MusicGenre() {
    }

    public MusicGenre(String description) {
        this.description = description;
    }

    public MusicGenre(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "MusicGenre{" +
                "id=" + id +
                ", description='" + description + '\'' +
                '}';
    }
}