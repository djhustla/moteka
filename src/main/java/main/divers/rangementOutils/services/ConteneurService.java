package main.divers.rangementOutils.services;
import main.divers.rangementOutils.modeles.Conteneur;
import main.divers.rangementOutils.repository.ConteneurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class ConteneurService {
    @Autowired
    private ConteneurRepository conteneurRepository;
    public Conteneur create(String nom, String description) {
        if (conteneurRepository.findByNom(nom).isPresent()) throw new RuntimeException("Nom deja utilise");
        return conteneurRepository.save(new Conteneur(nom, description));
    }
    public List<Conteneur> getAll() { return conteneurRepository.findAll(); }
    public Optional<Conteneur> getById(Long id) { return conteneurRepository.findById(id); }
    public Optional<Conteneur> getByNom(String nom) { return conteneurRepository.findByNom(nom); }
    public List<Conteneur> searchByNom(String nom) { return conteneurRepository.findByNomContainingIgnoreCase(nom); }
    public Conteneur update(Long id, String nom, String description) {
        Conteneur c = conteneurRepository.findById(id).orElseThrow(); c.setNom(nom); c.setDescription(description); return conteneurRepository.save(c);
    }
    public void deleteAll() { conteneurRepository.deleteAll(); }
    public void deleteById(Long id) { conteneurRepository.deleteById(id); }
}
