package main.services;

import main.modeles.User;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;

import static main.divers_services.CodeGenerator.genererCode;
import static main.divers_services.EnvoyerMail.envoyerEmail;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    @PersistenceContext
    private EntityManager entityManager;

    // === FONCTIONS EXISTANTES MODIFIÉES ===

    public User createUser(String username, String email, String password, String phoneNumber, String photoUrl) {
        // Validation des données
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new RuntimeException("Numéro de téléphone déjà utilisé: " + phoneNumber);
        }

        // Création de l'utilisateur
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setRole("USER");
        user.setPhotoUrl(photoUrl);
        user.setDonneeVisible("100");
        user.setIsActive(false); // Compte non activé par défaut

        // Générer et envoyer le code de validation
        String validationCode = genererCode();
        user.setValidationCode(validationCode);

        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);

        // Envoyer le code par SMS
        String message = "Votre code de validation est: " + validationCode;


        envoyerEmail(email ,message);

        return savedUser;
    }

    public User createUserWithRole(String username, String email, String password, String phoneNumber, String role, String photoUrl) {
        // Validation des données
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username déjà utilisé: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé: " + email);
        }
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new RuntimeException("Numéro de téléphone déjà utilisé: " + phoneNumber);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhoneNumber(phoneNumber);
        user.setRole(role.toUpperCase());
        user.setPhotoUrl(photoUrl);
        user.setIsActive(false); // Compte non activé par défaut

        // Générer et envoyer le code de validation
        String validationCode = genererCode();
        user.setValidationCode(validationCode);

        User savedUser = userRepository.save(user);

        // Envoyer le code par SMS
        String message = "Votre code de validation est: " + validationCode;

        envoyerEmail(email ,message);

        return savedUser;
    }

    // === NOUVELLES FONCTIONS POUR VALIDATION SMS ===

    public String validateAccount(String code, String phoneOrEmail) {
        // Chercher l'utilisateur par email ou numéro de téléphone
        Optional<User> userOpt = userRepository.findByEmailOrPhoneNumber(phoneOrEmail);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        User user = userOpt.get();

        // Vérifier si le code correspond
        if (user.getValidationCode() == null || !user.getValidationCode().equals(code)) {
            throw new RuntimeException("Code de validation incorrect");
        }

        // Vérifier si le code a expiré (optionnel pour plus tard)
        // if (isCodeExpired(user.getCodeGeneratedAt())) {
        //     throw new RuntimeException("Le code a expiré");
        // }

        // Activer le compte
        user.activateAccount();
        userRepository.save(user);

        return "Compte validé avec succès! Vous pouvez maintenant vous connecter.";
    }

    public String resendValidationCode(String phoneOrEmail) {
        Optional<User> userOpt = userRepository.findByEmailOrPhoneNumber(phoneOrEmail);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        User user = userOpt.get();

        // Vérifier si le compte est déjà activé
        if (user.getIsActive()) {
            throw new RuntimeException("Le compte est déjà activé");
        }

        // Générer un nouveau code
        String newCode = genererCode();
        user.setValidationCode(newCode);
        userRepository.save(user);

        // Envoyer le nouveau code par SMS
        String message = "Votre nouveau code de validation est: " + newCode;

        envoyerEmail(user.getEmail() ,message);

        return "Nouveau code envoyé avec succès";
    }

    // Méthode pour vérifier si un compte est activé
    public boolean isAccountActive(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier)
                .map(User::getIsActive)
                .orElse(false);
    }

    // === FONCTIONS EXISTANTES (avec modifications mineures) ===

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

    public String getDonneeVisible(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        return user.getDonneeVisible();
    }

    public User updateDonneeVisible(Long userId, String donneeVisible) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        user.setDonneeVisible(donneeVisible);
        return userRepository.save(user);
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

            // ===== ORDRE DES SUPPRESSIONS (Strict respect des contraintes d'intégrité) =====

            // 0. Supprimer les relations Many-to-Many entre Favoris et Genres
            // C'est cette étape qui causait l'erreur SQLState: 23503
            Query deleteMusicGenresLiaisons = entityManager.createNativeQuery(
                    "DELETE FROM musicfavoris_genres WHERE music_favoris_id IN " +
                            "(SELECT id FROM music_favoris WHERE user_id = ?)"
            );
            deleteMusicGenresLiaisons.setParameter(1, userId);
            int genresLiaisonsDeleted = deleteMusicGenresLiaisons.executeUpdate();
            System.out.println("0. Liaisons genres supprimées : " + genresLiaisonsDeleted);

            // 1. Supprimer les favoris musicaux (table: music_favoris)
            Query deleteMusicFavoris = entityManager.createNativeQuery(
                    "DELETE FROM music_favoris WHERE user_id = ?"
            );
            deleteMusicFavoris.setParameter(1, userId);
            int musicFavorisDeleted = deleteMusicFavoris.executeUpdate();
            System.out.println("1. Favoris musicaux supprimés : " + musicFavorisDeleted);

            // 2. Supprimer les commentaires écrits par l'utilisateur (table: commentaires)
            Query deleteCommentairesByUser = entityManager.createNativeQuery(
                    "DELETE FROM commentaires WHERE user_id = ?"
            );
            deleteCommentairesByUser.setParameter(1, userId);
            int commentairesByUserDeleted = deleteCommentairesByUser.executeUpdate();
            System.out.println("2. Commentaires écrits par l'user supprimés : " + commentairesByUserDeleted);

            // 3. Supprimer les commentaires sur les messages de l'utilisateur
            Query deleteCommentairesOnUserMessages = entityManager.createNativeQuery(
                    "DELETE FROM commentaires WHERE message_id IN " +
                            "(SELECT id FROM messages WHERE user_id = ?)"
            );
            deleteCommentairesOnUserMessages.setParameter(1, userId);
            int commentairesOnMessagesDeleted = deleteCommentairesOnUserMessages.executeUpdate();
            System.out.println("3. Commentaires sur les messages de l'user supprimés : " + commentairesOnMessagesDeleted);

            // 4. Supprimer les messages publics de l'utilisateur (table: messages)
            Query deleteMessages = entityManager.createNativeQuery(
                    "DELETE FROM messages WHERE user_id = ?"
            );
            deleteMessages.setParameter(1, userId);
            int messagesDeleted = deleteMessages.executeUpdate();
            System.out.println("4. Messages publics supprimés : " + messagesDeleted);

            // 5. Supprimer les messages privés (contenus dans les conversations)
            Query deleteMessagesPrives = entityManager.createNativeQuery(
                    "DELETE FROM messages_prives WHERE conversation_id IN " +
                            "(SELECT id FROM conversations WHERE user1_id = ? OR user2_id = ?)"
            );
            deleteMessagesPrives.setParameter(1, userId);
            deleteMessagesPrives.setParameter(2, userId);
            int messagesPrivesDeleted = deleteMessagesPrives.executeUpdate();
            System.out.println("5. Messages privés supprimés : " + messagesPrivesDeleted);

            // 6. Supprimer les conversations impliquant l'utilisateur (table: conversations)
            Query deleteConversations = entityManager.createNativeQuery(
                    "DELETE FROM conversations WHERE user1_id = ? OR user2_id = ?"
            );
            deleteConversations.setParameter(1, userId);
            deleteConversations.setParameter(2, userId);
            int conversationsDeleted = deleteConversations.executeUpdate();
            System.out.println("6. Conversations supprimées : " + conversationsDeleted);

            // 7. Supprimer l'utilisateur lui-même (table: users)
            Query deleteUser = entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = ?"
            );
            deleteUser.setParameter(1, userId);
            int userDeleted = deleteUser.executeUpdate();
            System.out.println("7. Utilisateur supprimé de la table users : " + userDeleted);

            System.out.println("===== SUPPRESSION TOTALE RÉUSSIE POUR L'ID " + userId + " =====");

        } catch (Exception e) {
            // Grâce au @Transactional, si une étape échoue, rien n'est supprimé
            throw new RuntimeException("Erreur critique lors de la suppression SQL : " + e.getMessage(), e);
        }
    }

    public User updateCurrentUser(Long userId, String username, String email,
                                  String currentPassword, String newPassword,
                                  boolean removePhoto, String photoUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("Ce nom d'utilisateur est déjà pris");
            }
            user.setUsername(username.trim());
        }

        if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            user.setEmail(email.trim());
        }

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

        if (removePhoto) {
            user.setPhotoUrl(null);
        } else if (photoUrl != null) {
            user.setPhotoUrl(photoUrl);
        }

        return userRepository.save(user);
    }

    public User updateListeReseauxCurrentUser(Long userId, String listeReseauxSociaux) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        user.setListeReseaux(listeReseauxSociaux);
        return userRepository.save(user);
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameOrEmail(identifier.trim());
    }

    private boolean isCodeExpired(Date codeGeneratedAt) {
        if (codeGeneratedAt == null) return true;

        long codeAge = new Date().getTime() - codeGeneratedAt.getTime();
        long fifteenMinutes = 15 * 60 * 1000; // 15 minutes en millisecondes

        return codeAge > fifteenMinutes;
    }
}