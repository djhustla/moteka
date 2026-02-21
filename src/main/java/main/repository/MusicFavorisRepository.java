package main.repository;

import main.modeles.MusicFavoris;
import main.modeles.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MusicFavorisRepository extends JpaRepository<MusicFavoris, Long> {

    // Trouver tous les MusicFavoris d'un utilisateur spécifique
    List<MusicFavoris> findByUserId(Long userId);

    // Trouver un MusicFavoris spécifique d'un utilisateur
    Optional<MusicFavoris> findByIdAndUserId(Long id, Long userId);

    // Vérifier si un MusicFavoris existe pour un utilisateur
    boolean existsByIdAndUserId(Long id, Long userId);

    // Supprimer un MusicFavoris spécifique d'un utilisateur
    void deleteByIdAndUserId(Long id, Long userId);

    // Compter le nombre de MusicFavoris d'un utilisateur
    long countByUserId(Long userId);

    // Trouver par cote de préférence et utilisateur
    Optional<MusicFavoris> findByUserIdAndCotePreference(Long userId, Integer cotePreference);

    @Query("SELECT mf2.user.id " +
            "FROM MusicFavoris mf1 " +
            "JOIN mf1.genres g1 " +
            "JOIN MusicFavoris mf2 ON mf2.cotePreference = mf1.cotePreference " +
            "JOIN mf2.genres g2 ON g2.id = g1.id " +
            "WHERE mf1.user.id = :userId " +
            "AND mf2.user.id != :userId " +
            "GROUP BY mf2.user.id " +
            "HAVING COUNT(DISTINCT mf1.cotePreference) = " +
            "(SELECT COUNT(DISTINCT mf3.cotePreference) FROM MusicFavoris mf3 WHERE mf3.user.id = :userId)")
    List<Long> findUsersWithSameGenresByCote(@Param("userId") Long userId);

    /**
     * Trouve les utilisateurs qui ont au moins un genre en commun
     * dans leurs MusicFavoris avec cote 10/10
     */
    @Query("""
        SELECT DISTINCT uf.user.id 
        FROM MusicFavoris uf 
        JOIN uf.genres g 
        WHERE uf.cotePreference = 10 
          AND g.id IN (
              SELECT g2.id 
              FROM MusicFavoris currentFavoris 
              JOIN currentFavoris.genres g2 
              WHERE currentFavoris.cotePreference = 10 
                AND currentFavoris.user.id = :currentUserId
          ) 
          AND uf.user.id != :currentUserId
    """)
    List<Long> findUsersWithCommonTopGenres(@Param("currentUserId") Long currentUserId);

    // NOUVELLE MÉTHODE avec Optional (pour Option 2)
    @Query("SELECT mf FROM MusicFavoris mf " +
            "WHERE mf.user.id = :userId " +
            "AND mf.cotePreference = :cotePreference")
    Optional<MusicFavoris> findOneByUserIdAndCotePreference(
            @Param("userId") Long userId,
            @Param("cotePreference") Integer cotePreference
    );

    /**
     * Trouve les utilisateurs avec des préférences opposées
     * (Les deux cas ci-dessus combinés)
     */
    @Query("""
        SELECT DISTINCT uf.user.id 
        FROM MusicFavoris uf 
        JOIN uf.genres g 
        WHERE (
            (uf.cotePreference = 10 AND g.id IN (
                SELECT g2.id 
                FROM MusicFavoris currentFavoris 
                JOIN currentFavoris.genres g2 
                WHERE currentFavoris.cotePreference = 2 
                  AND currentFavoris.user.id = :currentUserId
            ))
            OR
            (uf.cotePreference = 2 AND g.id IN (
                SELECT g2.id 
                FROM MusicFavoris currentFavoris 
                JOIN currentFavoris.genres g2 
                WHERE currentFavoris.cotePreference = 10 
                  AND currentFavoris.user.id = :currentUserId
            ))
        )
        AND uf.user.id != :currentUserId
    """)
    List<Long> findUsersWithOppositePreferences(@Param("currentUserId") Long currentUserId);

    @Query("SELECT DISTINCT u FROM User u " +
            "WHERE u.id != :currentUserId " +
            // Vérifie que tous les genres en 10/10 de l'utilisateur courant
            // sont dans les genres en 2/10 de l'autre utilisateur
            "AND NOT EXISTS (" +
            "    SELECT g1 FROM MusicFavoris mf1 " +
            "    JOIN mf1.genres g1 " +
            "    WHERE mf1.user.id = :currentUserId " +
            "    AND mf1.cotePreference = 10 " +
            "    AND g1 NOT IN (" +
            "        SELECT g2 FROM MusicFavoris mf2 " +
            "        JOIN mf2.genres g2 " +
            "        WHERE mf2.user.id = u.id " +
            "        AND mf2.cotePreference = 2" +
            "    )" +
            ") " +
            // Vérifie que tous les genres en 2/10 de l'utilisateur courant
            // sont dans les genres en 10/10 de l'autre utilisateur
            "AND NOT EXISTS (" +
            "    SELECT g3 FROM MusicFavoris mf3 " +
            "    JOIN mf3.genres g3 " +
            "    WHERE mf3.user.id = :currentUserId " +
            "    AND mf3.cotePreference = 2 " +
            "    AND g3 NOT IN (" +
            "        SELECT g4 FROM MusicFavoris mf4 " +
            "        JOIN mf4.genres g4 " +
            "        WHERE mf4.user.id = u.id " +
            "        AND mf4.cotePreference = 10" +
            "    )" +
            ") " +
            // Vérifie que tous les genres en 10/10 de l'autre utilisateur
            // sont dans les genres en 2/10 de l'utilisateur courant
            "AND NOT EXISTS (" +
            "    SELECT g5 FROM MusicFavoris mf5 " +
            "    JOIN mf5.genres g5 " +
            "    WHERE mf5.user.id = u.id " +
            "    AND mf5.cotePreference = 10 " +
            "    AND g5 NOT IN (" +
            "        SELECT g6 FROM MusicFavoris mf6 " +
            "        JOIN mf6.genres g6 " +
            "        WHERE mf6.user.id = :currentUserId " +
            "        AND mf6.cotePreference = 2" +
            "    )" +
            ") " +
            // Vérifie que tous les genres en 2/10 de l'autre utilisateur
            // sont dans les genres en 10/10 de l'utilisateur courant
            "AND NOT EXISTS (" +
            "    SELECT g7 FROM MusicFavoris mf7 " +
            "    JOIN mf7.genres g7 " +
            "    WHERE mf7.user.id = u.id " +
            "    AND mf7.cotePreference = 2 " +
            "    AND g7 NOT IN (" +
            "        SELECT g8 FROM MusicFavoris mf8 " +
            "        JOIN mf8.genres g8 " +
            "        WHERE mf8.user.id = :currentUserId " +
            "        AND mf8.cotePreference = 10" +
            "    )" +
            ")")
    List<User> findOppositionsTotal(@Param("currentUserId") Long currentUserId);






    // 2. MATCH TOTAL
    @Query("SELECT DISTINCT u FROM User u " +
            "WHERE u.id != :currentUserId " +
            "AND NOT EXISTS (" +
            "    SELECT mf1 FROM MusicFavoris mf1 " +
            "    WHERE mf1.user.id = :currentUserId " +
            "    AND mf1.cotePreference IN (2, 5, 7, 10) " +
            "    AND NOT EXISTS (" +
            "        SELECT mf2 FROM MusicFavoris mf2 " +
            "        WHERE mf2.user.id = u.id " +
            "        AND mf2.cotePreference = mf1.cotePreference " +
            // Mêmes genres (même taille et mêmes éléments)
            "        AND SIZE(mf2.genres) = SIZE(mf1.genres) " +
            "        AND NOT EXISTS (" +
            "            SELECT g FROM mf1.genres g " +
            "            WHERE g NOT IN (SELECT g2 FROM mf2.genres g2)" +
            "        )" +
            "    )" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT mf3 FROM MusicFavoris mf3 " +
            "    WHERE mf3.user.id = u.id " +
            "    AND mf3.cotePreference IN (2, 5, 7, 10) " +
            "    AND NOT EXISTS (" +
            "        SELECT mf4 FROM MusicFavoris mf4 " +
            "        WHERE mf4.user.id = :currentUserId " +
            "        AND mf4.cotePreference = mf3.cotePreference " +
            // Mêmes genres (même taille et mêmes éléments)
            "        AND SIZE(mf4.genres) = SIZE(mf3.genres) " +
            "        AND NOT EXISTS (" +
            "            SELECT g FROM mf3.genres g " +
            "            WHERE g NOT IN (SELECT g2 FROM mf4.genres g2)" +
            "        )" +
            "    )" +
            ")")
    List<User> findMatchTotal(@Param("currentUserId") Long currentUserId);



    // 3. CONCORDANCE MOYENNE - VERSION CORRIGÉE
    @Query("SELECT DISTINCT u FROM User u " +
            "WHERE u.id != :currentUserId " +
            "AND EXISTS (" +
            "    SELECT mf1 FROM MusicFavoris mf1 " +
            "    JOIN mf1.genres g1 " +
            "    WHERE mf1.user.id = :currentUserId " +
            "    AND mf1.cotePreference >= 7 " +
            "    AND EXISTS (" +
            "        SELECT mf2 FROM MusicFavoris mf2 " +
            "        JOIN mf2.genres g2 " +
            "        WHERE mf2.user.id = u.id " +
            "        AND mf2.cotePreference >= 7 " +
            "        AND g1.id = g2.id" +
            "    )" +
            ")")
    List<User> findConcordenceMoyenne(@Param("currentUserId") Long currentUserId);
}