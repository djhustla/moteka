package main.divers.rangementOutils.repository;
import main.divers.rangementOutils.modeles.Emplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface EmplacementRepository extends JpaRepository<Emplacement, Long> {}
