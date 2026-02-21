package main.controlleurs;

import main.modeles.MusicGenre;
import main.services.MusicGenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/music-genres")
// ✅ @CrossOrigin(origins = "*") SUPPRIMÉ - Géré par SecurityConfig
public class MusicGenreController {

    @Autowired
    private MusicGenreService musicGenreService;

    // CREATE - Créer un nouveau genre musical
    @PostMapping
    public ResponseEntity<?> createMusicGenre(@RequestBody MusicGenre musicGenre) {
        try {
            MusicGenre createdMusicGenre = musicGenreService.createMusicGenre(musicGenre);
            return new ResponseEntity<>(createdMusicGenre, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la création du genre musical", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // READ - Récupérer tous les genres musicaux
    @GetMapping
    public ResponseEntity<List<MusicGenre>> getAllMusicGenres() {
        List<MusicGenre> musicGenres = musicGenreService.getAllMusicGenres();
        return new ResponseEntity<>(musicGenres, HttpStatus.OK);
    }

    // READ - Récupérer un genre musical par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMusicGenreById(@PathVariable Long id) {
        Optional<MusicGenre> musicGenre = musicGenreService.getMusicGenreById(id);

        if (musicGenre.isPresent()) {
            return new ResponseEntity<>(musicGenre.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Genre musical non trouvé avec l'ID: " + id, HttpStatus.NOT_FOUND);
        }
    }

    // UPDATE - Mettre à jour un genre musical
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMusicGenre(@PathVariable Long id, @RequestBody MusicGenre musicGenreDetails) {
        try {
            MusicGenre updatedMusicGenre = musicGenreService.updateMusicGenre(id, musicGenreDetails);
            return new ResponseEntity<>(updatedMusicGenre, HttpStatus.OK);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("non trouvé")) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la mise à jour du genre musical", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE - Supprimer un genre musical
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMusicGenre(@PathVariable Long id) {
        try {
            musicGenreService.deleteMusicGenre(id);
            return new ResponseEntity<>("Genre musical supprimé avec succès", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la suppression du genre musical", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}