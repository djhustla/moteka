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
    private String role = "USER";

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "son_entree_url")
    private String sonEntreeURL;

    @Column(name = "liste_reseaux")
    private String listeReseaux;

    @Column(name = "donnee_visible")
    private String donneeVisible="110";

    /*
    // Relation OneToMany avec MusicFavoris
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MusicFavoris> musicFavoris = new ArrayList<>();

     */

    // =====================
    // CONSTRUCTEURS
    // =====================
    public User() {
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // =====================
    // GETTERS
    // =====================
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getPhotoUrl() {
        if (donneeVisible.charAt(0)==0 ) return "";
        /*
           if (donneeVisible == null || donneeVisible.isEmpty() || donneeVisible.charAt(0) == '0') {
            return "";
        }
        * */
        return photoUrl;
    }

    public String getSonEntreeURL() {
        return sonEntreeURL;
    }

    public String getListeReseaux() {
        return listeReseaux;
    }

    public String getDonneeVisible() {
        return donneeVisible;
    }

    /*
    public List<MusicFavoris> getMusicFavoris() {
        return musicFavoris;
    }

     */

    // =====================
    // SETTERS
    // =====================
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setSonEntreeURL(String sonEntreeURL) {
        this.sonEntreeURL = sonEntreeURL;
    }

    public void setListeReseaux(String listeReseaux) {
        this.listeReseaux = listeReseaux;
    }

    public void setDonneeVisible(String donneeVisible) {
        this.donneeVisible = donneeVisible;
    }

    /*
    public void setMusicFavoris(List<MusicFavoris> musicFavoris) {
        this.musicFavoris = musicFavoris;
    }

     */

    // =====================
    // MÉTHODES POUR MUSICFAVORIS
    // =====================

    /**
     * Ajoute un MusicFavoris à l'utilisateur
     */
    /*
    public void addMusicFavoris(MusicFavoris favoris) {
        if (favoris != null && !musicFavoris.contains(favoris)) {
            musicFavoris.add(favoris);
            favoris.setUser(this);
        }
    }

     */

    /**
     * Supprime un MusicFavoris de l'utilisateur
     */
    /*
    public void removeMusicFavoris(MusicFavoris favoris) {
        if (favoris != null) {
            musicFavoris.remove(favoris);
            favoris.setUser(null);
        }
    }

     */

    /*
    // =====================
    // TO STRING
    // =====================
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", musicFavorisCount=" + musicFavoris.size() +
                '}';
    }

     */
}