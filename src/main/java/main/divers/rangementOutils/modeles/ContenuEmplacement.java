package main.divers.rangementOutils.modeles;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contenu_emplacements")
public class ContenuEmplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "contenu_emplacement_outils",
        joinColumns = @JoinColumn(name = "contenu_emplacement_id"),
        inverseJoinColumns = @JoinColumn(name = "outil_id")
    )
    private List<Outil> listeOutils = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "emplacement_id", nullable = false)
    private Emplacement emplacement;

    @ManyToOne
    @JoinColumn(name = "conteneur_id", nullable = false)
    private Conteneur conteneur;

    public ContenuEmplacement() {}

    public ContenuEmplacement(List<Outil> listeOutils, Emplacement emplacement, Conteneur conteneur) {
        this.listeOutils = listeOutils;
        this.emplacement = emplacement;
        this.conteneur = conteneur;
    }

    public Long getId() { return id; }
    public List<Outil> getListeOutils() { return listeOutils; }
    public Emplacement getEmplacement() { return emplacement; }
    public Conteneur getConteneur() { return conteneur; }

    public void setId(Long id) { this.id = id; }
    public void setListeOutils(List<Outil> listeOutils) { this.listeOutils = listeOutils; }
    public void setEmplacement(Emplacement emplacement) { this.emplacement = emplacement; }
    public void setConteneur(Conteneur conteneur) { this.conteneur = conteneur; }
}
