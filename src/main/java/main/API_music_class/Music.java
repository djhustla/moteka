package main.API_music_class;

import jakarta.persistence.*;
import main.modeles.User;

@Entity
@Table(name = "music", uniqueConstraints = {
        @UniqueConstraint(columnNames = "titre"),
        @UniqueConstraint(columnNames = "lien_spotify")
})
public class Music {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = true, unique = true)
    private String titre;

    @Column(name = "lien_spotify", nullable = true, unique = true)
    private String lienSpotify;

    @Column(name = "lien_youtube", nullable = true)
    private String lienYoutube;

    @Column(name = "url_image_spotify", nullable = true)
    private String urlImageSpotify;

    @Column(name = "popularite", nullable = true)
    private Integer popularite;

    @Column(name = "genre_musical", nullable = true)
    private String genreMusical;

    @Column(name = "note", nullable = true)
    private Integer note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // =====================
    // CONSTRUCTEURS
    // =====================
    public Music() {}

    public Music(String titre, String lienSpotify, String lienYoutube, String urlImageSpotify, User user) {
        this.titre             = titre;
        this.lienSpotify       = lienSpotify;
        this.lienYoutube       = lienYoutube;
        this.urlImageSpotify   = urlImageSpotify;
        this.user              = user;
        // popularite garde sa valeur par défaut (0)
        // genreMusical garde sa valeur par défaut (null)
        // note garde sa valeur par défaut (0)
    }

    public Music(String titre, String lienSpotify, String lienYoutube, String urlImageSpotify, int popularite, User user) {
        this.titre             = titre;
        this.lienSpotify       = lienSpotify;
        this.lienYoutube       = lienYoutube;
        this.urlImageSpotify   = urlImageSpotify;
        this.popularite        = popularite;
        this.user              = user;
        // genreMusical garde sa valeur par défaut (null)
        // note garde sa valeur par défaut (0)
    }

    public Music(String titre, String lienSpotify, String lienYoutube, String urlImageSpotify, int popularite, String genreMusical, int note, User user) {
        this.titre             = titre;
        this.lienSpotify       = lienSpotify;
        this.lienYoutube       = lienYoutube;
        this.urlImageSpotify   = urlImageSpotify;
        this.popularite        = popularite;
        this.genreMusical      = genreMusical;
        this.note              = note;
        this.user              = user;
    }

    public Music(String titre, String lienSpotify, String lienYoutube, String urlImageSpotify, String genreMusical, int note, User user) {
        this.titre             = titre;
        this.lienSpotify       = lienSpotify;
        this.lienYoutube       = lienYoutube;
        this.urlImageSpotify   = urlImageSpotify;
        this.genreMusical      = genreMusical;
        this.note              = note;
        this.user              = user;
        // popularite garde sa valeur par défaut (0)
    }

    // =====================
    // GETTERS
    // =====================
    public Long getId() { return id; }

    public String getTitre() { return titre; }

    public String getLienSpotify() { return lienSpotify; }

    public String getLienYoutube() { return lienYoutube; }

    public String getUrlImageSpotify() { return urlImageSpotify; }

    public int getPopularite() { return popularite; }

    public String getGenreMusical() { return genreMusical; }

    public int getNote() { return note; }

    public User getUser() { return user; }

    // =====================
    // SETTERS
    // =====================
    public void setId(Long id) { this.id = id; }

    public void setTitre(String titre) { this.titre = titre; }

    public void setLienSpotify(String lienSpotify) { this.lienSpotify = lienSpotify; }

    public void setLienYoutube(String lienYoutube) { this.lienYoutube = lienYoutube; }

    public void setUrlImageSpotify(String urlImageSpotify) { this.urlImageSpotify = urlImageSpotify; }

    public void setPopularite(int popularite) { this.popularite = popularite; }

    public void setGenreMusical(String genreMusical) { this.genreMusical = genreMusical; }

    public void setNote(int note) { this.note = note; }

    public void setUser(User user) { this.user = user; }

    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", lienSpotify='" + lienSpotify + '\'' +
                ", lienYoutube='" + lienYoutube + '\'' +
                ", urlImageSpotify='" + urlImageSpotify + '\'' +
                ", popularite=" + popularite +
                ", genreMusical='" + genreMusical + '\'' +
                ", note=" + note +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }
}