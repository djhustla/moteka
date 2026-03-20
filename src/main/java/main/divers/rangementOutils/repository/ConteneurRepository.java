package main.divers.rangementOutils.repository;
import main.divers.rangementOutils.modeles.Conteneur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ConteneurRepository extends JpaRepository<Conteneur, Long> {
    Optional<Conteneur> findByNom(String nom);
    List<Conteneur> findByNomContainingIgnoreCase(String nom);
}
