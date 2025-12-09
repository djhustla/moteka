package main.modeles;

import jakarta.persistence.*;

@Entity
@Table(name = "music_favoris")
public class MusicFavoris {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "music_genre_id", nullable = false)
    private MusicGenre musicGenre;

    @Column(name = "cote_preference", nullable = false)
    private Integer cotePreference;

    // Constructeurs
    public MusicFavoris() {
    }

    public MusicFavoris(User user, MusicGenre musicGenre, Integer cotePreference) {
        this.user = user;
        this.musicGenre = musicGenre;
        this.cotePreference = cotePreference;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MusicGenre getMusicGenre() {
        return musicGenre;
    }

    public void setMusicGenre(MusicGenre musicGenre) {
        this.musicGenre = musicGenre;
    }

    public Integer getCotePreference() {
        return cotePreference;
    }

    public void setCotePreference(Integer cotePreference) {
        this.cotePreference = cotePreference;
    }

    @Override
    public String toString() {
        return "MusicFavoris{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : "null") +
                ", musicGenre=" + (musicGenre != null ? musicGenre.getDescription() : "null") +
                ", cotePreference=" + cotePreference +
                '}';
    }
}