package main.repository;


import main.modeles.MesModerateurs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesModerateursRepository extends JpaRepository<MesModerateurs, Long> {
    // Possibilité de chercher par username si besoin
    boolean existsByUsername(String username);
}