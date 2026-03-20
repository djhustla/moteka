package main.API_music_class;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import main.API_Music.SpotifyController;  // 👈 LIGNE D'IMPORT

@RestController
@RequestMapping("/api/music")
public class MusicController {

    @Autowired
    private MusicService musicService;

    // ── a. CRÉATION ──────────────────────────────────────────────────────────


    /**
     * POST /api/music
     * Body : { "lienSpotify": "...", "lienYoutube": "...", "userId": 5 }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            String lienSpotify = (String) body.get("lienSpotify");
            String lienYoutube = (String) body.get("lienYoutube");
            Long   userId      = Long.valueOf(body.get("userId").toString());


            // Créer l'entité Music avec les informations complètes
            Music music = musicService.createMusic(
                    lienSpotify,
                    lienYoutube,
                    userId
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "id",      music.getId(),
                    "userId",  music.getUser().getId(),
                    "titre",   music.getTitre(),
                    "photoUrl", music.getUrlImageSpotify()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── h. LECTURE DE TOUTES LES MUSIQUES ──────────────────────────────────────
    /**
     * GET /api/music/all
     * Retourne la liste de toutes les musiques
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        try {
            List<Music> allMusic = musicService.getAll();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count",   allMusic.size(),
                    "data",    allMusic.stream().map(this::toMap).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── b. LECTURE PAR ID ────────────────────────────────────────────────────
    /**
     * GET /api/music/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        try {
            Music music = musicService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Music non trouvée avec l'ID: " + id));

            return ResponseEntity.ok(toMap(music));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── c. LECTURE PAR TITRE ─────────────────────────────────────────────────
    /**
     * GET /api/music/titre?titre=Bohemian
     */
    @GetMapping("/titre")
    public ResponseEntity<Map<String, Object>> getByTitre(@RequestParam String titre) {
        try {
            List<Music> list = musicService.findByTitre(titre);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count",   list.size(),
                    "data",    list.stream().map(this::toMap).toList()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── d. LECTURE PAR LIEN YOUTUBE ──────────────────────────────────────────
    /**
     * GET /api/music/youtube?url=https://youtube.com/...
     */
    @GetMapping("/youtube")
    public ResponseEntity<Map<String, Object>> getByYoutube(@RequestParam String url) {
        try {
            Music music = musicService.findByLienYoutube(url)
                    .orElseThrow(() -> new RuntimeException("Aucune music trouvée avec ce lien YouTube"));

            return ResponseEntity.ok(toMap(music));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── e. UPDATE ────────────────────────────────────────────────────────────
    /**
     * PUT /api/music/{id}
     * Body : { "titre": "...", "lienSpotify": "...", "lienYoutube": "...",
     *          "urlImageSpotify": "...", "popularite": 80 }
     * Tous les champs sont optionnels — seuls les champs présents sont mis à jour.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String  titre           = (String) body.get("titre");
            String  lienSpotify     = (String) body.get("lienSpotify");
            String  lienYoutube     = (String) body.get("lienYoutube");
            String  urlImageSpotify = (String) body.get("urlImageSpotify");
            Integer popularite      = body.get("popularite") != null
                    ? Integer.valueOf(body.get("popularite").toString()) : null;

            Music music = musicService.update(id, titre, lienSpotify, lienYoutube, urlImageSpotify, popularite);

            return ResponseEntity.ok(Map.of("success", true, "data", toMap(music)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── f. SUPPRESSION PAR ID ────────────────────────────────────────────────
    /**
     * DELETE /api/music/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteById(@PathVariable Long id) {
        try {
            musicService.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── g. SUPPRESSION DE TOUT ───────────────────────────────────────────────
    /**
     * DELETE /api/music/all
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
        try {
            musicService.deleteAll();
            return ResponseEntity.ok(Map.of("success", true, "message", "Toutes les musiques supprimées"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ── HELPER ───────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(Music m) {
        return Map.of(
                "success",         true,
                "id",              m.getId(),
                "titre",           m.getTitre()           != null ? m.getTitre()           : "",
                "lienSpotify",     m.getLienSpotify()     != null ? m.getLienSpotify()     : "",
                "lienYoutube",     m.getLienYoutube()     != null ? m.getLienYoutube()     : "",
                "urlImageSpotify", m.getUrlImageSpotify() != null ? m.getUrlImageSpotify() : "",
                "popularite",      m.getPopularite(),
                "userId",          m.getUser().getId()
        );
    }
}