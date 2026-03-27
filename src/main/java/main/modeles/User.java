package main.modeles;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

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
    private String donneeVisible = "110";

    // === NOUVEAUX CHAMPS POUR VALIDATION SMS ===
    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "validation_code", length = 6)
    private String validationCode;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "code_generated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date codeGeneratedAt;

    // === NOUVEAUX CHAMPS POUR LIENS MUSIQUE (nullable = true) ===
    @Column(name = "lien_youtube", nullable = true)
    private String lienYoutube = "";

    @Column(name = "lien_spotify", nullable = true)
    private String lienSpotify = "";

    // === NOUVEAUX CHAMPS DEMANDÉS ===
    @Column(name = "pays", nullable = true)
    private String pays;

    @ElementCollection
    @CollectionTable(name = "user_genres", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "genre")
    private List<String> genreList = new ArrayList<>();

    // =====================
    // CONSTRUCTEURS
    // =====================
    public User() {
    }

    public User(String username, String email, String password, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
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
        if (donneeVisible == null || donneeVisible.isEmpty() || donneeVisible.charAt(0) == '0') {
            return "";
        }
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getValidationCode() {
        return validationCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Date getCodeGeneratedAt() {
        return codeGeneratedAt;
    }

    public String getLienYoutube() {
        return lienYoutube;
    }

    public String getLienSpotify() {
        return lienSpotify;
    }

    // === NOUVEAUX GETTERS ===
    public String getPays() {
        return pays;
    }

    public List<String> getGenreList() {
        return genreList;
    }

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

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
        this.codeGeneratedAt = new Date(); // Mettre à jour la date de génération
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setCodeGeneratedAt(Date codeGeneratedAt) {
        this.codeGeneratedAt = codeGeneratedAt;
    }

    public void setLienYoutube(String lienYoutube) {
        this.lienYoutube = lienYoutube;
    }

    public void setLienSpotify(String lienSpotify) {
        this.lienSpotify = lienSpotify;
    }

    // === NOUVEAUX SETTERS ===
    public void setPays(String pays) {
        this.pays = pays;
    }

    public void setGenreList(List<String> genreList) {
        this.genreList = genreList;
    }

    // === MÉTHODE UTILE POUR AJOUTER UN GENRE ===
    public void addGenre(String genre) {
        if (this.genreList == null) {
            this.genreList = new ArrayList<>();
        }
        this.genreList.add(genre);
    }

    // === MÉTHODE UTILE POUR SUPPRIMER UN GENRE ===
    public void removeGenre(String genre) {
        if (this.genreList != null) {
            this.genreList.remove(genre);
        }
    }

    // =====================
    // MÉTHODES UTILES
    // =====================
    public boolean isAccountValidated() {
        return Boolean.TRUE.equals(isActive);
    }

    public void activateAccount() {
        this.isActive = true;
        this.validationCode = null; // Nettoyer le code après activation
        this.codeGeneratedAt = null;
    }
}