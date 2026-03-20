package main.API_music_class;

import main.API_Music.SpotifyController;
import main.modeles.User;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MusicService {

    @Autowired
    private MusicRepository musicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpotifyController spotifyController;

    // ── CRÉATION ─────────────────────────────────────────────────────────────
    public Music createMusic(String lienSpotify, String lienYoutube, Long userId) {
        // 1. Vérification de l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // 2. CORRECTION DU LIEN YOUTUBE (Normalisation)
        // On remplace "m.youtube.com" par "www.youtube.com" si présent
        if (lienYoutube != null && lienYoutube.contains("m.youtube.com")) {
            lienYoutube = lienYoutube.replace("m.youtube.com", "www.youtube.com");
        }

        // 3. Récupérer les informations via Spotify (Préparation)
        String titre = "";
        String photoUrl = "";

        try {
            ResponseEntity<Map<String, Object>> summaryResponse = spotifyController.getTrackSummary(lienSpotify);

            if (summaryResponse.getStatusCode().is2xxSuccessful() && summaryResponse.getBody() != null) {
                Map<String, Object> body = summaryResponse.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    titre = (String) body.getOrDefault("summary", "");
                    photoUrl = (String) body.getOrDefault("photoUrl", "");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des infos Spotify: " + e.getMessage());
        }

        // 4. Création de l'objet Music avec le lien corrigé
        Music music = new Music();
        music.setLienSpotify(lienSpotify);
        music.setLienYoutube(lienYoutube); // Ici, le lien est déjà corrigé
        music.setUser(user);
        music.setTitre(titre);
        music.setUrlImageSpotify(photoUrl);

        // 5. Sauvegarde
        return musicRepository.save(music);
    }

    // ── LECTURE ──────────────────────────────────────────────────────────────
    public Optional<Music> findById(Long id) {
        return musicRepository.findById(id);
    }

    public List<Music> findByTitre(String titre) {
        return musicRepository.findByTitreContainingIgnoreCase(titre);
    }

    public Optional<Music> findByLienYoutube(String lienYoutube) {
        return musicRepository.findByLienYoutube(lienYoutube);
    }

    public List<Music> getAll() {
        return musicRepository.findAll();
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public Music update(Long id, String titre, String lienSpotify, String lienYoutube,
                        String urlImageSpotify, Integer popularite) {

        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Music non trouvée avec l'ID: " + id));

        if (titre            != null) music.setTitre(titre);
        if (lienSpotify      != null) music.setLienSpotify(lienSpotify);
        if (lienYoutube      != null) music.setLienYoutube(lienYoutube);
        if (urlImageSpotify  != null) music.setUrlImageSpotify(urlImageSpotify);
        if (popularite       != null) music.setPopularite(popularite);

        return musicRepository.save(music);
    }

    // ── SUPPRESSION ──────────────────────────────────────────────────────────
    public void deleteById(Long id) {
        if (!musicRepository.existsById(id)) {
            throw new RuntimeException("Music non trouvée avec l'ID: " + id);
        }
        musicRepository.deleteById(id);
    }

    public void deleteAll() {
        musicRepository.deleteAll();
    }
}