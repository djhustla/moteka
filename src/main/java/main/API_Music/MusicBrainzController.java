package main.API_Music;

import main.API_Music.MusicBrainz;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/musicbrainz")
public class MusicBrainzController {

    /**
     * GET /api/musicbrainz/artiste/reseaux?nom=Rihanna
     *
     * Retourne :
     * {
     *   "success":   true,
     *   "nom":       "Rihanna",
     *   "instagram": "https://instagram.com/...",
     *   "facebook":  "https://facebook.com/...",
     *   "tiktok":    "",
     *   "snapchat":  "",
     *   "spotify":   "https://open.spotify.com/artist/...",
     *   "youtube":   "https://youtube.com/..."
     * }
     */
    @GetMapping("/artiste/reseaux")
    public ResponseEntity<Map<String, Object>> getReseaux(@RequestParam String nom) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> liens = MusicBrainz.getLiensSociaux(nom);

            response.put("success",   true);
            response.put("nom",       nom);
            response.put("instagram", liens.get(0));
            response.put("facebook",  liens.get(1));
            response.put("tiktok",    liens.get(2));
            response.put("snapchat",  liens.get(3));
            response.put("spotify",   liens.get(4));
            response.put("youtube",   liens.get(5));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error",   e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}