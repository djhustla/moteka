package main.API_music_class;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicRepository extends JpaRepository<Music, Long> {

    List<Music>     findByTitreContainingIgnoreCase(String titre);
    Optional<Music> findByLienYoutube(String lienYoutube);
    List<Music>     findByUserId(Long userId);
}