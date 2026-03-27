package main.divers.rangementOutils.controlleurs;
import main.divers.rangementOutils.modeles.Conteneur;
import main.divers.rangementOutils.services.ConteneurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/conteneurs")
public class ConteneurController {
    @Autowired private ConteneurService conteneurService;
    @PostMapping
    public ResponseEntity<?> create(@RequestParam String nom, @RequestParam(required=false) String description) {
        try { return ResponseEntity.ok(conteneurService.create(nom, description)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
    @GetMapping
    public ResponseEntity<?> getAll() { return ResponseEntity.ok(conteneurService.getAll()); }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return conteneurService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/nom/{nom}")
    public ResponseEntity<?> getByNom(@PathVariable String nom) {
        return conteneurService.getByNom(nom).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query) { return ResponseEntity.ok(conteneurService.searchByNom(query)); }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestParam String nom, @RequestParam(required=false) String description) {
        try { return ResponseEntity.ok(conteneurService.update(id, nom, description)); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteAll() { conteneurService.deleteAll(); return ResponseEntity.ok(Map.of("message", "Supprimes")); }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try { conteneurService.deleteById(id); return ResponseEntity.ok(Map.of("message", "Supprime")); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
}
