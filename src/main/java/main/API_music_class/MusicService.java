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
    public Music createMusic(String lienSpotify, String lienYoutube, Long userId,
                             String genreMusical, int note) {
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
        int popularite = 0;

        try {
            // Récupérer le résumé du morceau
            ResponseEntity<Map<String, Object>> summaryResponse = spotifyController.getTrackSummary(lienSpotify);

            if (summaryResponse.getStatusCode().is2xxSuccessful() && summaryResponse.getBody() != null) {
                Map<String, Object> body = summaryResponse.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    titre = (String) body.getOrDefault("summary", "");
                    photoUrl = (String) body.getOrDefault("photoUrl", "");
                }
            }

            // Récupérer la popularité du morceau
            ResponseEntity<Map<String, Object>> popularityResponse = spotifyController.getTrackPopularity(lienSpotify);

            if (popularityResponse.getStatusCode().is2xxSuccessful() && popularityResponse.getBody() != null) {
                Map<String, Object> body = popularityResponse.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    popularite = (int) body.getOrDefault("popularite", 0);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des infos Spotify: " + e.getMessage());
        }

        // 4. Création de l'objet Music avec toutes les informations
        Music music = new Music();
        music.setLienSpotify(lienSpotify);
        music.setLienYoutube(lienYoutube);
        music.setUser(user);
        music.setTitre(titre);
        music.setUrlImageSpotify(photoUrl);
        music.setPopularite(popularite);
        music.setGenreMusical(genreMusical);
        music.setNote(note);

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

    // NOUVELLES MÉTHODES DE RECHERCHE
    public List<Music> findByGenreMusical(String genreMusical) {
        return musicRepository.findByGenreMusical(genreMusical);
    }

    public List<Music> findByNote(int note) {
        return musicRepository.findByNote(note);
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public Music update(Long id, String titre, String lienSpotify, String lienYoutube,
                        String urlImageSpotify, Integer popularite,
                        String genreMusical, Integer note) {

        Music music = musicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Music non trouvée avec l'ID: " + id));

        if (titre            != null) music.setTitre(titre);
        if (lienSpotify      != null) music.setLienSpotify(lienSpotify);
        if (lienYoutube      != null) music.setLienYoutube(lienYoutube);
        if (urlImageSpotify  != null) music.setUrlImageSpotify(urlImageSpotify);
        if (popularite       != null) music.setPopularite(popularite);
        if (genreMusical     != null) music.setGenreMusical(genreMusical);
        if (note             != null) music.setNote(note);

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