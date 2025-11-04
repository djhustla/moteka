package main.services;

import main.modeles.MusicFavoris;
import main.modeles.MusicGenre;
import main.modeles.User;
import main.repository.MusicFavorisRepository;
import main.repository.MusicGenreRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MusicFavorisService {

    @Autowired
    private MusicFavorisRepository musicFavorisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MusicGenreRepository musicGenreRepository;

    // CREATE - Ajouter un favori pour un utilisateur
    public MusicFavoris createMusicFavoris(Long userId, Long musicGenreId, Integer cotePreference) {
        // Vérifier si l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier si le genre musical existe
        MusicGenre musicGenre = musicGenreRepository.findById(musicGenreId)
                .orElseThrow(() -> new RuntimeException("Genre musical non trouvé avec l'ID: " + musicGenreId));

        // Vérifier si le favori existe déjà
        if (musicFavorisRepository.existsByUserIdAndMusicGenreId(userId, musicGenreId)) {
            throw new RuntimeException("Ce genre musical est déjà dans les favoris de l'utilisateur");
        }

        // Valider la cote de préférence
        if (cotePreference == null || cotePreference < 1 || cotePreference > 10) {
            throw new RuntimeException("La cote de préférence doit être entre 1 et 10");
        }

        // Créer et sauvegarder le favori
        MusicFavoris musicFavoris = new MusicFavoris(user, musicGenre, cotePreference);
        return musicFavorisRepository.save(musicFavoris);
    }

    // READ - Récupérer tous les favoris d'un utilisateur
    public List<MusicFavoris> getMusicFavorisByUserId(Long userId) {
        // Vérifier si l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        return musicFavorisRepository.findByUserId(userId);
    }

    // READ - Récupérer un favori spécifique
    public Optional<MusicFavoris> getMusicFavorisById(Long id) {
        return musicFavorisRepository.findById(id);
    }

    // UPDATE - Mettre à jour la cote de préférence d'un favori
    public MusicFavoris updateMusicFavoris(Long userId, Long musicGenreId, Integer nouvelleCote) {
        // Valider la nouvelle cote
        if (nouvelleCote == null || nouvelleCote < 1 || nouvelleCote > 10) {
            throw new RuntimeException("La cote de préférence doit être entre 1 et 10");
        }

        // Trouver le favori existant
        MusicFavoris musicFavoris = musicFavorisRepository.findByUserIdAndMusicGenreId(userId, musicGenreId)
                .orElseThrow(() -> new RuntimeException("Favori non trouvé pour cet utilisateur et genre musical"));

        // Mettre à jour la cote
        musicFavoris.setCotePreference(nouvelleCote);
        return musicFavorisRepository.save(musicFavoris);
    }

    // DELETE - Supprimer un favori spécifique
    public void deleteMusicFavoris(Long userId, Long musicGenreId) {
        // Vérifier si le favori existe
        if (!musicFavorisRepository.existsByUserIdAndMusicGenreId(userId, musicGenreId)) {
            throw new RuntimeException("Favori non trouvé pour cet utilisateur et genre musical");
        }

        musicFavorisRepository.deleteByUserIdAndMusicGenreId(userId, musicGenreId);
    }

    // DELETE - Supprimer tous les favoris d'un utilisateur
    public void deleteAllMusicFavorisByUserId(Long userId) {
        // Vérifier si l'utilisateur existe
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        musicFavorisRepository.deleteByUserId(userId);
    }

    // Vérifier si un favori existe
    public boolean existsByUserIdAndMusicGenreId(Long userId, Long musicGenreId) {
        return musicFavorisRepository.existsByUserIdAndMusicGenreId(userId, musicGenreId);
    }

    // Compter les favoris d'un utilisateur
    public long countByUserId(Long userId) {
        return musicFavorisRepository.countByUserId(userId);
    }
}