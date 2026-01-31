package main.controlleurs;

import main.modeles.MusicFavoris;
import main.modeles.MusicGenre;
import main.modeles.User;
import main.services.MusicFavorisService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music-favoris")
public class MusicFavorisController {

    private final MusicFavorisService musicFavorisService;
    private final UserService userService;

    @Autowired
    public MusicFavorisController(MusicFavorisService musicFavorisService, UserService userService) {
        this.musicFavorisService = musicFavorisService;
        this.userService = userService;
    }

    // =====================
    // ENDPOINTS "/me" (NOUVEAUX)
    // =====================

    /**
     * Récupère les MusicFavoris de l'utilisateur connecté
     * GET /api/music-favoris/users/me
     */
    @GetMapping("/users/me")
    public ResponseEntity<?> getMyMusicFavoris() {
        try {
            // Récupérer l'utilisateur connecté
            User user = getCurrentAuthenticatedUser();

            // Récupérer ses MusicFavoris
            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(user.getId());
            return ResponseEntity.ok(favoris);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la récupération de vos préférences"));
        }
    }


    /**
     * Crée un MusicFavoris pour l'utilisateur connecté
     * POST /api/music-favoris/users/me
     */
    @PostMapping("/users/me")
    public ResponseEntity<?> createMusicFavorisForMe(@RequestBody Map<String, Object> request) {
        try {
            // Récupérer l'utilisateur connecté
            User user = getCurrentAuthenticatedUser();

            // Extraire les données de la requête
            Integer cotePreference = (Integer) request.get("cotePreference");
            List<Long> genreIds = (List<Long>) request.get("genreIds");

            // Créer le MusicFavoris
            MusicFavoris created = musicFavorisService.createMusicFavoris(
                    user.getId(), cotePreference, genreIds);

            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne"));
        }
    }


    /**
     * Supprime tous les MusicFavoris de l'utilisateur connecté
     * DELETE /api/music-favoris/users/me/all
     */
    @DeleteMapping("/users/me/all")
    public ResponseEntity<?> deleteAllMyMusicFavoris() {
        try {
            // Récupérer l'utilisateur connecté
            User user = getCurrentAuthenticatedUser();

            // Supprimer tous ses MusicFavoris
            musicFavorisService.deleteAllMusicFavorisForUser(user.getId());
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la suppression"));
        }
    }

    // =====================
    // MÉTHODE UTILITAIRE POUR RÉCUPÉRER L'UTILISATEUR CONNECTÉ
    // =====================

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        // Récupérer le username depuis l'authentication
        String username = (String) authentication.getPrincipal();

