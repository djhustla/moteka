package main.repository;

import main.modeles.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByUsernameIn(List<String> usernames);


    // Méthode pour les deux cas
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN MusicFavoris mf ON mf.user.id = u.id " +
            "WHERE u.id <> :userId " +
            "AND ((mf.musicGenre.id IN :genresCote10 AND mf.cotePreference = 1) " +
            "OR (mf.musicGenre.id IN :genresCote1 AND mf.cotePreference = 10))")
    List<User> findUsersWithOppositePreferences(@Param("userId") Long userId,
                                                @Param("genresCote10") Set<Long> genresCote10,
                                                @Param("genresCote1") Set<Long> genresCote1);

    // Méthode pour seulement le cas "genres aimés -> détestés"
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN MusicFavoris mf ON mf.user.id = u.id " +
            "WHERE u.id <> :userId " +
            "AND mf.musicGenre.id IN :genresCote10 " +
            "AND mf.cotePreference = 1")
    List<User> findUsersWhoHateLovedGenres(@Param("userId") Long userId,
                                           @Param("genresCote10") Set<Long> genresCote10);

    // Méthode pour seulement le cas "genres détestés -> aimés"
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN MusicFavoris mf ON mf.user.id = u.id " +
            "WHERE u.id <> :userId " +
            "AND mf.musicGenre.id IN :genresCote1 " +
            "AND mf.cotePreference = 10")
    List<User> findUsersWhoLoveHatedGenres(@Param("userId") Long userId,
                                           @Param("genresCote1") Set<Long> genresCote1);

    @Query("SELECT DISTINCT u2 FROM User u1 " +
            "JOIN MusicFavoris mf1 ON mf1.user.id = u1.id " +
            "JOIN MusicFavoris mf2 ON mf2.musicGenre.id = mf1.musicGenre.id " +
            "JOIN User u2 ON u2.id = mf2.user.id " +
            "WHERE u1.id = :userId " +
            "AND mf1.cotePreference = 10 " +
            "AND mf2.cotePreference = 10 " +
            "AND u2.id <> :userId")
    List<User> findUsersWithSameTopFavoris(@Param("userId") Long userId);

    @Query(value = """
    SELECT u.* FROM users u
    WHERE u.id != :userId
    AND NOT EXISTS (
        -- Genres que l'user1 a mais pas l'user2
        SELECT mf1.music_genre_id, mf1.cote_preference
        FROM music_favoris mf1
        WHERE mf1.user_id = :userId
        AND NOT EXISTS (
            SELECT 1 FROM music_favoris mf2
            WHERE mf2.user_id = u.id
            AND mf2.music_genre_id = mf1.music_genre_id
            AND mf2.cote_preference = mf1.cote_preference
        )
    )
    AND NOT EXISTS (
        -- Genres que l'user2 a mais pas l'user1
        SELECT mf2.music_genre_id, mf2.cote_preference
        FROM music_favoris mf2
        WHERE mf2.user_id = u.id
        AND NOT EXISTS (
            SELECT 1 FROM music_favoris mf1
            WHERE mf1.user_id = :userId
            AND mf1.music_genre_id = mf2.music_genre_id
            AND mf1.cote_preference = mf2.cote_preference
        )
    )
    """, nativeQuery = true)
    List<User> findUsersWithExactSameMusicFavoris(@Param("userId") Long userId);

    // Recherche d'utilisateurs par username (insensible à la casse)
    List<User> findByUsernameContainingIgnoreCase(String username);
}