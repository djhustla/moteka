package main.divers.rangementOutils.repository;
import main.divers.rangementOutils.modeles.Outil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface OutilRepository extends JpaRepository<Outil, Long> {
    Optional<Outil> findByNom(String nom);
    List<Outil> findByNomContainingIgnoreCase(String nom);
}
