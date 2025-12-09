package main.modeles;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER"; // rôle par défaut

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date(); // date de création automatique

    // ✅ COLONNE pour stocker l'adresse/URL de la photo
    @Column(name = "photo_url")
    private String photoUrl; // ex: "/uploads/users/lucie.png"

    // ✅ NOUVELLE COLONNE pour stocker l'URL du son d'entrée
    @Column(name = "son_entree_url", nullable = true)
    private String sonEntreeURL; // ex: "/uploads/sons/user123_son.wav"

    // ✅ NOUVELLE COLONNE pour stocker les reseaux
    @Column(name = "liste_reseaux", nullable = true)
    private String listeReseaux;

    // ✅ NOUVELLE COLONNE pour stocker la donnée visible
    @Column(name = "donnee_visible", nullable = true)
    private String donneeVisible;

    // ✅ RELATION avec MusicFavoris
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MusicFavoris> musicFavoris = new ArrayList<>();

    // =====================
    // Getters et setters
    // =====================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getPhotoUrl()
    {
        return photoUrl;
    }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getSonEntreeURL() { return sonEntreeURL; }
    public void setSonEntreeURL(String sonEntreeURL) { this.sonEntreeURL = sonEntreeURL; }

    public String getListeReseaux() { return listeReseaux; }
    public void setListeReseaux(String listeReseaux) { this.listeReseaux = listeReseaux; }

    public String getDonneeVisible() { return donneeVisible; }
    public void setDonneeVisible(String donneeVisible) { this.donneeVisible = donneeVisible; }

    public List<MusicFavoris> getMusicFavoris() { return musicFavoris; }
    public void setMusicFavoris(List<MusicFavoris> musicFavoris) { this.musicFavoris = musicFavoris; }

    // Méthodes utilitaires pour gérer les favoris
    public void addMusicFavoris(MusicFavoris favoris) {
        musicFavoris.add(favoris);
        favoris.setUser(this);
    }

    public void removeMusicFavoris(MusicFavoris favoris) {
        musicFavoris.remove(favoris);
        favoris.setUser(null);
    }

    // Constructeurs
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}