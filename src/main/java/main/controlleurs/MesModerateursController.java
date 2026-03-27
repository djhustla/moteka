package main.controlleurs;

import main.modeles.MesModerateurs;
import main.services.MesModerateursService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/moderateurs")
public class MesModerateursController {

    @Autowired
    private MesModerateursService service;

    // POST - Créer un modérateur
    @PostMapping
    public MesModerateurs create(@RequestBody MesModerateurs moderateur) {
        return service.save(moderateur);
    }

    // GET - Tout récupérer
    @GetMapping
    public List<MesModerateurs> getAll() {
        return service.findAll();
    }

    // GET - Récupérer par ID
    @GetMapping("/{id}")
    public ResponseEntity<MesModerateurs> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT - Mettre à jour
    @PutMapping("/{id}")
    public ResponseEntity<MesModerateurs> update(@PathVariable Long id, @RequestBody MesModerateurs details) {
        try {
            return ResponseEntity.ok(service.update(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE - Supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}