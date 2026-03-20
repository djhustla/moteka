package main.controlleurs;

import main.API_Music.SpotifyController;
import main.modeles.MusicFavoris;
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

    @Autowired
    private SpotifyController spotifyController; // Assure-toi que le nom est correct




    // === ENDPOINTS D'AUTHENTIFICATION AVEC SMS ===

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "phoneNumber") String phoneNumber,
            @RequestParam(value = "file", required = false) MultipartFile file


    ) {

        try {


            // Validation du numéro de téléphone
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de téléphone est requis"));
            }



            String photoUrl = "https://raw.githubusercontent.com/djhustla/music-storage/main/photos/photo_par_defaut.jpg";

            // Si un fichier est fourni et n'est pas vide
            if (file != null && !file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
                }

                Path tempFile = Files.createTempFile("upload_", "_" + originalName);
                try {
                    file.transferTo(tempFile);
                    String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "photos");

                    if (uploadedFileUrl == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Erreur lors de l'upload de la photo vers GitHub"));
                    }

                    photoUrl = uploadedFileUrl;

                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            // Création de l'utilisateur avec le numéro de téléphone

            User user = userService.createUser(username, email, password, phoneNumber, photoUrl);

            // Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Inscription réussie! Un code de validation a été envoyé par SMS.");
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());


            response.put("phoneNumber", user.getPhoneNumber());


            response.put("role", user.getRole());
            response.put("photoUrl", photoUrl);
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("isActive", user.getIsActive());
            response.put("note", "Veuillez valider votre compte avec le code reçu par SMS avant de vous connecter.");

            return ResponseEntity.ok(response);

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
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de téléphone est requis"));
            }

            String photoUrl = "https://raw.githubusercontent.com/djhustla/music-storage/main/photos/photo_par_defaut.jpg";

            if (file != null && !file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Nom de fichier invalide"));
                }

                Path tempFile = Files.createTempFile("upload_", "_" + originalName);
                try {
                    file.transferTo(tempFile);
                    String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "photos");

                    if (uploadedFileUrl == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Erreur lors de l'upload de la photo vers GitHub"));
                    }

                    photoUrl = uploadedFileUrl;

                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            User user = userService.createUserWithRole(username, email, password, phoneNumber, "ADMIN", photoUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Administrateur créé avec succès! Un code de validation a été envoyé par SMS.");
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("role", user.getRole());
            response.put("photoUrl", photoUrl);
            response.put("isActive", user.getIsActive());

            return ResponseEntity.ok(response);

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

    /**
     * Active un compte utilisateur par userId (sans auth requise)
     * PUT /api/users/{userId}/activate
     */
    @PutMapping("/{userId}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            user.activateAccount(); // setIsActive(true) + clear validationCode
            userService.updateUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success",  true);
            response.put("message",  "Compte activé avec succès");
            response.put("userId",   userId);
            response.put("username", user.getUsername());
            response.put("isActive", user.getIsActive());

            System.out.println("Compte activé : " + user.getUsername() + " (ID: " + userId + ")");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * Met à jour la photoUrl d'un utilisateur par userId (URL string directe, pas de fichier)
     * PUT /api/users/{userId}/photo-url?photoUrl=https://...
     */
    @PutMapping("/{userId}/photo-url")
    public ResponseEntity<Map<String, Object>> updatePhotoUrl(
            @PathVariable Long userId,
            @RequestParam String photoUrl) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            if (photoUrl == null || photoUrl.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "photoUrl ne peut pas être vide"));
            }

            user.setPhotoUrl(photoUrl);
            userService.updateUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success",  true);
            response.put("message",  "Photo mise à jour avec succès");
            response.put("userId",   userId);
            response.put("username", user.getUsername());
            response.put("photoUrl", photoUrl);

            System.out.println("Photo mise à jour : " + user.getUsername() + " → " + photoUrl);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
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

            //
            response.put("lien_spotify", user.getLienSpotify());

            //

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

    @PutMapping("/youtube/{userId}")
    public ResponseEntity<User> updateYoutubeLink(
            @PathVariable Long userId,
            @RequestParam String youtubeLink) {
        User updatedUser = userService.updateYoutubeLink(userId, youtubeLink);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/spotify/{userId}")
    public ResponseEntity<User> updateSpotifyLink(
            @PathVariable Long userId,
            @RequestParam String spotifyLink) {
        User updatedUser = userService.updateSpotifyLink(userId, spotifyLink);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        try {
            User updatedUser = userService.updateRole(userId, role);
            return ResponseEntity.ok(Map.of(
                    "success",  true,
                    "userId",   updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "role",     updatedUser.getRole()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> getRole(@PathVariable Long userId) {
        try {
            String role = userService.getRole(userId);
            return ResponseEntity.ok(Map.of(
                    "success",  true,
                    "userId",   userId,
                    "role",     role
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Met à jour la listeReseaux d'un utilisateur par userId (sans auth requise)
     * PUT /api/users/{userId}/listeReseaux?listeReseaux=...
     */
    @PutMapping("/{userId}/listeReseaux")
    public ResponseEntity<Map<String, Object>> updateListeReseaux(
            @PathVariable Long userId,
            @RequestParam(value = "listeReseaux", required = false) String listeReseaux) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            user.setListeReseaux(listeReseaux);
            userService.updateUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success",      true);
            response.put("message",      "Liste réseaux mise à jour avec succès");
            response.put("userId",       userId);
            response.put("username",     user.getUsername());
            response.put("listeReseaux", user.getListeReseaux());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    @GetMapping("/artistes")
    public ResponseEntity<Map<String, Object>> getAllArtistes() {
        try {
            List<User> artistes = userService.getUsersByRole("ARTISTE");

            List<Map<String, Object>> artistesList = artistes.stream()
                    .map(user -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id",          user.getId());
                        m.put("username",    user.getUsername());
                        m.put("email",       user.getEmail());
                        m.put("photoUrl",    user.getPhotoUrl());
                        m.put("lienSpotify", user.getLienSpotify());
                        m.put("lienYoutube", user.getLienYoutube());
                        m.put("isActive",    user.getIsActive());
                        m.put("createdAt",   user.getCreatedAt());
                        m.put("genreList",   user.getGenreList());
                        m.put("pays",        user.getPays());
                        return m;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "artistes", artistesList,
                    "count",    artistesList.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des artistes"));
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

    /**
     * Récupère le lien YouTube d'un utilisateur
     * GET /api/users/youtube/{userId}
     */
    @GetMapping("/youtube/{userId}")
    public ResponseEntity<Map<String, Object>> getYoutubeLink(@PathVariable Long userId) {
        try {
            String youtubeLink = userService.getYoutubeLink(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("youtubeLink", youtubeLink != null ? youtubeLink : "");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur serveur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Récupère le lien Spotify d'un utilisateur
     * GET /api/users/spotify/{userId}
     */
    @GetMapping("/spotify/{userId}")
    public ResponseEntity<Map<String, Object>> getSpotifyLink(@PathVariable Long userId) {
        try {
            String spotifyLink = userService.getSpotifyLink(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("spotifyLink", spotifyLink != null ? spotifyLink : "");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur serveur: " + e.getMessage()
                    ));
        }
    }


    /**
     * Cherche un utilisateur par son URL Spotify
     * GET /api/users/by-spotify?url=https://open.spotify.com/artist/...
     */
    @GetMapping("/by-spotify")
    public ResponseEntity<Map<String, Object>> getUserBySpotifyUrl(
            @RequestParam("url") String spotifyUrl) {
        try {
            if (spotifyUrl == null || spotifyUrl.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Paramètre 'url' requis"));
            }

            Optional<User> userOpt = userService.findBySpotifyUrl(spotifyUrl.trim());

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "error", "Aucun utilisateur trouvé avec ce lien Spotify",
                                "spotifyUrl", spotifyUrl
                        ));
            }

            User user = userOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("success",      true);
            response.put("id",           user.getId());
            response.put("username",     user.getUsername());
            response.put("email",        user.getEmail());
            response.put("photoUrl",     user.getPhotoUrl());
            response.put("lienSpotify",  user.getLienSpotify());
            response.put("lienYoutube",  user.getLienYoutube());
            response.put("listeReseaux", user.getListeReseaux());
            response.put("role",         user.getRole());
            response.put("isActive",     user.getIsActive());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════
    // PAYS
    // ═══════════════════════════════════════════════════

    /**
     * GET /api/users/{userId}/pays
     */
    @GetMapping("/{userId}/pays")
    public ResponseEntity<Map<String, Object>> getPays(@PathVariable Long userId) {
        try {
            String pays = userService.getPays(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId",  userId,
                    "pays",    pays != null ? pays : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/users/{userId}/pays?pays=France
     */
    @PutMapping("/{userId}/pays")
    public ResponseEntity<Map<String, Object>> updatePays(
            @PathVariable Long userId,
            @RequestParam String pays) {
        try {
            User user = userService.updatePays(userId, pays);
            return ResponseEntity.ok(Map.of(
                    "success",  true,
                    "userId",   userId,
                    "username", user.getUsername(),
                    "pays",     user.getPays() != null ? user.getPays() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════
    // GENRES
    // ═══════════════════════════════════════════════════

    /**
     * GET /api/users/{userId}/genres
     */
    @GetMapping("/{userId}/genres")
    public ResponseEntity<Map<String, Object>> getGenres(@PathVariable Long userId) {
        try {
            List<String> genres = userService.getGenreList(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId",  userId,
                    "genres",  genres
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/users/{userId}/genres
     * Body (JSON) : ["rap", "afrobeat", "drill"]
     */
    @PutMapping("/{userId}/genres")
    public ResponseEntity<Map<String, Object>> updateGenres(
            @PathVariable Long userId,
            @RequestBody List<String> genres) {
        try {
            User user = userService.updateGenreList(userId, genres);
            return ResponseEntity.ok(Map.of(
                    "success",  true,
                    "userId",   userId,
                    "username", user.getUsername(),
                    "genres",   user.getGenreList()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }


    /**
     * POST /api/users/{userId}/sync-genres-spotify
     * Pour un seul artiste : récupère ses genres via Spotify et les encode en BDD.
     */
    @PostMapping("/{userId}/sync-genres-spotify")
    public ResponseEntity<Map<String, Object>> syncGenresSpotify(@PathVariable Long userId) {
        try {
            // 1. Récupérer l'artiste
            User artiste = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            // 2. Vérifier qu'il a un lien Spotify
            String spotifyUrl = artiste.getLienSpotify();
            if (spotifyUrl == null || spotifyUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error",   "Cet artiste n'a pas de lien Spotify"
                ));
            }

            // 3. Appel GET /api/spotify/artiste/genres
            ResponseEntity<Map<String, Object>> genresRes =
                    spotifyController.getArtisteGenres(spotifyUrl);

            if (!genresRes.getStatusCode().is2xxSuccessful()
                    || genresRes.getBody() == null
                    || !Boolean.TRUE.equals(genresRes.getBody().get("success"))) {
                return ResponseEntity.status(500).body(Map.of(
                        "success", false,
                        "error",   "Erreur Spotify : " + (genresRes.getBody() != null ? genresRes.getBody().get("error") : "null")
                ));
            }

            // 4. Extraire la liste de genres
            Object rawGenres = genresRes.getBody().get("genres");
            List<String> genres = new ArrayList<>();
            if (rawGenres instanceof List<?>) {
                for (Object g : (List<?>) rawGenres) {
                    if (g instanceof String) genres.add((String) g);
                }
            }

            // 5. PUT genres en BDD
            userService.updateGenreList(userId, genres);

            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "userId",     userId,
                    "username",   artiste.getUsername(),
                    "spotifyUrl", spotifyUrl,
                    "genres",     genres,
                    "total",      genres.size()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }



    /**
     * Récupère les favoris musicaux d'un utilisateur spécifique par son ID
     * GET /api/users/{userId}/music-favoris
     */
    @GetMapping("/{userId}/music-favoris")
    public ResponseEntity<?> getUserMusicFavorisById(@PathVariable Long userId) {
        try {
            // 1. Vérifier que l'utilisateur existe
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            // 2. Récupérer ses favoris musicaux
            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(user.getId());

            // 3. Retourner la réponse
            return ResponseEntity.ok(favoris);

        } catch (RuntimeException e) {
            // Erreur métier (utilisateur non trouvé)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            // Erreur serveur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la récupération des favoris : " + e.getMessage()
                    ));
        }
    }

    /**
     * POST /api/users/{userId}/auto-tag-preferences
     * Pour un artiste spécifique : analyse ses genres Spotify et lui attribue automatiquement
     * des préférences musicales (cote=9) basées sur un mapping de mots-clés.
     *
     * @param userId L'ID de l'artiste à traiter
     * @return Rapport indiquant les genres attribués
     */
    @PostMapping("/{userId}/auto-tag-preferences")
    public ResponseEntity<Map<String, Object>> autoTagArtistPreferencesForUser(@PathVariable Long userId) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Récupérer l'artiste spécifique
            User artiste = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Artiste non trouvé avec l'ID: " + userId));

            // 2. Vérifier que c'est bien un artiste
            if (!"ARTISTE".equals(artiste.getRole())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "L'utilisateur avec l'ID " + userId + " n'a pas le rôle ARTISTE"
                ));
            }

            // 3. Récupérer ses genres Spotify
            List<String> spotifyGenres = artiste.getGenreList();
            if (spotifyGenres == null || spotifyGenres.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Aucun genre Spotify trouvé pour cet artiste",
                        "userId", userId,
                        "username", artiste.getUsername(),
                        "genres_attribues", Collections.emptyList()
                ));
            }

            // 4. Mapper les genres Spotify vers des IDs de genres internes
            Set<Long> internalGenreIdsToAssign = new HashSet<>();

            for (String g : spotifyGenres) {
                String genre = g.toLowerCase().trim();

                // Mapping précis selon les instructions
                if (matches(genre, "afrobeat", "afrobeats", "afropiano", "afropop", "afroswing", "amapiano", "azonto", "bongo flava", "afro r&b")) {
                    internalGenreIdsToAssign.add(35L); // afrobeat & amapiano
                }

                if (matches(genre, "french pop", "french r&b", "french rap", "pop urbaine")) {
                    internalGenreIdsToAssign.add(8L); // Pop urbaine
                }

                if (matches(genre, "g-funk", "gangster rap", "hardcore hip hop", "hip hop", "hiplife", "hyphy", "east coast hip hop", "new jack swing", "new orleans bounce", "old school hip hop", "southern hip hop", "west coast hip hop", "crunk", "bounce")) {
                    internalGenreIdsToAssign.add(37L); // Oldschool Rnb Hiphop
                }

                if (matches(genre, "brazilian funk", "chilean mambo", "chilean trap", "latin", "latin pop", "funk carioca", "reggaeton")) {
                    internalGenreIdsToAssign.add(9L); // Latino urbain
                }

                if (matches(genre, "roots reggae", "dancehall", "shatta")) {
                    internalGenreIdsToAssign.add(34L); // Shatta Dancehall
                }

                if (matches(genre, "drill", "trap soul")) {
                    internalGenreIdsToAssign.add(36L); // Trap & Drill
                }

                if (matches(genre, "k-ballad", "k-pop", "k-rap")) {
                    internalGenreIdsToAssign.add(1L); // Kpop
                }

                if (matches(genre, "kizomba", "kompa", "zouk")) {
                    internalGenreIdsToAssign.add(7L); // Zouk, Kizomba
                }

                if (matches(genre, "ndombolo", "rumba congolaise")) {
                    internalGenreIdsToAssign.add(40L); // Musique congolaise
                }

                if (matches(genre, "coupé décalé")) {
                    internalGenreIdsToAssign.add(41L); // Musique ivoirienne
                }
            }

            // 5. Si aucune correspondance trouvée
            if (internalGenreIdsToAssign.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Aucune correspondance trouvée pour les genres de cet artiste",
                        "userId", userId,
                        "username", artiste.getUsername(),
                        "spotifyGenres", spotifyGenres,
                        "genres_attribues", Collections.emptyList()
                ));
            }

            // 6. Créer la préférence musicale
            List<Long> genreIdsList = new ArrayList<>(internalGenreIdsToAssign);
            musicFavorisService.createMusicFavoris(artiste.getId(), 9, genreIdsList);

            // 7. Retourner le rapport
            result.put("success", true);
            result.put("message", "Tagging automatique effectué avec succès");
            result.put("userId", userId);
            result.put("username", artiste.getUsername());
            result.put("spotifyGenres", spotifyGenres);
            result.put("genres_attribues", internalGenreIdsToAssign);
            result.put("cotePreference", 9);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors du tagging automatique: " + e.getMessage()
                    ));
        }
    }

    ////////////---------------///////////////// fonction SUPER ADMIN

    @PostMapping("/import-bulk-artists")
    public ResponseEntity<Map<String, Object>> importBulkArtists(@RequestBody List<String> artistLines) {
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;

        for (String line : artistLines) {
            Map<String, Object> status = new HashMap<>();
            try {
                // 1. PARSING (Nom|URL_Spotify|Socials)
                String[] parts = line.split("\\|");
                if (parts.length < 2) throw new RuntimeException("Format de ligne invalide");

                String artistName = parts[0].trim();
                String spotifyUrl = parts[1].trim();
                String socialLinks = (parts.length > 2) ? parts[2].trim() : "";

                // 2. RÉCUPÉRATION DE LA PHOTO (via endpoint /api/spotify/artiste/photo)
                String realPhotoUrl = "https://raw.githubusercontent.com/djhustla/music-storage/main/photos/photo_par_defaut.jpg";
                try {
                    ResponseEntity<Map<String, Object>> searchRes = spotifyController.searchArtiste(artistName);
                    if (searchRes.getStatusCode().is2xxSuccessful() && searchRes.getBody() != null) {
                        String foundUrl = (String) searchRes.getBody().get("spotifyUrl");
                        if (foundUrl != null && !foundUrl.isBlank()) {
                            spotifyUrl = foundUrl;
                        }
                    }

                    // FALLBACK 2 : récupérer la photo avec le nouvel URL Spotify
                    ResponseEntity<Map<String, Object>> photoRes2 = spotifyController.getArtistPhoto(spotifyUrl);
                    if (photoRes2.getStatusCode().is2xxSuccessful() && photoRes2.getBody() != null) {
                        String foundPhoto = (String) photoRes2.getBody().get("photoUrl");
                        if (foundPhoto != null && !foundPhoto.isBlank()) {
                            realPhotoUrl = foundPhoto;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Phase critique échouée pour " + artistName + " → tentative de recherche par nom...");
                }

                // 3. GÉNÉRATION DES CREDENTIALS
                String slug = artistName.toLowerCase().replace(" ", "_");
                String email = slug + "@moteka.com";
                String password = slug + "_pass";
                String dummyPhone = "0" + (100000000 + new java.util.Random().nextInt(900000000));

                // 4. CRÉATION DE L'USER (ID récupéré)
                User user = userService.createUser(artistName, email, password, dummyPhone, realPhotoUrl);
                Long userId = user.getId();

                // 5. ENCODE SPOTIFY URL
                userService.updateSpotifyLink(userId, spotifyUrl);

                // 6. ENCODE RÉSEAUX (String unique avec ;)
                user.setListeReseaux(socialLinks);

                // 7. ENCODE ROLE "ARTISTE"
                userService.updateRole(userId, "ARTISTE");

                // 8. ACTIVATION DU COMPTE
                user.activateAccount();

                // SAUVEGARDE FINALE
                userService.updateUser(user);

                // ========== SYNC DES GENRES SPOTIFY ==========
                try {
                    // Appel direct au endpoint de synchronisation des genres
                    ResponseEntity<Map<String, Object>> syncResponse = syncGenresSpotify(userId);

                    if (syncResponse.getStatusCode().is2xxSuccessful() && syncResponse.getBody() != null) {
                        Boolean syncSuccess = (Boolean) syncResponse.getBody().get("success");
                        if (Boolean.TRUE.equals(syncSuccess)) {
                            List<String> genres = (List<String>) syncResponse.getBody().get("genres");
                            status.put("genresSync", "SUCCESS");
                            status.put("genresCount", genres != null ? genres.size() : 0);
                            status.put("genres", genres);
                        } else {
                            String errorMsg = (String) syncResponse.getBody().get("error");
                            status.put("genresSync", "FAILED");
                            status.put("genresError", errorMsg != null ? errorMsg : "Erreur inconnue");
                        }
                    } else {
                        status.put("genresSync", "FAILED");
                        status.put("genresError", "Échec de l'appel à syncGenresSpotify");
                    }
                } catch (Exception e) {
                    status.put("genresSync", "ERROR");
                    status.put("genresError", e.getMessage());
                    System.out.println("Erreur lors de la synchronisation des genres pour " + artistName + ": " + e.getMessage());
                }
                // =============================================

                // ========== AUTO-TAG DES PRÉFÉRENCES ==========
                try {
                    // Appel au endpoint d'auto-tagging des préférences
                    ResponseEntity<Map<String, Object>> autoTagResponse = autoTagArtistPreferencesForUser(userId);

                    if (autoTagResponse.getStatusCode().is2xxSuccessful() && autoTagResponse.getBody() != null) {
                        Boolean autoTagSuccess = (Boolean) autoTagResponse.getBody().get("success");
                        if (Boolean.TRUE.equals(autoTagSuccess)) {
                            Set<Long> genresAttribues = (Set<Long>) autoTagResponse.getBody().get("genres_attribues");
                            status.put("autoTag", "SUCCESS");
                            status.put("autoTagCount", genresAttribues != null ? genresAttribues.size() : 0);
                            status.put("autoTagGenres", genresAttribues);
                        } else {
                            String errorMsg = (String) autoTagResponse.getBody().get("error");
                            status.put("autoTag", "FAILED");
                            status.put("autoTagError", errorMsg != null ? errorMsg : "Erreur inconnue");
                        }
                    } else {
                        status.put("autoTag", "FAILED");
                        status.put("autoTagError", "Échec de l'appel à autoTagArtistPreferencesForUser");
                    }
                } catch (Exception e) {
                    status.put("autoTag", "ERROR");
                    status.put("autoTagError", e.getMessage());
                    System.out.println("Erreur lors de l'auto-tagging pour " + artistName + ": " + e.getMessage());
                }
                // ===============================================

                successCount++;
                status.put("artist", artistName);
                status.put("status", "SUCCESS");
                status.put("photoUsed", realPhotoUrl);

            } catch (Exception e) {
                status.put("line", line);
                status.put("status", "ERROR");
                status.put("message", e.getMessage());
            }
            results.add(status);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalProcessed", artistLines.size());
        response.put("successfullyImported", successCount);
        response.put("details", results);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/users/encodage-auto-genres-artistes
     * Parcourt tous les artistes en BDD, récupère leurs genres via Spotify,
     * et les encode automatiquement dans leur profil.
     */
    @PostMapping("/encodage-auto-genres-artistes")
    public ResponseEntity<Map<String, Object>> encodageAutoGenresArtistes() {

        List<Map<String, Object>> details = new ArrayList<>();
        int successCount = 0;
        int skipCount    = 0;
        int errorCount   = 0;

        // 1. Récupérer tous les artistes
        List<User> artistes = userService.getUsersByRole("ARTISTE");

        for (User artiste : artistes) {
            Map<String, Object> ligne = new HashMap<>();
            ligne.put("userId",   artiste.getId());
            ligne.put("username", artiste.getUsername());

            try {
                String spotifyUrl = artiste.getLienSpotify();

                // 2. Ignorer si pas de lien Spotify
                if (spotifyUrl == null || spotifyUrl.isBlank()) {
                    ligne.put("status",  "SKIP");
                    ligne.put("message", "Pas de lien Spotify");
                    skipCount++;
                    details.add(ligne);
                    continue;
                }

                // 3. Appel endpoint GET /api/spotify/artiste/genres
                ResponseEntity<Map<String, Object>> genresRes =
                        spotifyController.getArtisteGenres(spotifyUrl);

                if (!genresRes.getStatusCode().is2xxSuccessful()
                        || genresRes.getBody() == null
                        || !Boolean.TRUE.equals(genresRes.getBody().get("success"))) {
                    ligne.put("status",  "ERROR");
                    ligne.put("message", "Erreur Spotify : " + (genresRes.getBody() != null ? genresRes.getBody().get("error") : "null"));
                    errorCount++;
                    details.add(ligne);
                    continue;
                }

                // 4. Extraire la liste de genres
                Object rawGenres = genresRes.getBody().get("genres");
                List<String> genres = new ArrayList<>();
                if (rawGenres instanceof List<?>) {
                    for (Object g : (List<?>) rawGenres) {
                        if (g instanceof String) genres.add((String) g);
                    }
                }

                // 5. Encoder via PUT /{userId}/genres (appel direct au service)
                userService.updateGenreList(artiste.getId(), genres);

                ligne.put("status", "SUCCESS");
                ligne.put("genres", genres);
                successCount++;

            } catch (Exception e) {
                ligne.put("status",  "ERROR");
                ligne.put("message", e.getMessage());
                errorCount++;
            }

            details.add(ligne);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalArtistes", artistes.size());
        response.put("success",       successCount);
        response.put("skipped",       skipCount);
        response.put("errors",        errorCount);
        response.put("details",       details);

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/users/admin/batch-update-donnee-visible-artistes
     * Parcourt tous les artistes et met à jour le dernier caractère de donneeVisible à "1"
     * Accessible uniquement aux administrateurs
     */
    @PutMapping("/update-visible-donnee-visible-artistes")
    public ResponseEntity<Map<String, Object>> batchUpdateDonneeVisibleArtistes() {
        List<Map<String, Object>> results = new ArrayList<>();
        int updatedCount = 0;
        int skippedCount = 0;

        try {
            // 1. Récupérer tous les utilisateurs avec le rôle ARTISTE
            List<User> artistes = userService.getUsersByRole("ARTISTE");

            for (User artiste : artistes) {
                Map<String, Object> artisteStatus = new HashMap<>();
                artisteStatus.put("userId", artiste.getId());
                artisteStatus.put("username", artiste.getUsername());

                String donneeVisible = artiste.getDonneeVisible();
                String originalDonnee = donneeVisible;

                // 2. Vérifier que donneeVisible n'est pas null ou vide
                if (donneeVisible == null || donneeVisible.isEmpty()) {
                    // Si vide, on initialise avec "000" et on met le dernier à 1
                    donneeVisible = "001";
                    artiste.setDonneeVisible(donneeVisible);
                    userService.updateUser(artiste);

                    updatedCount++;
                    artisteStatus.put("status", "UPDATED");
                    artisteStatus.put("original", "null/empty");
                    artisteStatus.put("new", donneeVisible);
                    results.add(artisteStatus);
                    continue;
                }

                // 3. Modifier le dernier caractère
                int length = donneeVisible.length();
                if (length > 0) {
                    // Remplacer le dernier caractère par "1"
                    String newDonneeVisible = donneeVisible.substring(0, length - 1) + "1";

                    // 4. Vérifier si un changement est nécessaire
                    if (!newDonneeVisible.equals(donneeVisible)) {
                        artiste.setDonneeVisible(newDonneeVisible);
                        userService.updateUser(artiste);

                        updatedCount++;
                        artisteStatus.put("status", "UPDATED");
                        artisteStatus.put("original", originalDonnee);
                        artisteStatus.put("new", newDonneeVisible);
                    } else {
                        skippedCount++;
                        artisteStatus.put("status", "SKIPPED (already correct)");
                        artisteStatus.put("donneeVisible", donneeVisible);
                    }
                } else {
                    skippedCount++;
                    artisteStatus.put("status", "SKIPPED (empty string)");
                }

                results.add(artisteStatus);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mise à jour massive terminée");
            response.put("totalArtistes", artistes.size());
            response.put("updated", updatedCount);
            response.put("skipped", skippedCount);
            response.put("details", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la mise à jour massive: " + e.getMessage()
                    ));
        }
    }

    /**
     * Récupère la liste de tous les genres uniques parmi tous les artistes.
     * @return Une liste de chaînes de caractères sans doublons, triée par ordre alphabétique.
     */
    @GetMapping("/genres/all-unique")
    public ResponseEntity<?> getAllUniqueGenres() {
        try {
            // 1. Récupérer tous les utilisateurs ayant le rôle "ARTISTE"
            List<User> artistes = userRepository.findByRole("ARTISTE");

            // 2. Utiliser un Set pour garantir l'unicité et collecter les genres
            // On utilise un TreeSet pour que la liste finale soit triée par ordre alphabétique
            Set<String> uniqueGenres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            for (User artiste : artistes) {
                if (artiste.getGenreList() != null && !artiste.getGenreList().isEmpty()) {
                    // On ajoute chaque genre de l'artiste au Set
                    for (String genre : artiste.getGenreList()) {
                        if (genre != null && !genre.trim().isEmpty()) {
                            uniqueGenres.add(genre.trim().toLowerCase()); // Nettoyage simple
                        }
                    }
                }
            }

            // 3. Retourner la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", uniqueGenres.size());
            response.put("genres", uniqueGenres);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la récupération des genres uniques: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/auto-tag-artist-preferences")
    public ResponseEntity<Map<String, Object>> autoTagArtistPreferences() {
        List<Map<String, Object>> report = new ArrayList<>();

        // 1. Récupérer tous les artistes
        List<User> artistes = userService.getUsersByRole("ARTISTE");

        for (User artiste : artistes) {
            List<String> spotifyGenres = artiste.getGenreList();
            if (spotifyGenres == null || spotifyGenres.isEmpty()) continue;

            // On utilise un Set pour éviter d'ajouter plusieurs fois le même ID de genre interne
            Set<Long> internalGenreIdsToAssign = new HashSet<>();

            for (String g : spotifyGenres) {
                String genre = g.toLowerCase().trim();

                // MAPPING PRÉCIS SELON TES INSTRUCTIONS
                if (matches(genre, "afrobeat", "afrobeats", "afropiano", "afropop", "afroswing", "amapiano", "azonto", "bongo flava", "afro r&b")) {
                    internalGenreIdsToAssign.add(35L); // afrobeat & amapiano
                }

                if (matches(genre, "french pop", "french r&b", "french rap", "pop urbaine")) {
                    internalGenreIdsToAssign.add(8L); // Pop urbaine
                }

                if (matches(genre, "g-funk", "gangster rap", "hardcore hip hop", "hip hop",  "hyphy", "east coast hip hop", "new jack swing", "new orleans bounce", "old school hip hop", "southern hip hop", "west coast hip hop", "crunk", "bounce")) {
                    internalGenreIdsToAssign.add(37L); // Oldschool Rnb Hiphop
                }

                if (matches(genre, "brazilian funk", "chilean mambo", "chilean trap", "latin", "latin pop", "funk carioca", "reggaeton")) {
                    internalGenreIdsToAssign.add(9L); // Latino urbain
                }

                if (matches(genre, "roots reggae", "dancehall", "shatta")) {
                    internalGenreIdsToAssign.add(34L); // Shatta Dancehall
                }

                if (matches(genre, "drill", "trap soul")) {
                    internalGenreIdsToAssign.add(36L); // Trap & Drill
                }

                if (matches(genre, "k-ballad", "k-pop", "k-rap")) {
                    internalGenreIdsToAssign.add(1L); // Kpop
                }

                if (matches(genre, "kizomba", "kompa", "zouk")) {
                    internalGenreIdsToAssign.add(7L); // Zouk, Kizomba
                }

                if (matches(genre, "ndombolo", "rumba congolaise")) {
                    internalGenreIdsToAssign.add(40L); // Musique congolaise
                }

                if (matches(genre, "coupé décalé")) {
                    internalGenreIdsToAssign.add(41L); // Musique ivoirienne
                }
            }

            // 2. Appliquer les favoris si des correspondances ont été trouvées
            if (!internalGenreIdsToAssign.isEmpty()) {
                try {
                    // On convertit le Set en List pour ton service
                    List<Long> genreIdsList = new ArrayList<>(internalGenreIdsToAssign);

                    // Appel de ton service : userId, cotePreference=9, liste des IDs
                    musicFavorisService.createMusicFavoris(artiste.getId(), 9, genreIdsList);

                    Map<String, Object> log = new HashMap<>();
                    log.put("artiste", artiste.getUsername());
                    log.put("genres_attribues", internalGenreIdsToAssign);
                    report.add(log);
                } catch (Exception e) {
                    // Log l'erreur pour cet artiste spécifique mais continue le parcours
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tagging automatique terminé",
                "artistes_traites", report.size(),
                "details", report
        ));
    }

    // Méthode utilitaire pour vérifier si le genre Spotify contient un des mots clés
    private boolean matches(String genre, String... keywords) {
        for (String key : keywords) {
            if (genre.contains(key.toLowerCase())) return true;
        }
        return false;
    }



}