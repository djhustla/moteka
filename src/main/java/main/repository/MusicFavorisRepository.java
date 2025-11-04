package main.repository;

import main.modeles.MusicFavoris;
import main.modeles.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicFavorisRepository extends JpaRepository<MusicFavoris, Long> {

    // Trouver tous les favoris d'un utilisateur
    List<MusicFavoris> findByUserId(Long userId);

    // Trouver un favori spécifique par utilisateur et genre musical
    Optional<MusicFavoris> findByUserIdAndMusicGenreId(Long userId, Long musicGenreId);

    // Vérifier si un favori existe pour un utilisateur et genre musical
    boolean existsByUserIdAndMusicGenreId(Long userId, Long musicGenreId);

    // Supprimer tous les favoris d'un utilisateur
    @Modifying
    @Query("DELETE FROM MusicFavoris mf WHERE mf.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // Supprimer un favori spécifique par utilisateur et genre musical
    @Modifying
    @Query("DELETE FROM MusicFavoris mf WHERE mf.user.id = :userId AND mf.musicGenre.id = :musicGenreId")
    void deleteByUserIdAndMusicGenreId(@Param("userId") Long userId, @Param("musicGenreId") Long musicGenreId);

    // Compter le nombre de favoris d'un utilisateur
    long countByUserId(Long userId);

    // Dans MusicFavorisRepository
    List<MusicFavoris> findByUserIdAndCotePreferenceIn(Long userId, List<Integer> cotes);
}