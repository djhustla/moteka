package main.divers.rangementOutils.services;
import main.divers.rangementOutils.modeles.Emplacement;
import main.divers.rangementOutils.repository.EmplacementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class EmplacementService {
    @Autowired
    private EmplacementRepository emplacementRepository;
    public Emplacement create(int x, int y) { return emplacementRepository.save(new Emplacement(x, y)); }
    public List<Emplacement> getAll() { return emplacementRepository.findAll(); }
    public Optional<Emplacement> getById(Long id) { return emplacementRepository.findById(id); }
    public Emplacement update(Long id, int x, int y) {
        Emplacement e = emplacementRepository.findById(id).orElseThrow() ; e.setX(x); e.setY(y); return emplacementRepository.save(e);
    }
    public void deleteAll() { emplacementRepository.deleteAll(); }
    public void deleteById(Long id) { emplacementRepository.deleteById(id); }
}
