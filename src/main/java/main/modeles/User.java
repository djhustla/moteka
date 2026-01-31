package main.modeles;

import jakarta.persistence.*;
import java.util.Date;

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
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "validation_code", length = 6)
    private String validationCode;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "code_generated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date codeGeneratedAt;

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