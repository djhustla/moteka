package main.controlleurs;

import main.modeles.MusicFavoris;
import main.modeles.User;
import main.repository.UserRepository;
import main.security.JwtUtil;
import main.services.MusicFavorisService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static main.divers_services.github.GitHubFileUploader.uploadFileGitHub;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicFavorisService musicFavorisService;

    // === ENDPOINTS D'AUTHENTIFICATION ===

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier photo est requis"));
            }

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

                User user = userService.createUser(username, email, password, uploadedFileUrl);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Utilisateur créé avec succès");
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                response.put("photoUrl", uploadedFileUrl);
                response.put("createdAt", user.getCreatedAt().toString());

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

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
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le fichier photo est requis"));
            }

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

                User user = userService.createUserWithRole(username, email, password, "ADMIN", uploadedFileUrl);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Administrateur créé avec succès");
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                response.put("photoUrl", uploadedFileUrl);

                return ResponseEntity.ok(response);

            } finally {
                Files.deleteIfExists(tempFile);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création de l'administrateur : " + e.getMessage()));
        }
    }

    // === ENDPOINTS DE PROFIL UTILISATEUR ===

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

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(user.getId());

            List<Map<String, Object>> musicFavorisList = favoris.stream()
                    .map(favori -> {
                        Map<String, Object> favoriMap = new HashMap<>();
                        favoriMap.put("musicGenreId", favori.getMusicGenre().getId());
                        favoriMap.put("musicGenreDescription", favori.getMusicGenre().getDescription());
                        favoriMap.put("cotePreference", favori.getCotePreference());
                        return favoriMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("photoUrl", user.getPhotoUrl());
            response.put("sonEntreeURL", user.getSonEntreeURL());
            response.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
            response.put("musicFavoris", musicFavorisList);
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

    // === ENDPOINTS DE GESTION DES UTILISATEURS ===

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(userId);

            List<Map<String, Object>> musicFavorisList = favoris.stream()
                    .map(favori -> {
                        Map<String, Object> favoriMap = new HashMap<>();
                        favoriMap.put("musicGenreId", favori.getMusicGenre().getId());
                        favoriMap.put("musicGenreDescription", favori.getMusicGenre().getDescription());
                        favoriMap.put("cotePreference", favori.getCotePreference());
                        return favoriMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt().toString());
            response.put("photoUrl", user.getPhotoUrl());
            response.put("sonEntreeURL", user.getSonEntreeURL());
            response.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
            response.put("musicFavoris", musicFavorisList);
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des utilisateurs"));
        }
    }

    // === ENDPOINTS DE SUPPRESSION - ACCÈS LIBRE POUR TOUS LES UTILISATEURS ===

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

    // === ENDPOINTS POUR MUSICFAVORIS ===

    @PostMapping("/me/music-favoris")
    public ResponseEntity<Map<String, Object>> createMusicFavoris(@RequestBody List<UserService.MusicFavorisRequest> favorisRequests) {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<MusicFavoris> createdFavoris = userService.createMusicFavoris(currentUser.getId(), favorisRequests);

            List<Map<String, Object>> favorisList = createdFavoris.stream()
                    .map(favoris -> {
                        Map<String, Object> favorisMap = new HashMap<>();
                        favorisMap.put("id", favoris.getId());
                        favorisMap.put("musicGenreId", favoris.getMusicGenre().getId());
                        favorisMap.put("musicGenreDescription", favoris.getMusicGenre().getDescription());
                        favorisMap.put("cotePreference", favoris.getCotePreference());
                        return favorisMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Favoris musicaux créés avec succès");
            response.put("favoris", favorisList);
            response.put("count", favorisList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création des favoris: " + e.getMessage()));
        }
    }

    @GetMapping("/me/music-favoris")
    public ResponseEntity<Map<String, Object>> getMusicFavoris() {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<MusicFavoris> favoris = userService.getMusicFavoris(currentUser.getId());

            List<Map<String, Object>> favorisList = favoris.stream()
                    .map(favori -> {
                        Map<String, Object> favorisMap = new HashMap<>();
                        favorisMap.put("id", favori.getId());
                        favorisMap.put("musicGenreId", favori.getMusicGenre().getId());
                        favorisMap.put("musicGenreDescription", favori.getMusicGenre().getDescription());
                        favorisMap.put("cotePreference", favori.getCotePreference());
                        return favorisMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("favoris", favorisList);
            response.put("count", favorisList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des favoris: " + e.getMessage()));
        }
    }

    @PutMapping("/me/music-favoris")
    public ResponseEntity<Map<String, Object>> updateMusicFavoris(@RequestBody List<UserService.MusicFavorisRequest> favorisRequests) {
        try {
            String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<MusicFavoris> updatedFavoris = userService.updateMusicFavoris(currentUser.getId(), favorisRequests);

            List<Map<String, Object>> favorisList = updatedFavoris.stream()
                    .map(favoris -> {
                        Map<String, Object> favorisMap = new HashMap<>();
                        favorisMap.put("id", favoris.getId());
                        favorisMap.put("musicGenreId", favoris.getMusicGenre().getId());
                        favorisMap.put("musicGenreDescription", favoris.getMusicGenre().getDescription());
                        favorisMap.put("cotePreference", favoris.getCotePreference());
                        return favorisMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Favoris musicaux mis à jour avec succès");
            response.put("favoris", favorisList);
            response.put("count", favorisList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour des favoris: " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}/music-favoris")
    public ResponseEntity<Map<String, Object>> getUserMusicFavoris(@PathVariable Long userId) {
        try {
            List<MusicFavoris> favoris = userService.getMusicFavoris(userId);

            List<Map<String, Object>> favorisList = favoris.stream()
                    .map(favori -> {
                        Map<String, Object> favorisMap = new HashMap<>();
                        favorisMap.put("id", favori.getId());
                        favorisMap.put("musicGenreId", favori.getMusicGenre().getId());
                        favorisMap.put("musicGenreDescription", favori.getMusicGenre().getDescription());
                        favorisMap.put("cotePreference", favori.getCotePreference());
                        return favorisMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("favoris", favorisList);
            response.put("count", favorisList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des favoris: " + e.getMessage()));
        }
    }

    // === ENDPOINTS DE RECHERCHE D'UTILISATEURS PAR MUSIQUE ===

    @GetMapping("/me/same-music-favoris")
    public ResponseEntity<Map<String, Object>> getUsersWithSameMusicFavoris(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

            List<User> usersWithSameFavoris = userService.getUsersWithExactSameMusicFavoris(currentUser.getId());

            List<Map<String, Object>> usersList = usersWithSameFavoris.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
                        userMap.put("role", user.getRole());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la recherche des utilisateurs avec les mêmes favoris"));
        }
    }

    @GetMapping("/me/opposite-music-favoris")
    public ResponseEntity<Map<String, Object>> getUsersWithOppositeMusicFavoris(Principal principal) {
        try {
            String username = principal.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

            List<User> usersWithOppositeFavoris = userService.getUsersWithOppositeMusicFavoris(currentUser.getId());

            List<Map<String, Object>> usersList = usersWithOppositeFavoris.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
                        userMap.put("role", user.getRole());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la recherche des utilisateurs avec des goûts opposés"));
        }
    }

    @GetMapping("/me/same-top-music")
    public ResponseEntity<Map<String, Object>> getUsersWithSameTopMusic(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));

            List<User> usersWithSameTopMusic = userService.getUsersWithSameTopMusicFavoris(currentUser.getId());

            List<Map<String, Object>> usersList = usersWithSameTopMusic.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("username", user.getUsername());
                        userMap.put("email", user.getEmail());
                        userMap.put("photoUrl", user.getPhotoUrl());
                        userMap.put("donneeVisible", user.getDonneeVisible()); // ✅ AJOUTÉ
                        userMap.put("role", user.getRole());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la recherche des utilisateurs avec la même musique favorite"));
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

    // Dans UserController.java - AJOUTEZ CETTE MÉTHODE
    @PutMapping("/me/update")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "currentPassword", required = false) String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "donneeVisible", required = false) String donneeVisible, // ✅ AJOUTÉ
            @RequestParam(value = "removePhoto", required = false) String removePhotoParam,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            // 1. Récupérer l'utilisateur connecté
            String currentUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 2. Convertir removePhoto en boolean
            boolean removePhoto = removePhotoParam != null && removePhotoParam.equalsIgnoreCase("true");

            // 3. Gérer l'upload de photo si un fichier est fourni
            String photoUrl = currentUser.getPhotoUrl(); // Garde l'ancienne par défaut

            if (file != null && !file.isEmpty()) {
                // Validation du fichier
                if (!file.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Le fichier doit être une image"));
                }

                if (file.getSize() > 5 * 1024 * 1024) { // 5MB max
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "L'image est trop volumineuse (max 5MB)"));
                }

                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Nom de fichier invalide"));
                }

                // Upload vers GitHub
                Path tempFile = Files.createTempFile("profile_", "_" + originalName);
                try {
                    file.transferTo(tempFile);
                    String uploadedFileUrl = uploadFileGitHub(tempFile.toString(), "profiles");

                    if (uploadedFileUrl == null) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Erreur lors de l'upload de la photo"));
                    }

                    photoUrl = uploadedFileUrl;
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            // 4. Mettre à jour donneeVisible si fourni
            if (donneeVisible != null) {
                currentUser.setDonneeVisible(donneeVisible);
            }

            // 5. Appeler le service
            User updatedUser = userService.updateCurrentUser(
                    currentUser.getId(),
                    username,
                    email,
                    currentPassword,
                    newPassword,
                    removePhoto,
                    photoUrl
            );

            // 6. Retourner la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("user", Map.of(
                    "id", updatedUser.getId(),
                    "username", updatedUser.getUsername(),
                    "email", updatedUser.getEmail(),
                    "role", updatedUser.getRole(),
                    "photoUrl", updatedUser.getPhotoUrl(),
                    "donneeVisible", updatedUser.getDonneeVisible(), // ✅ AJOUTÉ
                    "createdAt", updatedUser.getCreatedAt()
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


/*



* */