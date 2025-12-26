package main.modeles;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "music_favoris")
public class MusicFavoris {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "cote_preference", nullable = false)
    private Integer cotePreference;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "musicfavoris_genres",
            joinColumns = @JoinColumn(name = "music_favoris_id"),
            inverseJoinColumns = @JoinColumn(name = "music_genre_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"music_favoris_id", "music_genre_id"}
            )
    )
    private List<MusicGenre> genres = new ArrayList<>();

    // =====================
    // CONSTRUCTEURS
    // =====================

    protected MusicFavoris() {}

    public MusicFavoris(User user, Integer cotePreference, List<MusicGenre> genres) {
        if (user == null) {
            throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        }
        if (cotePreference == null) {
            throw new IllegalArgumentException("La cote de préférence ne peut pas être null");
        }

        this.user = user;
        this.cotePreference = cotePreference;
        this.genres = (genres != null) ? new ArrayList<>(genres) : new ArrayList<>();
    }

    public MusicFavoris(User user, Integer cotePreference) {
        this(user, cotePreference, new ArrayList<>());
    }

    // =====================
    // GETTERS ET SETTERS
    // =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) {
        if (user == null) throw new IllegalArgumentException("L'utilisateur ne peut pas être null");
        this.user = user;
    }

    public Integer getCotePreference() { return cotePreference; }
    public void setCotePreference(Integer cotePreference) {
        if (cotePreference == null) {
            throw new IllegalArgumentException("La cote de préférence ne peut pas être null");
        }
        this.cotePreference = cotePreference;
    }

    public List<MusicGenre> getGenres() {
        return new ArrayList<>(genres);
    }

    public void setGenres(List<MusicGenre> genres) {
        this.genres = (genres != null) ? new ArrayList<>(genres) : new ArrayList<>();
    }

    // =====================
    // MÉTHODES UTILITAIRES
    // =====================

    public void addGenre(MusicGenre genre) {
        if (genre == null) throw new IllegalArgumentException("Le genre ne peut pas être null");
        if (!genres.contains(genre)) genres.add(genre);
    }

    public void addGenres(List<MusicGenre> genresToAdd) {
        if (genresToAdd == null) throw new IllegalArgumentException("La liste de genres ne peut pas être null");
        for (MusicGenre genre : genresToAdd) addGenre(genre);
    }

    public void removeGenre(MusicGenre genre) {
        if (genre == null) throw new IllegalArgumentException("Le genre à supprimer ne peut pas être null");
        genres.remove(genre);
    }

    public boolean containsGenre(MusicGenre genre) {
        return genre != null && genres.contains(genre);
    }

    public boolean containsGenreById(Long genreId) {
        return genreId != null && genres.stream().anyMatch(g -> genreId.equals(g.getId()));
    }

    public int getNombreGenres() { return genres.size(); }
    public boolean isEmpty() { return genres.isEmpty(); }
    public void clearGenres() { genres.clear(); }

    public boolean isForUser(Long userId) {
        return userId != null && user != null && userId.equals(user.getId());
    }

    // =====================
    // ÉQUALS, HASHCODE, TOSTRING
    // =====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicFavoris that = (MusicFavoris) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }

    @Override
    public String toString() {
        return String.format("MusicFavoris[id=%d, userId=%d, cote=%d, genres=%d]",
                id != null ? id : 0,
                user != null ? user.getId() : 0,
                cotePreference,
                genres.size());
    }
}