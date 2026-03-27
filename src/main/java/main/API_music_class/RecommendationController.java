package main.API_music_class;

import main.modeles.MusicFavoris;
import main.modeles.MusicGenre;
import main.modeles.User;
import main.services.MusicFavorisService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private MusicFavorisService musicFavorisService;

    @Autowired
    private UserService userService;

    @Autowired
    private MusicService musicService;

    /**
     * GET /api/recommendations/users/{userId}
     * Récupère les recommandations pour un utilisateur basées sur ses genres favoris
     * (cote 10/10 et 7/10)
     *
     * @param userId L'ID de l'utilisateur
     * @return ResponseEntity contenant les musiques et artistes recommandés par genre
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRecommendations(@PathVariable Long userId) {
        try {
            // 1. Vérifier que l'utilisateur existe
            User user = userService.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

            // 2. Récupérer les genres favoris (cote 10 et 7)
            Map<String, Object> favoriteGenres = getFavoriteGenres(userId);

            @SuppressWarnings("unchecked")
            List<String> genresCote10 = (List<String>) favoriteGenres.get("genres_cote_10");

            @SuppressWarnings("unchecked")
            List<String> genresCote7 = (List<String>) favoriteGenres.get("genres_cote_7");

            // 3. Construire les recommandations
            Map<String, Object> recommendations = new LinkedHashMap<>();

            // Pour les genres cote 10
            List<Map<String, Object>> cote10List = new ArrayList<>();
            for (String genre : genresCote10) {
                cote10List.add(buildGenreRecommendation(genre));
            }
            recommendations.put("genres_cote_10", cote10List);

            // Pour les genres cote 7
            List<Map<String, Object>> cote7List = new ArrayList<>();
            for (String genre : genresCote7) {
                cote7List.add(buildGenreRecommendation(genre));
            }
            recommendations.put("genres_cote_7", cote7List);

            // 4. Construire la réponse finale
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("username", user.getUsername());
            response.put("recommendations", recommendations);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la génération des recommandations: " + e.getMessage()
                    ));
        }
    }

    /**
     * Récupère les genres favoris d'un utilisateur (cote 10 et 7)
     */
    private Map<String, Object> getFavoriteGenres(Long userId) {
        List<MusicFavoris> favorisList = musicFavorisService.getMusicFavorisByUserId(userId);

        List<String> genresCote10 = new ArrayList<>();
        List<String> genresCote7 = new ArrayList<>();

        for (MusicFavoris favoris : favorisList) {
            Integer cote = favoris.getCotePreference();
            List<MusicGenre> genres = favoris.getGenres();

            if (genres == null || genres.isEmpty()) {
                continue;
            }

            List<String> genreNames = genres.stream()
                    .map(MusicGenre::getDescription)
                    .filter(desc -> desc != null && !desc.trim().isEmpty())
                    .collect(Collectors.toList());

            if (cote == 10) {
                genresCote10.addAll(genreNames);
            } else if (cote == 7) {
                genresCote7.addAll(genreNames);
            }
        }

        // Supprimer les doublons
        genresCote10 = genresCote10.stream().distinct().collect(Collectors.toList());
        genresCote7 = genresCote7.stream().distinct().collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("genres_cote_10", genresCote10);
        result.put("genres_cote_7", genresCote7);

        return result;
    }

    /**
     * Construit les recommandations pour un genre spécifique
     */
    private Map<String, Object> buildGenreRecommendation(String genre) {
        Map<String, Object> genreRecommendation = new LinkedHashMap<>();
        genreRecommendation.put("genre", genre);

        // Récupérer les musiques du genre
        List<Music> musiques = musicService.findByGenreMusical(genre);
        List<Map<String, Object>> musiquesList = musiques.stream()
                .map(this::musicToMap)
                .collect(Collectors.toList());
        genreRecommendation.put("musiques", musiquesList);
        genreRecommendation.put("musiques_count", musiquesList.size());

        // Récupérer les artistes du genre
        List<Map<String, Object>> artistesList = getArtistsByGenre(genre);
        genreRecommendation.put("artistes", artistesList);
        genreRecommendation.put("artistes_count", artistesList.size());

        return genreRecommendation;
    }

    /**
     * Récupère les artistes qui ont un genre spécifique (cote 9)
     */
    private List<Map<String, Object>> getArtistsByGenre(String genre) {
        List<User> allArtists = userService.getUsersByRole("ARTISTE");
        List<Map<String, Object>> matchingArtists = new ArrayList<>();

        for (User artist : allArtists) {
            List<MusicFavoris> favorisList = musicFavorisService.getMusicFavorisByUserId(artist.getId());

            for (MusicFavoris favoris : favorisList) {
                if (favoris.getCotePreference() == 9) {
                    List<MusicGenre> genres = favoris.getGenres();
                    if (genres != null && !genres.isEmpty()) {
                        boolean hasGenre = genres.stream()
                                .anyMatch(g -> g.getDescription() != null
                                        && g.getDescription().equalsIgnoreCase(genre));

                        if (hasGenre) {
                            Map<String, Object> artistInfo = new LinkedHashMap<>();
                            artistInfo.put("id", artist.getId());
                            artistInfo.put("username", artist.getUsername());
                            artistInfo.put("photoUrl", artist.getPhotoUrl());
                            artistInfo.put("lienSpotify", artist.getLienSpotify());
                            artistInfo.put("lienYoutube", artist.getLienYoutube());

                            // Ajouter les genres de l'artiste
                            List<String> genreNames = genres.stream()
                                    .map(MusicGenre::getDescription)
                                    .filter(desc -> desc != null && !desc.trim().isEmpty())
                                    .collect(Collectors.toList());
                            artistInfo.put("genres", genreNames);

                            matchingArtists.add(artistInfo);
                            break;
                        }
                    }
                }
            }
        }

        return matchingArtists;
    }

    /**
     * Convertit un objet Music en Map pour la réponse
     */
    private Map<String, Object> musicToMap(Music music) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", music.getId());
        map.put("titre", music.getTitre() != null ? music.getTitre() : "");
        map.put("lienSpotify", music.getLienSpotify() != null ? music.getLienSpotify() : "");
        map.put("lienYoutube", music.getLienYoutube() != null ? music.getLienYoutube() : "");
        map.put("urlImageSpotify", music.getUrlImageSpotify() != null ? music.getUrlImageSpotify() : "");
        map.put("popularite", music.getPopularite() != 0 ? music.getPopularite() : 0);
        map.put("note", music.getNote() != 0 ? music.getNote() : 0);
        map.put("userId", music.getUser() != null ? music.getUser().getId() : null);
        return map;
    }
}