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
        user.setDonneeVisible("100");

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

    // === FONCTIONS POUR DONNEEVISIBLE ===

    // Récupérer la donnée visible d'un utilisateur
    public String getDonneeVisible(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        return user.getDonneeVisible();
    }

    // Mettre à jour la donnée visible
    public User updateDonneeVisible(Long userId, String donneeVisible) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        user.setDonneeVisible(donneeVisible);
        return userRepository.save(user);
    }

    // Mettre à jour la donnée visible (version avec validation optionnelle)
    public User updateDonneeVisibleWithValidation(Long userId, String donneeVisible, Integer maxLength) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // Validation optionnelle de la longueur
        if (maxLength != null && donneeVisible != null && donneeVisible.length() > maxLength) {
            throw new RuntimeException("La donnée visible ne peut pas dépasser " + maxLength + " caractères");
        }

        user.setDonneeVisible(donneeVisible);
        return userRepository.save(user);
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

    public List<User> searchUsersByUsername(String usernameQuery) {
        if (usernameQuery == null || usernameQuery.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository.findByUsernameContainingIgnoreCase(usernameQuery.trim());
    }

    // === NOUVELLE FONCTION : SUPPRESSION COMPLÈTE D'UN UTILISATEUR ===
    @Transactional
    public void deleteUserWithAllData(Long userId) {
        try {
            // Vérifier que l'utilisateur existe
            if (!userRepository.existsById(userId)) {
                throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
            }

            // ===== ORDRE DES SUPPRESSIONS (de dépendance faible à forte) =====

            // 1. Supprimer les favoris musicaux (table: music_favoris)
            Query deleteMusicFavoris = entityManager.createNativeQuery(
                    "DELETE FROM music_favoris WHERE user_id = ?"
            );
            deleteMusicFavoris.setParameter(1, userId);
            int musicFavorisDeleted = deleteMusicFavoris.executeUpdate();
            System.out.println("Favoris musicaux supprimés: " + musicFavorisDeleted);

            // 2. Supprimer les commentaires écrits par l'utilisateur (table: commentaires)
            Query deleteCommentairesByUser = entityManager.createNativeQuery(
                    "DELETE FROM commentaires WHERE user_id = ?"
            );
            deleteCommentairesByUser.setParameter(1, userId);
            int commentairesByUserDeleted = deleteCommentairesByUser.executeUpdate();
            System.out.println("Commentaires écrits par l'user supprimés: " + commentairesByUserDeleted);

            // 3. Supprimer les commentaires sur les messages de l'utilisateur (table: commentaires via messages)
            Query deleteCommentairesOnUserMessages = entityManager.createNativeQuery(
                    "DELETE FROM commentaires WHERE message_id IN " +
                            "(SELECT id FROM messages WHERE user_id = ?)"
            );
            deleteCommentairesOnUserMessages.setParameter(1, userId);
            int commentairesOnMessagesDeleted = deleteCommentairesOnUserMessages.executeUpdate();
            System.out.println("Commentaires sur les messages de l'user supprimés: " + commentairesOnMessagesDeleted);

            // 4. Supprimer les messages publics de l'utilisateur (table: messages)
            Query deleteMessages = entityManager.createNativeQuery(
                    "DELETE FROM messages WHERE user_id = ?"
            );
            deleteMessages.setParameter(1, userId);
            int messagesDeleted = deleteMessages.executeUpdate();
            System.out.println("Messages publics supprimés: " + messagesDeleted);

            // 5. Supprimer les messages privés de l'utilisateur (table: messages_prives via conversations)
            Query deleteMessagesPrives = entityManager.createNativeQuery(
                    "DELETE FROM messages_prives WHERE conversation_id IN " +
                            "(SELECT id FROM conversations WHERE user1_id = ? OR user2_id = ?)"
            );
            deleteMessagesPrives.setParameter(1, userId);
            deleteMessagesPrives.setParameter(2, userId);
            int messagesPrivesDeleted = deleteMessagesPrives.executeUpdate();
            System.out.println("Messages privés supprimés: " + messagesPrivesDeleted);

            // 6. Supprimer les conversations impliquant l'utilisateur (table: conversations)
            Query deleteConversations = entityManager.createNativeQuery(
                    "DELETE FROM conversations WHERE user1_id = ? OR user2_id = ?"
            );
            deleteConversations.setParameter(1, userId);
            deleteConversations.setParameter(2, userId);
            int conversationsDeleted = deleteConversations.executeUpdate();
            System.out.println("Conversations supprimées: " + conversationsDeleted);

            // 7. Supprimer l'utilisateur lui-même (table: users)
            Query deleteUser = entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = ?"
            );
            deleteUser.setParameter(1, userId);
            int userDeleted = deleteUser.executeUpdate();
            System.out.println("Utilisateur supprimé: " + userDeleted);

            // ===== RÉSUMÉ =====
            System.out.println("===== SUPPRESSION TERMINÉE =====");
            System.out.println("• Favoris musicaux: " + musicFavorisDeleted);
            System.out.println("• Commentaires écrits par l'user: " + commentairesByUserDeleted);
            System.out.println("• Commentaires sur messages de l'user: " + commentairesOnMessagesDeleted);
            System.out.println("• Messages publics: " + messagesDeleted);
            System.out.println("• Messages privés: " + messagesPrivesDeleted);
            System.out.println("• Conversations: " + conversationsDeleted);
            System.out.println("• Utilisateur: " + userDeleted);

        } catch (Exception e) {
            // Annuler la transaction en cas d'erreur
            throw new RuntimeException("Erreur lors de la suppression de l'utilisateur " + userId + ": " + e.getMessage(), e);
        }
    }

    // Mettre à jour les informations de l'utilisateur courant
    public User updateCurrentUser(Long userId, String username, String email,
                                  String currentPassword, String newPassword,
                                  boolean removePhoto, String photoUrl) {

        // 1. Récupérer l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // 2. Vérifier l'unicité du username si fourni
        if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("Ce nom d'utilisateur est déjà pris");
            }
            user.setUsername(username.trim());
        }

        // 3. Vérifier l'unicité de l'email si fourni
        if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            user.setEmail(email.trim());
        }

        // 4. Gestion du mot de passe
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new RuntimeException("Le mot de passe actuel est requis pour changer de mot de passe");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("Mot de passe actuel incorrect");
            }
            if (newPassword.length() < 4) {
                throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 4 caractères");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // 5. Gestion de la photo
        if (removePhoto) {
            user.setPhotoUrl(null);
        } else if (photoUrl != null) {
            user.setPhotoUrl(photoUrl);
        }

        // 6. Sauvegarder
        return userRepository.save(user);
    }

    // Mettre à jour la liste des réseaux sociaux
    public User updateListeReseauxCurrentUser(Long userId, String listeReseauxSociaux) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        user.setListeReseaux(listeReseauxSociaux);

        return userRepository.save(user);
    }
}