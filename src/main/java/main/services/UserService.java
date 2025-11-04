package main.services;

import main.modeles.MusicFavoris;
import main.modeles.MusicGenre;
import main.modeles.User;
import main.repository.MusicFavorisRepository;
import main.repository.MusicGenreRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MusicFavorisRepository musicFavorisRepository;

    @Autowired
    private MusicGenreRepository musicGenreRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // === FONCTIONS EXISTANTES ===

    public User createUser(String username, String email, String password, String photoUrl) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setPhotoUrl(photoUrl);

        return userRepository.save(user);
    }

    public User createUserWithRole(String username, String email, String password, String role, String photoUrl) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.toUpperCase());
        user.setPhotoUrl(photoUrl);

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User updateUser(User user) {
        if (user == null) {
            throw new RuntimeException("L'utilisateur ne peut pas être null");
        }

        if (user.getId() == null) {
            throw new RuntimeException("Impossible de mettre à jour un utilisateur sans ID");
        }

        if (!userRepository.existsById(user.getId())) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + user.getId());
        }

        return userRepository.save(user);
    }

    // === NOUVELLE FONCTION : SUPPRESSION COMPLÈTE D'UN UTILISATEUR ===
    @Transactional
    public void deleteUserWithAllData(Long userId) {
        try {
            // Vérifier que l'utilisateur existe
            if (!userRepository.existsById(userId)) {
                throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
            }

            // 1. Supprimer les messages privés des conversations de l'user
            Query deleteMessagesPrives = entityManager.createNativeQuery(
                    "DELETE FROM messages_prives WHERE conversation_id IN " +
                            "(SELECT id FROM conversations WHERE user1_id = ? OR user2_id = ?)"
            );
            deleteMessagesPrives.setParameter(1, userId);
            deleteMessagesPrives.setParameter(2, userId);
            int messagesPrivesDeleted = deleteMessagesPrives.executeUpdate();
            System.out.println("Messages privés supprimés: " + messagesPrivesDeleted);

            // 2. Supprimer les conversations
            Query deleteConversations = entityManager.createNativeQuery(
                    "DELETE FROM conversations WHERE user1_id = ? OR user2_id = ?"
            );
            deleteConversations.setParameter(1, userId);
            deleteConversations.setParameter(2, userId);
            int conversationsDeleted = deleteConversations.executeUpdate();
            System.out.println("Conversations supprimées: " + conversationsDeleted);

            // 3. Supprimer les commentaires
            Query deleteCommentaires = entityManager.createNativeQuery(
                    "DELETE FROM commentaires WHERE user_id = ?"
            );
            deleteCommentaires.setParameter(1, userId);
            int commentairesDeleted = deleteCommentaires.executeUpdate();
            System.out.println("Commentaires supprimés: " + commentairesDeleted);

            // 4. Supprimer les messages publics
            Query deleteMessages = entityManager.createNativeQuery(
                    "DELETE FROM messages WHERE user_id = ?"
            );
            deleteMessages.setParameter(1, userId);
            int messagesDeleted = deleteMessages.executeUpdate();
            System.out.println("Messages publics supprimés: " + messagesDeleted);

            // 5. Supprimer les favoris musicaux
            Query deleteMusicFavoris = entityManager.createNativeQuery(
                    "DELETE FROM music_favoris WHERE user_id = ?"
            );
            deleteMusicFavoris.setParameter(1, userId);
            int favorisDeleted = deleteMusicFavoris.executeUpdate();
            System.out.println("Favoris musicaux supprimés: " + favorisDeleted);

            // 6. Supprimer l'user
            Query deleteUser = entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = ?"
            );
            deleteUser.setParameter(1, userId);
            int userDeleted = deleteUser.executeUpdate();

            if (userDeleted == 0) {
                throw new RuntimeException("Échec de la suppression de l'utilisateur avec l'ID: " + userId);
            }

            System.out.println("Utilisateur supprimé avec succès. ID: " + userId);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de l'utilisateur " + userId + ": " + e.getMessage(), e);
        }
    }

    // === FONCTIONS POUR MUSICFAVORIS ===

    // Créer la liste des musicFavoris
    public List<MusicFavoris> createMusicFavoris(Long userId, List<MusicFavorisRequest> favorisRequests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier qu'on ne dépasse pas 4 favoris
        if (favorisRequests.size() > 4) {
            throw new RuntimeException("Un utilisateur ne peut avoir que 4 favoris maximum");
        }

        // Supprimer d'abord tous les favoris existants
        try {
            List<MusicFavoris> existingFavoris = musicFavorisRepository.findByUserId(userId);
            musicFavorisRepository.deleteAll(existingFavoris);
        } catch (Exception e) {
            // Log l'erreur mais continue
            System.out.println("Aucun favori existant à supprimer: " + e.getMessage());
        }

        // Créer les nouveaux favoris
        List<MusicFavoris> newFavoris = new ArrayList<>();

        for (MusicFavorisRequest request : favorisRequests) {
            MusicGenre musicGenre = musicGenreRepository.findById(request.getMusicGenreId())
                    .orElseThrow(() -> new RuntimeException("Genre musical non trouvé avec l'ID: " + request.getMusicGenreId()));

            MusicFavoris favoris = new MusicFavoris(user, musicGenre, request.getCotePreference());
            MusicFavoris savedFavoris = musicFavorisRepository.save(favoris);
            newFavoris.add(savedFavoris);
        }

        return newFavoris;
    }

    // Afficher la liste des musicFavoris
    public List<MusicFavoris> getMusicFavoris(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        return musicFavorisRepository.findByUserId(userId);
    }

    // Modifier la liste des favoris
    public List<MusicFavoris> updateMusicFavoris(Long userId, List<MusicFavorisRequest> favorisRequests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier qu'on ne dépasse pas 4 favoris
        if (favorisRequests.size() > 4) {
            throw new RuntimeException("Un utilisateur ne peut avoir que 4 favoris maximum");
        }

        // Supprimer les anciens favoris
        musicFavorisRepository.deleteByUserId(userId);

        // Créer les nouveaux favoris
        List<MusicFavoris> updatedFavoris = new ArrayList<>();
        Set<Integer> usedCotes = new HashSet<>();

        for (MusicFavorisRequest request : favorisRequests) {
            MusicGenre musicGenre = musicGenreRepository.findById(request.getMusicGenreId())
                    .orElseThrow(() -> new RuntimeException("Genre musical non trouvé avec l'ID: " + request.getMusicGenreId()));

            // Vérifier que la cote de préférence est valide (1-4)
            if (request.getCotePreference() < 1 || request.getCotePreference() > 4) {
                throw new RuntimeException("La cote de préférence doit être entre 1 et 4");
            }

            // Vérifier qu'il n'y a pas de doublons de cote de préférence
            if (usedCotes.contains(request.getCotePreference())) {
                throw new RuntimeException("La cote de préférence " + request.getCotePreference() + " est déjà utilisée");
            }
            usedCotes.add(request.getCotePreference());

            MusicFavoris favoris = new MusicFavoris(user, musicGenre, request.getCotePreference());
            MusicFavoris savedFavoris = musicFavorisRepository.save(favoris);
            updatedFavoris.add(savedFavoris);
        }

        return updatedFavoris;
    }

    // === FONCTION OPTIMISÉE : Trouver les utilisateurs avec exactement les mêmes favoris ===
    public List<User> getUsersWithExactSameMusicFavoris(Long userId) {
        // Vérifier que l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        // REQUÊTE UNIQUE optimisée
        return userRepository.findUsersWithExactSameMusicFavoris(userId);
    }

    // === FONCTION OPTIMISÉE : Trouver les utilisateurs avec des préférences musicales opposées ===
    public List<User> getUsersWithOppositeMusicFavoris(Long userId) {
        // Vérifier que l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        // Récupérer uniquement les favoris avec cote 1 et 10 de l'utilisateur cible
        List<MusicFavoris> extremeFavoris = musicFavorisRepository.findByUserIdAndCotePreferenceIn(userId, Arrays.asList(1, 10));

        if (extremeFavoris.isEmpty()) {
            return new ArrayList<>();
        }

        // Séparer les genres avec cote 1 et cote 10
        Set<Long> genresCote10 = new HashSet<>();
        Set<Long> genresCote1 = new HashSet<>();

        for (MusicFavoris favoris : extremeFavoris) {
            if (favoris.getCotePreference() == 10) {
                genresCote10.add(favoris.getMusicGenre().getId());
            } else if (favoris.getCotePreference() == 1) {
                genresCote1.add(favoris.getMusicGenre().getId());
            }
        }

        // Si pas de genres extrêmes, retourner liste vide
        if (genresCote10.isEmpty() && genresCote1.isEmpty()) {
            return new ArrayList<>();
        }

        // REQUÊTE UNIQUE : Récupérer tous les utilisateurs qui ont des préférences opposées
        List<User> usersWithOppositeFavoris;

        if (!genresCote10.isEmpty() && !genresCote1.isEmpty()) {
            // Cas où l'user a à la fois des cotes 1 et 10
            usersWithOppositeFavoris = userRepository.findUsersWithOppositePreferences(
                    userId, genresCote10, genresCote1);
        } else if (!genresCote10.isEmpty()) {
            // Cas où l'user a seulement des cotes 10
            usersWithOppositeFavoris = userRepository.findUsersWhoHateLovedGenres(userId, genresCote10);
        } else {
            // Cas où l'user a seulement des cotes 1
            usersWithOppositeFavoris = userRepository.findUsersWhoLoveHatedGenres(userId, genresCote1);
        }

        return usersWithOppositeFavoris;
    }

    // === VERSION LA PLUS SIMPLE ET OPTIMALE ===
    public List<User> getUsersWithSameTopMusicFavoris(Long userId) {
        return userRepository.findUsersWithSameTopFavoris(userId);
    }

    // Classe interne pour la requête
    public static class MusicFavorisRequest {
        private Long musicGenreId;
        private Integer cotePreference;

        // Getters et Setters
        public Long getMusicGenreId() {
            return musicGenreId;
        }

        public void setMusicGenreId(Long musicGenreId) {
            this.musicGenreId = musicGenreId;
        }

        public Integer getCotePreference() {
            return cotePreference;
        }

        public void setCotePreference(Integer cotePreference) {
            this.cotePreference = cotePreference;
        }
    }
}