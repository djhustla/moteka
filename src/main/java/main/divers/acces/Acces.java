package main.divers.acces;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "acces")
public class Acces {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_acces", nullable = false)
    private String nomAcces;

    @Column(name = "ligne_voie_bk", nullable = false)
    private String ligneBkVoie;

    @Column(name = "adresse", nullable = true)
    private String adresse;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeurs
    public Acces() {
    }

    public Acces(String nomAcces, String ligneBkVoie, String adresse, String description,
                 Double latitude, Double longitude) {
        this.nomAcces = nomAcces;
        this.ligneBkVoie = ligneBkVoie;
        this.adresse = adresse;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomAcces() {
        return nomAcces;
    }

    public void setNomAcces(String nomAcces) {
        this.nomAcces = nomAcces;
    }

    public String getLigneBkVoie() {
        return ligneBkVoie;
    }

    public void setLigneBkVoie(String ligneBkVoie) {
        this.ligneBkVoie = ligneBkVoie;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Méthode pour obtenir les coordonnées formatées
    public String getCoordonneesFormatees() {
        if (latitude != null && longitude != null) {
            return String.format("%.6f, %.6f", latitude, longitude);
        }
        return "Non défini";
    }

    // Callback JPA
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}