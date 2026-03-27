package main.API_music_class;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.API_Music.SpotifyController;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private SpotifyController spotifyController;

    // ── a. CRÉATION ──────────────────────────────────────────────────────────

    /**
     * POST /api/music
     * Body : {
     *   "lienSpotify": "...",
     *   "lienYoutube": "...",
     *   "userId": 5,
     *   "genreMusical": "Pop",
     *   "note": 8
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            String lienSpotify = (String) body.get("lienSpotify");
            String lienYoutube = (String) body.get("lienYoutube");
            Long   userId      = Long.valueOf(body.get("userId").toString());
            String genreMusical = (String) body.get("genreMusical");
            Integer note = body.get("note") != null ? Integer.valueOf(body.get("note").toString()) : 0;

            // Créer l'entité Music avec les informations complètes
            Music music = musicService.createMusic(
                    lienSpotify,
                    lienYoutube,
                    userId,
                    genreMusical,
                    note != null ? note : 0
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "id",      music.getId(),
                    "userId",  music.getUser().getId(),
                    "titre",   music.getTitre(),
                    "photoUrl", music.getUrlImageSpotify(),
                    "genreMusical", music.getGenreMusical(),
                    "note",    music.getNote()
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

    // ── i. LECTURE PAR GENRE MUSICAL ─────────────────────────────────────────
    /**
     * GET /api/music/genre?genre=Pop
     */
    @GetMapping("/genre")
    public ResponseEntity<Map<String, Object>> getByGenre(@RequestParam String genre) {
        try {
            List<Music> list = musicService.findByGenreMusical(genre);
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

    // ── j. LECTURE PAR NOTE ──────────────────────────────────────────────────
    /**
     * GET /api/music/note?note=8
     */
    @GetMapping("/note")
    public ResponseEntity<Map<String, Object>> getByNote(@RequestParam int note) {
        try {
            List<Music> list = musicService.findByNote(note);
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

    // ── e. UPDATE ────────────────────────────────────────────────────────────
    /**
     * PUT /api/music/{id}
     * Body : {
     *   "titre": "...",
     *   "lienSpotify": "...",
     *   "lienYoutube": "...",
     *   "urlImageSpotify": "...",
     *   "popularite": 80,
     *   "genreMusical": "Pop",
     *   "note": 8
     * }
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
            String  genreMusical    = (String) body.get("genreMusical");
            Integer note            = body.get("note") != null
                    ? Integer.valueOf(body.get("note").toString()) : null;

            Music music = musicService.update(id, titre, lienSpotify, lienYoutube,
                    urlImageSpotify, popularite, genreMusical, note);

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

    // ── k. MISE À JOUR DE TOUTES LES NOTES ──────────────────────────────────────
    /**
     * PUT /api/music/all/notes
     * Body : { "note": 0 }
     * Met à jour la note de toutes les musiques
     */
    @PutMapping("/all/notes")
    public ResponseEntity<Map<String, Object>> updateAllNotes(@RequestBody Map<String, Object> body) {
        try {
            Integer note = body.get("note") != null ? Integer.valueOf(body.get("note").toString()) : 0;

            List<Music> allMusic = musicService.getAll();
            int updatedCount = 0;

            for (Music music : allMusic) {
                music.setNote(note);
                musicService.update(music.getId(), null, null, null, null, null, null, note);
                updatedCount++;
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", updatedCount + " musiques mises à jour avec note = " + note,
                    "updatedCount", updatedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── l. CALCULER LA POPULARITÉ DE TOUS LES MORCEAUX ────────────────────────
    /**
     * PUT /api/music/all/popularity
     * Calcule et met à jour la popularité de toutes les musiques depuis Spotify
     */
    @PutMapping("/all/popularity")
    public ResponseEntity<Map<String, Object>> updateAllPopularities() {
        try {
            List<Music> allMusic = musicService.getAll();
            int updatedCount = 0;
            int errorCount = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (Music music : allMusic) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", music.getId());
                result.put("titre", music.getTitre() != null ? music.getTitre() : "Sans titre");

                try {
                    // Récupérer la popularité depuis Spotify
                    String spotifyUrl = music.getLienSpotify();
                    Integer popularity = 0;

                    if (spotifyUrl != null && !spotifyUrl.isEmpty()) {
                        ResponseEntity<Map<String, Object>> popularityResponse =
                                spotifyController.getTrackPopularity(spotifyUrl);

                        if (popularityResponse.getStatusCode().is2xxSuccessful() &&
                                popularityResponse.getBody() != null) {
                            Map<String, Object> body = popularityResponse.getBody();
                            if (Boolean.TRUE.equals(body.get("success"))) {
                                popularity = (Integer) body.getOrDefault("popularite", 0);
                            }
                        }
                    }

                    // Mettre à jour la popularité
                    music.setPopularite(popularity);
                    musicService.update(music.getId(), null, null, null, null, popularity, null, null);

                    result.put("status", "success");
                    result.put("popularite", popularity);
                    updatedCount++;

                } catch (Exception e) {
                    result.put("status", "error");
                    result.put("error", e.getMessage());
                    errorCount++;
                }

                results.add(result);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("%d musiques mises à jour, %d erreurs", updatedCount, errorCount));
            response.put("updatedCount", updatedCount);
            response.put("errorCount", errorCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

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
                "popularite",      m.getPopularite()      != 0 ? m.getPopularite()      : 0,
                "genreMusical",    m.getGenreMusical()    != null ? m.getGenreMusical()    : "",
                "note",            m.getNote()            != 0 ? m.getNote()            : 0,
                "userId",          m.getUser().getId()
        );
    }
}