package main.services;

import main.modeles.MusicFavoris;
import main.modeles.MusicGenre;
import main.modeles.User;
import main.repository.MusicFavorisRepository;
import main.repository.MusicGenreRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MusicFavorisService {

    private final MusicFavorisRepository musicFavorisRepository;
    private final UserRepository userRepository;
    private final MusicGenreRepository musicGenreRepository;

    @Autowired
    public MusicFavorisService(
            MusicFavorisRepository musicFavorisRepository,
            UserRepository userRepository,
            MusicGenreRepository musicGenreRepository
    ) {
        this.musicFavorisRepository = musicFavorisRepository;
        this.userRepository = userRepository;
        this.musicGenreRepository = musicGenreRepository;
    }

    // =====================
    // CRÉATION
    // =====================

    public MusicFavoris createMusicFavoris(MusicFavoris musicFavoris) {
        if (musicFavoris == null)
            throw new IllegalArgumentException("MusicFavoris ne peut pas être null");

        if (musicFavoris.getUser() == null)
            throw new IllegalArgumentException("MusicFavoris doit avoir un utilisateur");

        return musicFavorisRepository.save(musicFavoris);
    }

    public MusicFavoris createMusicFavoris(Long userId, Integer cotePreference, List<Long> genreIds) {

        if (userId == null)
            throw new IllegalArgumentException("L'ID utilisateur ne peut pas être null");

        if (cotePreference == null)
            throw new IllegalArgumentException("La cote de préférence ne peut pas être null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        Optional<MusicFavoris> existing =
                musicFavorisRepository.findByUserIdAndCotePreference(userId, cotePreference);

        if (existing.isPresent())
            throw new IllegalStateException("L'utilisateur a déjà un MusicFavoris avec la cote: " + cotePreference);

        List<MusicGenre> genres = new ArrayList<>();
        if (genreIds != null && !genreIds.isEmpty()) {
            genres = musicGenreRepository.findAllById(genreIds);

            if (genres.size() != genreIds.size())
                throw new RuntimeException("Certains genres n'existent pas");
        }

        MusicFavoris musicFavoris = new MusicFavoris(user, cotePreference, genres);
        return musicFavorisRepository.save(musicFavoris);
    }

    public MusicFavoris createMusicFavorisForUser(User user, Integer cotePreference) {
        if (user == null) throw new IllegalArgumentException("Utilisateur null");
        if (cotePreference == null) throw new IllegalArgumentException("Cote null");

        return musicFavorisRepository.save(new MusicFavoris(user, cotePreference));
    }

    // =====================
    // LECTURE
    // =====================

    public List<MusicFavoris> getAllMusicFavoris() {
        return musicFavorisRepository.findAll();
    }

    public Optional<MusicFavoris> getMusicFavorisById(Long id) {
        if (id == null) throw new IllegalArgumentException("ID null");
        return musicFavorisRepository.findById(id);
    }

    public List<MusicFavoris> getMusicFavorisByUserId(Long userId) {
        if (userId == null) throw new IllegalArgumentException("ID utilisateur null");
        return musicFavorisRepository.findByUserId(userId);
    }

    public Optional<MusicFavoris> getMusicFavorisByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null)
            throw new IllegalArgumentException("IDs invalides");
        return musicFavorisRepository.findByIdAndUserId(id, userId);
    }

    // =====================
    // MISE À JOUR
    // =====================

    public MusicFavoris updateMusicFavoris(Long id, MusicFavoris details) {
        if (id == null) throw new IllegalArgumentException("ID null");
        if (details == null) throw new IllegalArgumentException("Détails null");

        return musicFavorisRepository.findById(id)
                .map(ex -> {
                    ex.setCotePreference(details.getCotePreference());
                    ex.setGenres(details.getGenres());
                    return musicFavorisRepository.save(ex);
                })
                .orElseThrow(() -> new RuntimeException("MusicFavoris non trouvé avec ID: " + id));
    }

    public MusicFavoris updateMusicFavorisForUser(Long id, Long userId, MusicFavoris details) {
        if (id == null || userId == null)
            throw new IllegalArgumentException("IDs invalides");

        return musicFavorisRepository.findByIdAndUserId(id, userId)
                .map(ex -> {
                    ex.setCotePreference(details.getCotePreference());
                    ex.setGenres(details.getGenres());
                    return musicFavorisRepository.save(ex);
                })
                .orElseThrow(() ->
                        new RuntimeException("MusicFavoris non trouvé pour ID: " + id + " et user: " + userId));
    }

    public MusicFavoris addGenreToMusicFavoris(Long id, MusicGenre genre) {
        if (id == null) throw new IllegalArgumentException("ID null");
        if (genre == null) throw new IllegalArgumentException("Genre null");

        return musicFavorisRepository.findById(id)
                .map(f -> {
                    f.addGenre(genre);
                    return musicFavorisRepository.save(f);
                })
                .orElseThrow(() -> new RuntimeException("MusicFavoris non trouvé avec ID: " + id));
    }


    public List<MusicGenre> getGenresOfMusicFavoris(Long musicFavorisId) {
        // Validation
        if (musicFavorisId == null) {
            throw new IllegalArgumentException("L'ID du MusicFavoris ne peut pas être null");
        }

        // 1. Trouver le MusicFavoris
        MusicFavoris favoris = musicFavorisRepository.findById(musicFavorisId)
                .orElseThrow(() -> new RuntimeException(
                        "MusicFavoris non trouvé avec l'ID: " + musicFavorisId));

        // 2. Retourner les genres
        return favoris.getGenres();
    }

    /**
     * Supprime un genre spécifique d'un MusicFavoris
     * @param musicFavorisId L'ID du MusicFavoris
     * @param genreId L'ID du genre à supprimer
     * @return Le MusicFavoris mis à jour
     */
    public MusicFavoris removeGenreFromMusicFavoris(Long musicFavorisId, Long genreId) {
        // Validation des paramètres
        if (musicFavorisId == null) {
            throw new IllegalArgumentException("L'ID du MusicFavoris ne peut pas être null");
        }
        if (genreId == null) {
            throw new IllegalArgumentException("L'ID du genre ne peut pas être null");
        }

        // Récupérer le MusicFavoris
        return musicFavorisRepository.findById(musicFavorisId)
                .map(musicFavoris -> {
                    // Trouver le genre dans la liste
                    Optional<MusicGenre> genreToRemove = musicFavoris.getGenres().stream()
                            .filter(genre -> genreId.equals(genre.getId()))
                            .findFirst();

                    // Vérifier si le genre existe dans ce MusicFavoris
                    if (genreToRemove.isEmpty()) {
                        throw new RuntimeException(
                                "Le genre avec l'ID " + genreId + " n'existe pas dans ce MusicFavoris"
                        );
                    }

                    // Supprimer le genre
                    musicFavoris.removeGenre(genreToRemove.get());

                    // Sauvegarder les modifications
                    return musicFavorisRepository.save(musicFavoris);
                })
                .orElseThrow(() -> new RuntimeException(
                        "MusicFavoris non trouvé avec l'ID: " + musicFavorisId
                ));
    }

    /**
     * Supprime un genre d'un MusicFavoris spécifique d'un utilisateur
     * @param musicFavorisId L'ID du MusicFavoris
     * @param userId L'ID de l'utilisateur
     * @param genreId L'ID du genre à supprimer
     * @return Le MusicFavoris mis à jour
     */
    public MusicFavoris removeGenreFromUserMusicFavoris(Long musicFavorisId, Long userId, Long genreId) {
        // Validation des paramètres
        if (musicFavorisId == null) {
            throw new IllegalArgumentException("L'ID du MusicFavoris ne peut pas être null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("L'ID de l'utilisateur ne peut pas être null");
        }
        if (genreId == null) {
            throw new IllegalArgumentException("L'ID du genre ne peut pas être null");
        }

        // Récupérer le MusicFavoris spécifique à l'utilisateur
        return musicFavorisRepository.findByIdAndUserId(musicFavorisId, userId)
                .map(musicFavoris -> {
                    // Trouver le genre dans la liste
                    Optional<MusicGenre> genreToRemove = musicFavoris.getGenres().stream()
                            .filter(genre -> genreId.equals(genre.getId()))
                            .findFirst();

                    // Vérifier si le genre existe dans ce MusicFavoris
                    if (genreToRemove.isEmpty()) {
                        throw new RuntimeException(
                                "Le genre avec l'ID " + genreId + " n'existe pas dans ce MusicFavoris"
                        );
                    }

                    // Supprimer le genre
                    musicFavoris.removeGenre(genreToRemove.get());

                    // Sauvegarder les modifications
                    return musicFavorisRepository.save(musicFavoris);
                })
                .orElseThrow(() -> new RuntimeException(
                        "MusicFavoris non trouvé avec l'ID: " + musicFavorisId +
                                " pour l'utilisateur: " + userId
                ));
    }

    /**
     * Supprime plusieurs genres d'un MusicFavoris
     * @param musicFavorisId L'ID du MusicFavoris
     * @param genreIds Liste des IDs des genres à supprimer
     * @return Le MusicFavoris mis à jour
     */
    public MusicFavoris removeGenresFromMusicFavoris(Long musicFavorisId, List<Long> genreIds) {
        // Validation des paramètres
        if (musicFavorisId == null) {
            throw new IllegalArgumentException("L'ID du MusicFavoris ne peut pas être null");
        }
        if (genreIds == null || genreIds.isEmpty()) {
            throw new IllegalArgumentException("La liste des IDs de genres ne peut pas être null ou vide");
        }

        // Récupérer le MusicFavoris
        return musicFavorisRepository.findById(musicFavorisId)
                .map(musicFavoris -> {
                    // Récupérer tous les genres existants
                    List<MusicGenre> currentGenres = new ArrayList<>(musicFavoris.getGenres());

                    // Filtrer pour garder seulement les genres à ne PAS supprimer
                    List<MusicGenre> updatedGenres = currentGenres.stream()
                            .filter(genre -> !genreIds.contains(genre.getId()))
                            .toList();

                    // Vérifier si tous les genres à supprimer existaient
                    List<Long> existingGenreIds = currentGenres.stream()
                            .map(MusicGenre::getId)
                            .toList();

                    List<Long> nonExistentGenres = genreIds.stream()
                            .filter(id -> !existingGenreIds.contains(id))
                            .toList();

                    if (!nonExistentGenres.isEmpty()) {
                        throw new RuntimeException(
                                "Certains genres n'existent pas dans ce MusicFavoris: " + nonExistentGenres
                        );
                    }

                    // Mettre à jour la liste des genres
                    musicFavoris.setGenres(updatedGenres);

                    // Sauvegarder les modifications
                    return musicFavorisRepository.save(musicFavoris);
                })
                .orElseThrow(() -> new RuntimeException(
                        "MusicFavoris non trouvé avec l'ID: " + musicFavorisId
                ));
    }

    // =====================
    // SUPPRESSION
    // =====================

    public void deleteMusicFavoris(Long id) {
        if (id == null) throw new IllegalArgumentException("ID null");

        if (!musicFavorisRepository.existsById(id))
            throw new RuntimeException("MusicFavoris non trouvé avec ID: " + id);

        musicFavorisRepository.deleteById(id);
    }

    public void deleteMusicFavorisForUser(Long id, Long userId) {
        if (id == null || userId == null)
            throw new IllegalArgumentException("IDs invalides");

        if (!musicFavorisRepository.existsByIdAndUserId(id, userId))
            throw new RuntimeException("MusicFavoris non trouvé pour cet utilisateur");

        musicFavorisRepository.deleteByIdAndUserId(id, userId);
    }

    public void deleteAllMusicFavorisForUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("ID utilisateur null");

        musicFavorisRepository.deleteAll(musicFavorisRepository.findByUserId(userId));
    }

    // =====================
    // STATS
    // =====================

    public long countMusicFavorisByUserId(Long userId) {
        if (userId == null) throw new IllegalArgumentException("ID utilisateur null");
        return musicFavorisRepository.countByUserId(userId);
    }

    public long countAllMusicFavoris() {
        return musicFavorisRepository.count();
    }

    /**
     * Récupère la liste des utilisateurs ayant les mêmes goûts principaux
     * (même cote + au moins un genre commun)
     */
    public List<User> findUsersWithSameMainPreferences(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("L'ID utilisateur ne peut pas être null");
        }

        try {
            // 1. Récupérer les IDs des utilisateurs similaires
            List<Long> similarUserIds = musicFavorisRepository.findUsersWithSameGenresByCote(userId);

            if (similarUserIds.isEmpty()) {
                return List.of(); // Retourne liste vide si aucun utilisateur similaire
            }

            // 2. Récupérer les objets User complets
            return userRepository.findAllById(similarUserIds);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche d'utilisateurs similaires: " + e.getMessage(), e);
        }
    }


    /**
     * Trouve les utilisateurs ayant les mêmes kiffs (cote 10/10)
     * Retourne une liste d'utilisateurs
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithSameTopKiff(Long currentUserId) {
        try {
            // 1. Validation de l'utilisateur
            if (currentUserId == null) {
                throw new IllegalArgumentException("ID utilisateur ne peut pas être null");
            }

            // Vérifier que l'utilisateur existe
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Utilisateur introuvable avec l'ID: " + currentUserId
                    ));

            // 2. Récupérer le kiff principal (cote 10) de l'utilisateur
            // AVEC Optional - car on s'attend à un seul MusicFavoris par cote
            Optional<MusicFavoris> currentTopKiffOpt = musicFavorisRepository
                    .findByUserIdAndCotePreference(currentUserId, 10);

            // Si pas de kiff défini, retourner liste vide
            if (currentTopKiffOpt.isEmpty()) {
                return Collections.emptyList();
            }

            MusicFavoris currentTopKiff = currentTopKiffOpt.get();

            // 3. Récupérer les genres du kiff
            Set<Long> currentGenreIds = currentTopKiff.getGenres().stream()
                    .map(genre -> genre.getId())
                    .collect(Collectors.toSet());

            // Vérifier si le kiff a des genres
            if (currentGenreIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. Trouver les IDs des utilisateurs avec des genres en commun
            List<Long> similarUserIds = musicFavorisRepository
                    .findUsersWithCommonTopGenres(currentUserId);

            // Si personne n'a les mêmes kiffs
            if (similarUserIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 5. Récupérer les utilisateurs complets
            List<User> similarUsers = userRepository.findAllById(similarUserIds);

            // 6. Filtrer pour ne garder que ceux qui ont vraiment des genres communs
            // (double vérification pour être sûr)
            return similarUsers.stream()
                    .filter(user -> {
                        Optional<MusicFavoris> userTopKiffOpt = musicFavorisRepository
                                .findByUserIdAndCotePreference(user.getId(), 10);

                        if (userTopKiffOpt.isPresent()) {
                            Set<Long> userGenreIds = userTopKiffOpt.get().getGenres().stream()
                                    .map(genre -> genre.getId())
                                    .collect(Collectors.toSet());

                            // Vérifier s'il y a au moins un genre en commun
                            return userGenreIds.stream()
                                    .anyMatch(currentGenreIds::contains);
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

        } catch (IllegalArgumentException e) {
            // Relancer les erreurs de validation
            throw e;
        } catch (Exception e) {
            // Encapsuler les autres erreurs
            throw new RuntimeException(
                    "Erreur lors de la recherche des utilisateurs similaires: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Version simplifiée qui retourne juste la liste d'utilisateurs
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithOppositePreferencesSimple(Long currentUserId) {
        try {
            if (currentUserId == null) {
                throw new IllegalArgumentException("L'ID utilisateur ne peut pas être null");
            }

            List<Long> oppositeUserIds = musicFavorisRepository
                    .findUsersWithOppositePreferences(currentUserId);

            if (oppositeUserIds.isEmpty()) {
                return Collections.emptyList();
            }

            return userRepository.findAllById(oppositeUserIds);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur lors de la recherche d'utilisateurs avec préférences opposées: " + e.getMessage(),
                    e
            );
        }
    }

    // 1. OPPOSITIONS TOTALES
    @Transactional(readOnly = true)
    public List<User> getOppositionsTotal(User currentUser) {
        try {
            if (currentUser == null) {
                throw new RuntimeException("Utilisateur non connecté");
            }

            // Vérifier que l'utilisateur existe
            userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            return musicFavorisRepository.findOppositionsTotal(currentUser.getId());

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche des oppositions totales: " + e.getMessage(), e);
        }
    }

    // 2. MATCH TOTAL
    @Transactional(readOnly = true)
    public List<User> getMatchTotal(User currentUser) {
        try {
            if (currentUser == null) {
                throw new RuntimeException("Utilisateur non connecté");
            }

            // Vérifier que l'utilisateur existe
            userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            return musicFavorisRepository.findMatchTotal(currentUser.getId());

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche des matches totaux: " + e.getMessage(), e);
        }
    }

    // 3. CONCORDANCE MOYENNE
    @Transactional(readOnly = true)
    public List<User> getConcordenceMoyenne(User currentUser) {
        try {
            if (currentUser == null) {
                throw new RuntimeException("Utilisateur non connecté");
            }

            // Vérifier que l'utilisateur existe
            userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            return musicFavorisRepository.findConcordenceMoyenne(currentUser.getId());

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche des concordances moyennes: " + e.getMessage(), e);
        }
    }





}
