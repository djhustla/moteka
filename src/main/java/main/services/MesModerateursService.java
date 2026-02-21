package main.services;

import main.modeles.MesModerateurs;
import main.repository.MesModerateursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MesModerateursService {

    @Autowired
    private MesModerateursRepository repository;

    // Create
    public MesModerateurs save(MesModerateurs moderateur) {
        return repository.save(moderateur);
    }

    // Read All
    public List<MesModerateurs> findAll() {
        return repository.findAll();
    }

    // Read by ID
    public Optional<MesModerateurs> findById(Long id) {
        return repository.findById(id);
    }

    // Update
    public MesModerateurs update(Long id, MesModerateurs details) {
        return repository.findById(id).map(mod -> {
            mod.setUsername(details.getUsername());
            return repository.save(mod);
        }).orElseThrow(() -> new RuntimeException("Modérateur non trouvé avec l'id : " + id));
    }

    // Delete
    public void delete(Long id) {
        repository.deleteById(id);
    }
}