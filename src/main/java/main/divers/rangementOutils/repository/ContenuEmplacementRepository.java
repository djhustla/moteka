package main.divers.rangementOutils.repository;
import main.divers.rangementOutils.modeles.ContenuEmplacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ContenuEmplacementRepository extends JpaRepository<ContenuEmplacement, Long> {}
