package main.divers.rangementOutils.services;
import main.divers.rangementOutils.modeles.Outil;
import main.divers.rangementOutils.repository.OutilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class OutilService {
    @Autowired
    private OutilRepository outilRepository;
    public Outil create(String nom, String description) {
        if (outilRepository.findByNom(nom).isPresent()) throw new RuntimeException("Nom deja utilise");
        return outilRepository.save(new Outil(nom, description));
    }
    public List<Outil> getAll() { return outilRepository.findAll(); }
    public Optional<Outil> getById(Long id) { return outilRepository.findById(id); }
    public Optional<Outil> getByNom(String nom) { return outilRepository.findByNom(nom); }
    public List<Outil> searchByNom(String nom) { return outilRepository.findByNomContainingIgnoreCase(nom); }
    public Outil update(Long id, String nom, String description) {
        Outil o = outilRepository.findById(id).orElseThrow(); o.setNom(nom); o.setDescription(description); return outilRepository.save(o);
    }
    public void deleteAll() { outilRepository.deleteAll(); }
    public void deleteById(Long id) { outilRepository.deleteById(id); }
}