        // Trouver l'utilisateur dans la base de données
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec le username: " + username));
    }

    // =====================
    // CRÉATION (EXISTANTS)
    // =====================

    @PostMapping
    public ResponseEntity<?> createMusicFavoris(@RequestBody MusicFavoris musicFavoris) {
        try {
            MusicFavoris created = musicFavorisService.createMusicFavoris(musicFavoris);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne"));
        }
    }

    @PostMapping("/users/{userId}")
    public ResponseEntity<?> createMusicFavorisForUser(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {

        try {
            Integer cotePreference = (Integer) request.get("cotePreference");
            List<Long> genreIds = (List<Long>) request.get("genreIds");

            MusicFavoris created = musicFavorisService.createMusicFavoris(userId, cotePreference, genreIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur interne"));
        }
    }

    // =====================
    // LECTURE (EXISTANTS)
    // =====================

    @GetMapping
    public ResponseEntity<?> getAllMusicFavoris() {
        try {
            List<MusicFavoris> favoris = musicFavorisService.getAllMusicFavoris();
            return ResponseEntity.ok(favoris);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de récupération"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMusicFavorisById(@PathVariable Long id) {
        try {
            return musicFavorisService.getMusicFavorisById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de récupération"));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getMusicFavorisByUserId(@PathVariable Long userId) {
        try {
            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(userId);
            return ResponseEntity.ok(favoris);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de récupération"));
        }
    }

    @GetMapping("/{id}/users/{userId}")
    public ResponseEntity<?> getMusicFavorisByIdAndUserId(
            @PathVariable Long id,
            @PathVariable Long userId) {

        try {
            return musicFavorisService.getMusicFavorisByIdAndUserId(id, userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de récupération"));
        }
    }

    @GetMapping("/{id}/genres")
    public ResponseEntity<?> getGenresOfMusicFavoris(@PathVariable Long id) {
        try {
            List<MusicGenre> genres = musicFavorisService.getGenresOfMusicFavoris(id);
            return ResponseEntity.ok(genres);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de récupération"));
        }
    }

    // =====================
    // MISE À JOUR (EXISTANTS)
    // =====================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMusicFavoris(
            @PathVariable Long id,
            @RequestBody MusicFavoris details) {

        try {
            MusicFavoris updated = musicFavorisService.updateMusicFavoris(id, details);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de mise à jour"));
        }
    }

    @PutMapping("/{id}/users/{userId}")
    public ResponseEntity<?> updateMusicFavorisForUser(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody MusicFavoris details) {

        try {
            MusicFavoris updated = musicFavorisService.updateMusicFavorisForUser(id, userId, details);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de mise à jour"));
        }
    }

    @PostMapping("/{id}/genres")
    public ResponseEntity<?> addGenreToMusicFavoris(
            @PathVariable Long id,
            @RequestBody MusicGenre genre) {

        try {
            MusicFavoris updated = musicFavorisService.addGenreToMusicFavoris(id, genre);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur d'ajout"));
        }
    }

    // =====================
    // SUPPRESSION DE GENRES (EXISTANTS)
    // =====================

    @DeleteMapping("/{id}/genres/{genreId}")
    public ResponseEntity<?> removeGenreFromMusicFavoris(
            @PathVariable Long id,
            @PathVariable Long genreId) {

        try {
            MusicFavoris updated = musicFavorisService.removeGenreFromMusicFavoris(id, genreId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    @DeleteMapping("/{id}/users/{userId}/genres/{genreId}")
    public ResponseEntity<?> removeGenreFromUserMusicFavoris(
            @PathVariable Long id,
            @PathVariable Long userId,
            @PathVariable Long genreId) {

        try {
            MusicFavoris updated = musicFavorisService.removeGenreFromUserMusicFavoris(id, userId, genreId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    @DeleteMapping("/{id}/genres")
    public ResponseEntity<?> removeGenresFromMusicFavoris(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {

        try {
            List<Long> genreIds = request.get("genreIds");
            if (genreIds == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Liste des genreIds manquante"));
            }

            MusicFavoris updated = musicFavorisService.removeGenresFromMusicFavoris(id, genreIds);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    // =====================
    // SUPPRESSION (EXISTANTS)
    // =====================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMusicFavoris(@PathVariable Long id) {
        try {
            musicFavorisService.deleteMusicFavoris(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<?> deleteMusicFavorisForUser(
            @PathVariable Long id,
            @PathVariable Long userId) {

        try {
            musicFavorisService.deleteMusicFavorisForUser(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    @DeleteMapping("/users/{userId}/all")
    public ResponseEntity<?> deleteAllMusicFavorisForUser(@PathVariable Long userId) {
        try {
            musicFavorisService.deleteAllMusicFavorisForUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de suppression"));
        }
    }

    // =====================
    // STATISTIQUES (EXISTANTS)
    // =====================

    @GetMapping("/count")
    public ResponseEntity<?> countAllMusicFavoris() {
        try {
            long count = musicFavorisService.countAllMusicFavoris();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de comptage"));
        }
    }

    @GetMapping("/users/{userId}/count")
    public ResponseEntity<?> countMusicFavorisByUserId(@PathVariable Long userId) {
        try {
            long count = musicFavorisService.countMusicFavorisByUserId(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur de comptage"));
        }
    }


    // 1 meme gouts principaux
    @GetMapping("me/similar-users")
    public ResponseEntity<?> getUsersWithSameMainPreferences() {
        try {
            // 1. Récupérer l'utilisateur connecté
            User currentUser = getCurrentAuthenticatedUser();

            // 2. Chercher les utilisateurs similaires
            List<User> similarUsers = musicFavorisService.findUsersWithSameMainPreferences(currentUser.getId());

            // 3. Retourner la réponse
            if (similarUsers.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Aucun utilisateur avec des goûts similaires trouvé");
                response.put("count", 0);
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("users", similarUsers);
            response.put("count", similarUsers.size());
            response.put("message", String.format("%d utilisateur(s) avec des goûts similaires trouvé(s)", similarUsers.size()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Erreur de paramètre
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Paramètre invalide", "details", e.getMessage()));

        } catch (RuntimeException e) {
            // Erreur d'authentification ou autre
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentification requise", "details", e.getMessage()));

        } catch (Exception e) {
            // Erreur serveur
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur serveur", "details", "Impossible de traiter la requête"));
        }
    }

    // 2 meme kiff
    /**
         * GET /api/music-favoris/same-top-kiff/me
         * Retourne les utilisateurs qui ont les mêmes kiffs (cote 10/10) que l'utilisateur connecté
         * L'utilisateur connecté est obtenu automatiquement via @AuthenticationPrincipal
    */
    @GetMapping("me/same-top-kiff")
    public ResponseEntity<?> getUsersWithSameTopKiff() {
            try {


                User currentUser = getCurrentAuthenticatedUser();

                // 2. Appel du service
                List<User> similarUsers = musicFavorisService
                        .findUsersWithSameTopKiff(currentUser.getId());

                // 3. Préparer la réponse simple
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("currentUserId", currentUser.getId());
                response.put("currentUsername", currentUser.getUsername());
                response.put("similarUsersCount", similarUsers.size());
                response.put("similarUsers", similarUsers);

                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                // Erreur de validation
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Requête invalide",
                        e.getMessage()
                );

            } catch (RuntimeException e) {
                // Erreur interne
                return buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur lors de la recherche",
                        e.getMessage()
                );

            } catch (Exception e) {
                // Erreur inattendue
                return buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur technique",
                        "Une erreur inattendue est survenue"
                );
            }
        }



    // 3 oposé principaux
        /**
         * GET /api/music-favoris/me/opposite
         * Trouve les utilisateurs avec des goûts opposés aux vôtres
         * Retourne une liste simple d'utilisateurs
         */
    @GetMapping("/me/opposite")
    public ResponseEntity<?> getUsersWithOppositePreferences() {
            try {
                // 1. Récupérer l'utilisateur connecté
                User currentUser = getCurrentAuthenticatedUser();

                // 2. Vérification de l'authentification
                if (currentUser == null) {
                    return buildErrorResponse(
                            HttpStatus.UNAUTHORIZED,
                            "Non authentifié",
                            "Vous devez être connecté pour accéder à cette ressource"
                    );
                }

                // 3. Appel du service (version simplifiée)
                List<User> oppositeUsers = musicFavorisService
                        .findUsersWithOppositePreferencesSimple(currentUser.getId());

                // 4. Préparer la réponse simple
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("currentUserId", currentUser.getId());
                response.put("currentUsername", currentUser.getUsername());
                response.put("oppositeUsersCount", oppositeUsers.size());
                response.put("oppositeUsers", oppositeUsers);

                return ResponseEntity.ok(response);

            } catch (IllegalArgumentException e) {
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Requête invalide",
                        e.getMessage()
                );

            } catch (RuntimeException e) {
                return buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur lors de la recherche",
                        e.getMessage()
                );

            } catch (Exception e) {
                return buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur technique",
                        "Une erreur inattendue est survenue"
                );
            }
        }

    // 4. ENDPOINT OPPOSITIONS TOTALES
    @GetMapping("/me/oppositions-total")
    public ResponseEntity<Map<String, Object>> getOppositionsTotal() {
        try {
            User currentUser = getCurrentAuthenticatedUser();

            if (currentUser == null) {
                return buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "User not authenticated"
                );
            }

            List<User> users = musicFavorisService.getOppositionsTotal(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users);
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Request",
                    e.getMessage()
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error",
                    "Internal server error occurred"
            );
        }
    }

    // 5. ENDPOINT MATCH TOTAL
    @GetMapping("/me/match-total")
    public ResponseEntity<Map<String, Object>> getMatchTotal() {
        try {
            User currentUser = getCurrentAuthenticatedUser();

            if (currentUser == null) {
                return buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "User not authenticated"
                );
            }

            List<User> users = musicFavorisService.getMatchTotal(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users);
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Request",
                    e.getMessage()
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error",
                    "Internal server error occurred"
            );
        }
    }

    // 6. ENDPOINT CONCORDANCE MOYENNE
    @GetMapping("/me/concordence-moyenne")
    public ResponseEntity<Map<String, Object>> getConcordenceMoyenne() {
        try {
            User currentUser = getCurrentAuthenticatedUser();

            if (currentUser == null) {
                return buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Authentication Failed",
                        "User not authenticated"
                );
            }

            List<User> users = musicFavorisService.getConcordenceMoyenne(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", users);
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Request",
                    e.getMessage()
            );
        } catch (Exception e) {
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Server Error",
                    "Internal server error occurred"
            );
        }
    }

        /**
         * Méthode utilitaire pour construire les réponses d'erreur
         */
        private ResponseEntity<Map<String, Object>> buildErrorResponse(
                HttpStatus status, String error, String message) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", error);
            errorResponse.put("message", message);
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(status).body(errorResponse);
        }



















}