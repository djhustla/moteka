package main.controlleurs;

import main.modeles.MusicFavoris;
import main.services.MusicFavorisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/music-favoris")
// ✅ @CrossOrigin(origins = "*") SUPPRIMÉ - Géré par SecurityConfig
public class MusicFavorisController {

    @Autowired
    private MusicFavorisService musicFavorisService;

    // CREATE - Ajouter un favori pour un utilisateur
    @PostMapping("/user/{userId}/genre/{musicGenreId}")
    public ResponseEntity<?> createMusicFavoris(
            @PathVariable Long userId,
            @PathVariable Long musicGenreId,
            @RequestParam Integer cotePreference) {

        try {
            MusicFavoris musicFavoris = musicFavorisService.createMusicFavoris(userId, musicGenreId, cotePreference);
            return new ResponseEntity<>(musicFavoris, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de l'ajout du favori", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // READ - Récupérer tous les favoris d'un utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMusicFavorisByUserId(@PathVariable Long userId) {
        try {
            List<MusicFavoris> favoris = musicFavorisService.getMusicFavorisByUserId(userId);
            return new ResponseEntity<>(favoris, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la récupération des favoris", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // READ - Récupérer un favori spécifique par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMusicFavorisById(@PathVariable Long id) {
        try {
            Optional<MusicFavoris> musicFavoris = musicFavorisService.getMusicFavorisById(id);

            if (musicFavoris.isPresent()) {
                return new ResponseEntity<>(musicFavoris.get(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Favori non trouvé avec l'ID: " + id, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la récupération du favori", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // UPDATE - Mettre à jour la cote de préférence d'un favori
    @PutMapping("/user/{userId}/genre/{musicGenreId}")
    public ResponseEntity<?> updateMusicFavoris(
            @PathVariable Long userId,
            @PathVariable Long musicGenreId,
            @RequestParam Integer cotePreference) {

        try {
            MusicFavoris updatedFavoris = musicFavorisService.updateMusicFavoris(userId, musicGenreId, cotePreference);
            return new ResponseEntity<>(updatedFavoris, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la mise à jour du favori", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE - Supprimer un favori spécifique
    @DeleteMapping("/user/{userId}/genre/{musicGenreId}")
    public ResponseEntity<?> deleteMusicFavoris(
            @PathVariable Long userId,
            @PathVariable Long musicGenreId) {

        try {
            musicFavorisService.deleteMusicFavoris(userId, musicGenreId);
            return new ResponseEntity<>("Favori supprimé avec succès", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la suppression du favori", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE - Supprimer tous les favoris d'un utilisateur
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteAllMusicFavorisByUserId(@PathVariable Long userId) {
        try {
            musicFavorisService.deleteAllMusicFavorisByUserId(userId);
            return new ResponseEntity<>("Tous les favoris de l'utilisateur ont été supprimés", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la suppression des favoris", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET - Vérifier si un favori existe
    @GetMapping("/user/{userId}/genre/{musicGenreId}/exists")
    public ResponseEntity<Boolean> checkMusicFavorisExists(
            @PathVariable Long userId,
            @PathVariable Long musicGenreId) {

        boolean exists = musicFavorisService.existsByUserIdAndMusicGenreId(userId, musicGenreId);
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }

    // GET - Compter les favoris d'un utilisateur
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> countMusicFavorisByUserId(@PathVariable Long userId) {
        try {
            long count = musicFavorisService.countByUserId(userId);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(0L, HttpStatus.NOT_FOUND);
        }
    }
}