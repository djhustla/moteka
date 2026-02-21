package main.repository;

import main.modeles.MusicGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MusicGenreRepository extends JpaRepository<MusicGenre, Long> {
    // Méthodes personnalisées si nécessaire
    boolean existsByDescription(String description);
}