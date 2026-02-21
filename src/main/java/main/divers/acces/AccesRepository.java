package main.divers.acces;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccesRepository extends JpaRepository<Acces, Long> {

    // Recherche EXACTE par nom (pour vérification d'existence)
    Optional<Acces> findByNomAcces(String nomAcces);

    // Recherche PARTIELLE par nom (pour recherche utilisateur)
    List<Acces> findByNomAccesContainingIgnoreCase(String nomAcces);

    // Recherche PARTIELLE par ligneBkVoie
    List<Acces> findByLigneBkVoieContainingIgnoreCase(String ligneBkVoie);

    // Recherche PARTIELLE par description
    List<Acces> findByDescriptionContainingIgnoreCase(String description);

    // Vérification d'existence (reste exacte)
    boolean existsByNomAcces(String nomAcces);

    // Cette requête récupère uniquement la colonne ligneBkVoie
    // "DISTINCT" évite d'avoir des doublons dans la liste
    @Query("SELECT DISTINCT a.ligneBkVoie FROM Acces a")
    List<String> findAllLigneBkVoie();
}