package main.controlleurs;

import main.modeles.User;
import main.repository.UserRepository;
import main.security.JwtUtil;
import main.services.MusicFavorisService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static main.divers_services.github.GitHubFileUploader.uploadFileGitHub;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicFavorisService musicFavorisService;



    // === ENDPOINTS D'AUTHENTIFICATION AVEC SMS ===

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("file") MultipartFile file) {

        try {
            // Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier photo est requis"));
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
            }

            // Validation du numéro de téléphone
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de téléphone est requis"));
            }

            // Upload de la photo
            Path tempFile = Files.createTempFile("upload_", "_" + originalName);
            try {
                file.transferTo(tempFile);
                String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "photos");

                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Erreur lors de l'upload de la photo vers GitHub"));
                }

                // Création de l'utilisateur avec le numéro de téléphone
                User user = userService.createUser(username, email, password, phoneNumber, uploadedFileUrl);

                // Réponse
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Inscription réussie! Un code de validation a été envoyé par SMS.");
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("phoneNumber", user.getPhoneNumber());
                response.put("role", user.getRole());
                response.put("photoUrl", uploadedFileUrl);
                response.put("createdAt", user.getCreatedAt().toString());
                response.put("isActive", user.getIsActive());
                response.put("note", "Veuillez valider votre compte avec le code reçu par SMS avant de vous connecter.");

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'inscription : " + e.getMessage()));
        }
    }

    @PostMapping("/register-admin")
    public ResponseEntity<Map<String, Object>> registerAdmin(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier photo est requis"));
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
            }

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de téléphone est requis"));
            }

            Path tempFile = Files.createTempFile("upload_", "_" + originalName);
            try {
                file.transferTo(tempFile);
                String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "photos");

                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Erreur lors de l'upload de la photo vers GitHub"));
                }

                User user = userService.createUserWithRole(username, email, password, phoneNumber, "ADMIN", uploadedFileUrl);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Administrateur créé avec succès! Un code de validation a été envoyé par SMS.");
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("phoneNumber", user.getPhoneNumber());
                response.put("role", user.getRole());
                response.put("photoUrl", uploadedFileUrl);
                response.put("isActive", user.getIsActive());

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création de l'administrateur : " + e.getMessage()));
        }
    }

    // === NOUVEAUX ENDPOINTS POUR VALIDATION SMS ===

    @PostMapping("/validate-account")
    public ResponseEntity<Map<String, Object>> validateAccount(
            @RequestBody Map<String, String> validationData) {
        try {
            String code = validationData.get("code");
            String phoneOrEmail = validationData.get("phoneOrEmail");

            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le code de validation est requis"));
            }
            if (phoneOrEmail == null || phoneOrEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'email ou le numéro de téléphone est requis"));
            }

            String message = userService.validateAccount(code.trim(), phoneOrEmail.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("phoneOrEmail", phoneOrEmail);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur lors de la validation du compte"));
        }
    }

    @PostMapping("/resend-validation-code")
    public ResponseEntity<Map<String, Object>> resendValidationCode(
            @RequestBody Map<String, String> requestData) {
        try {
            String phoneOrEmail = requestData.get("phoneOrEmail");

            if (phoneOrEmail == null || phoneOrEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'email ou le numéro de téléphone est requis"));
            }

            String message = userService.resendValidationCode(phoneOrEmail.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("phoneOrEmail", phoneOrEmail);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur lors de l'envoi du code"));
        }
    }

    @GetMapping("/check-account-status")
    public ResponseEntity<Map<String, Object>> checkAccountStatus(
            @RequestParam("identifier") String identifier) {
        try {
            if (identifier == null || identifier.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Identifiant requis"));
            }

            boolean isActive = userService.isAccountActive(identifier.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("identifier", identifier);
            response.put("isActive", isActive);
            response.put("message", isActive ? "Compte activé" : "Compte en attente de validation");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur lors de la vérification du compte"));
        }
    }

    // === ENDPOINTS EXISTANTS (conservés avec modifications mineures) ===

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("photoUrl", user.getPhotoUrl());
            response.put("sonEntreeURL", user.getSonEntreeURL());
            response.put("donneeVisible", user.getDonneeVisible());
            response.put("isActive", user.getIsActive());
            response.put("listeReseaux", user.getListeReseaux());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération du profil"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("photoUrl", user.getPhotoUrl());
            response.put("sonEntreeURL", user.getSonEntreeURL());
            response.put("donneeVisible", user.getDonneeVisible());
            response.put("isActive", user.getIsActive());
            response.put("listeReseaux", user.getListeReseaux());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération de l'utilisateur"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();

            List<Map<String, Object>> usersList = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("phoneNumber", user.getPhoneNumber());
                        userMap.put("role", user.getRole());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL());
                        userMap.put("donneeVisible", user.getDonneeVisible());
                        userMap.put("isActive", user.getIsActive());
                        userMap.put("createdAt", user.getCreatedAt());
                        userMap.put("listeReseaux", user.getListeReseaux());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des utilisateurs"));
        }
    }

    @PutMapping("/me/update")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "currentPassword", required = false) String currentPassword, // Ancien mot de passe
            @RequestParam(value = "newPassword", required = false) String newPassword, // Nouveau mot de passe
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            String currentUsername = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Liste des modifications effectuées
            List<String> modifications = new ArrayList<>();

            // 1. Mise à jour des champs textuels (si fournis et différents)
            if (username != null && !username.trim().isEmpty()
                    && !username.equals(currentUser.getUsername())) {
                currentUser.setUsername(username);
                modifications.add("username");
            }

            if (email != null && !email.trim().isEmpty()
                    && !email.equals(currentUser.getEmail())) {
                currentUser.setEmail(email);
                modifications.add("email");
            }

            if (phoneNumber != null && !phoneNumber.trim().isEmpty()
                    && !phoneNumber.equals(currentUser.getPhoneNumber())) {
                currentUser.setPhoneNumber(phoneNumber);
                modifications.add("phoneNumber");
            }

            // 2. Mise à jour du mot de passe (si nouveau mot de passe fourni)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Vérification de sécurité : l'ancien mot de passe est obligatoire
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Le mot de passe actuel est requis pour modifier le mot de passe"
                    ));
                }

                // Vérifier que l'ancien mot de passe est correct
                if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                    return ResponseEntity.status(401).body(Map.of(
                            "error", "Mot de passe actuel incorrect"
                    ));
                }

                // Vérifier que le nouveau mot de passe est différent de l'ancien
                if (passwordEncoder.matches(newPassword, currentUser.getPassword())) {
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Le nouveau mot de passe doit être différent de l'actuel"
                    ));
                }

                // Vérifier la complexité du mot de passe (optionnel mais recommandé)
                if (newPassword.length() < 4) {
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Le mot de passe doit contenir au moins 4 caractères"
                    ));
                }

                // Encoder et sauvegarder le nouveau mot de passe
                String encodedPassword = passwordEncoder.encode(newPassword);
                currentUser.setPassword(encodedPassword);
                modifications.add("password");
            } else if (currentPassword != null && !currentPassword.trim().isEmpty()) {
                // Cas où currentPassword est fourni sans newPassword (erreur)
                return ResponseEntity.status(400).body(Map.of(
                        "error", "Le nouveau mot de passe est requis lorsque vous fournissez le mot de passe actuel"
                ));
            }

            // 3. Gestion de la photo
            if (file != null && !file.isEmpty()) {
                // Vérifier la taille du fichier (max 5MB)
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "La photo ne doit pas dépasser 5 Mo"
                    ));
                }

                // Vérifier le type de fichier
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(400).body(Map.of(
                            "error", "Le fichier doit être une image"
                    ));
                }

                String originalName = file.getOriginalFilename();
                Path tempFile = Files.createTempFile("update_", "_" + originalName);
                try {
                    file.transferTo(tempFile);
                    String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "photos");
                    if (uploadedFileUrl == null) {
                        return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de l'upload sur GitHub"));
                    }
                    currentUser.setPhotoUrl(uploadedFileUrl);
                    modifications.add("photo");
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            // 4. Vérifier qu'au moins une modification a été demandée
            if (modifications.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "Aucune modification à effectuer"
                ));
            }

            // 5. Sauvegarde
            User updatedUser = userService.updateUser(currentUser);

            // 6. Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mise à jour réussie");
            response.put("modifications", modifications);
            response.put("timestamp", new Date());
            response.put("user", Map.of(
                    "id", updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "email", updatedUser.getEmail(),
                    "phoneNumber", updatedUser.getPhoneNumber(),
                    "photoUrl", updatedUser.getPhotoUrl()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // Pour le débogage
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur lors de la mise à jour : " + e.getMessage()
            ));
        }
    }




    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);

            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalide ou expiré"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("message", "Bienvenue dans ton profil sécurisé !");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Erreur d'authentification"));
        }
    }



    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        try {
            // Vérifier que l'utilisateur connecté existe
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Utilisateur connecté non trouvé"));

            // Vérifier que l'utilisateur cible existe
            if (!userRepository.existsById(userId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Utilisateur non trouvé avec l'ID: " + userId));
            }

            // SUPPRESSION AUTORISÉE POUR TOUS LES UTILISATEURS
            // N'importe quel utilisateur connecté peut supprimer n'importe quel autre utilisateur
            userService.deleteUserWithAllData(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Utilisateur supprimé avec succès");
            response.put("deletedUserId", userId);
            response.put("deletedBy", currentUsername);
            response.put("timestamp", new Date().toString());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteMyAccount() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Long userId = currentUser.getId();

            // Supprimer l'utilisateur et toutes ses données
            userService.deleteUserWithAllData(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Votre compte a été supprimé avec succès");
            response.put("deletedUserId", userId);
            response.put("username", username);
            response.put("timestamp", new Date().toString());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression du compte : " + e.getMessage()));
        }
    }

    // === ENDPOINTS POUR LE SON D'ENTRÉE ===

    @PutMapping("/me/son-entree")
    public ResponseEntity<Map<String, Object>> updateSonEntree(@RequestParam("audio") MultipartFile audioFile) {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier audio est requis"));
            }

            if (!audioFile.getContentType().startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier doit être un fichier audio"));
            }

            if (audioFile.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier est trop volumineux (max 10MB)"));
            }

            String originalName = audioFile.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
            }

            Path tempFile = Files.createTempFile("audio_upload_", "_" + originalName);

            try {
                audioFile.transferTo(tempFile);
                String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "sons");

                if (uploadedFileUrl == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Erreur lors de l'upload du son vers GitHub"));
                }

                currentUser.setSonEntreeURL(uploadedFileUrl);
                userService.updateUser(currentUser);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Son d'entrée mis à jour avec succès");
                response.put("sonEntreeURL", uploadedFileUrl);
                response.put("username", username);

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour du son: " + e.getMessage()));
        }
    }

    // === ENDPOINTS D'ADMINISTRATION ===

    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            long totalUsers = userService.getAllUsers().size();
            long adminUsers = userService.getAllUsers().stream()
                    .filter(user -> "ADMIN".equals(user.getRole()))
                    .count();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("message", "Bienvenue dans le tableau de bord administrateur");
            dashboard.put("connectedAdmin", username);
            dashboard.put("totalUsers", totalUsers);
            dashboard.put("adminUsers", adminUsers);

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'accès au tableau de bord"));
        }
    }

    @GetMapping("/admin/users-details")
    public ResponseEntity<Map<String, Object>> getAllUsersWithDetails() {
        try {
            List<User> users = userService.getAllUsers();
            users.forEach(user -> user.setPassword("*****"));

            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("count", users.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des détails utilisateurs"));
        }
    }



    @GetMapping("/search_contains")
    public ResponseEntity<Map<String, Object>> searchUsersByUsername(
            @RequestParam("username") String usernameQuery) {
        try {
            if (usernameQuery == null || usernameQuery.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Le paramètre 'username' est requis"));
            }

            List<User> users = userService.searchUsersByUsername(usernameQuery);

            List<Map<String, Object>> usersList = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("role", user.getRole());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("sonEntreeURL", user.getSonEntreeURL());
                        userMap.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
                        userMap.put("createdAt", user.getCreatedAt());
                        userMap.put("listeReseaux", user.getListeReseaux());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());
            response.put("searchQuery", usernameQuery);
            response.put("message", usersList.isEmpty() ?
                    "Aucun utilisateur trouvé pour '" + usernameQuery + "'" :
                    usersList.size() + " utilisateur(s) trouvé(s)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la recherche: " + e.getMessage()));
        }
    }



    @PutMapping("/me/updateListeReseaux")
    public ResponseEntity<Map<String, Object>> updateListeReseauxCurrentUser(
            @RequestParam(value = "listeReseaux", required = false) String listeReseaux) {

        try {
            // 1. Récupérer l'utilisateur connecté
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 4. Appeler le service
            User updatedUser = userService.updateListeReseauxCurrentUser(
                    currentUser.getId(),
                    listeReseaux
            );

            // 5. Retourner la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("user", Map.of(
                    "id", updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "email", updatedUser.getEmail(),
                    "role", updatedUser.getRole(),
                    "photoUrl", updatedUser.getPhotoUrl(),
                    "createdAt", updatedUser.getCreatedAt(),
                    "listeReseaux", updatedUser.getListeReseaux()
            ));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    @GetMapping("/me/liste-reseaux")
    public ResponseEntity<Map<String, Object>> getListeReseaux() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "listeReseaux", currentUser.getListeReseaux()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur"));
        }
    }

    // === NOUVEAUX ENDPOINTS POUR DONNEEVISIBLE (optionnels mais recommandés) ===

    @GetMapping("/me/donnee-visible")
    public ResponseEntity<Map<String, Object>> getDonneeVisible() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "donneeVisible", currentUser.getDonneeVisible()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur"));
        }
    }

    @PutMapping("/me/donnee-visible")
    public ResponseEntity<Map<String, Object>> updateDonneeVisible(
            @RequestParam(value = "donneeVisible", required = false) String donneeVisible) {

        try {
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Utiliser la nouvelle méthode du service
            User updatedUser = userService.updateDonneeVisible(currentUser.getId(), donneeVisible);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Donnée visible mise à jour avec succès");
            response.put("donneeVisible", updatedUser.getDonneeVisible());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }
}