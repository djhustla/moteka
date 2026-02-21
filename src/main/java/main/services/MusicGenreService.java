package main.services;

import main.modeles.MusicGenre;
import main.repository.MusicGenreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MusicGenreService {

    @Autowired
    private MusicGenreRepository musicGenreRepository;

    // CREATE - Créer un nouveau genre musical
    public MusicGenre createMusicGenre(MusicGenre musicGenre) {
        if (musicGenreRepository.existsByDescription(musicGenre.getDescription())) {
            throw new RuntimeException("Un genre musical avec cette description existe déjà");
        }
        return musicGenreRepository.save(musicGenre);
    }

    // READ - Récupérer tous les genres musicaux
    public List<MusicGenre> getAllMusicGenres() {
        return musicGenreRepository.findAll();
    }

    // READ - Récupérer un genre musical par son ID
    public Optional<MusicGenre> getMusicGenreById(Long id) {
        return musicGenreRepository.findById(id);
    }

    // UPDATE - Mettre à jour un genre musical
    public MusicGenre updateMusicGenre(Long id, MusicGenre musicGenreDetails) {
        Optional<MusicGenre> optionalMusicGenre = musicGenreRepository.findById(id);

        if (optionalMusicGenre.isPresent()) {
            MusicGenre existingMusicGenre = optionalMusicGenre.get();

            // Vérifier si la nouvelle description n'existe pas déjà (sauf pour l'élément actuel)
            if (!existingMusicGenre.getDescription().equals(musicGenreDetails.getDescription()) &&
                    musicGenreRepository.existsByDescription(musicGenreDetails.getDescription())) {
                throw new RuntimeException("Un genre musical avec cette description existe déjà");
            }

            existingMusicGenre.setDescription(musicGenreDetails.getDescription());
            return musicGenreRepository.save(existingMusicGenre);
        } else {
            throw new RuntimeException("Genre musical non trouvé avec l'ID: " + id);
        }
    }

    // DELETE - Supprimer un genre musical
    public void deleteMusicGenre(Long id) {
        Optional<MusicGenre> optionalMusicGenre = musicGenreRepository.findById(id);

        if (optionalMusicGenre.isPresent()) {
            musicGenreRepository.deleteById(id);
        } else {
            throw new RuntimeException("Genre musical non trouvé avec l'ID: " + id);
        }
    }

    // Vérifier si un genre musical existe
    public boolean existsById(Long id) {
        return musicGenreRepository.existsById(id);
    }
}