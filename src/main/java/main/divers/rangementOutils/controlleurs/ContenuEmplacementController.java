package main.divers.rangementOutils.controlleurs;
import main.divers.rangementOutils.modeles.ContenuEmplacement;
import main.divers.rangementOutils.services.ContenuEmplacementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/contenu-emplacements")
public class ContenuEmplacementController {
    @Autowired private ContenuEmplacementService service;
    @PostMapping
    public ResponseEntity<?> create(@RequestParam List<Long> outilIds, @RequestParam Long emplacementId, @RequestParam Long conteneurId) {
        try { return ResponseEntity.ok(service.create(outilIds, emplacementId, conteneurId)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
    @GetMapping
    public ResponseEntity<?> getAll() { return ResponseEntity.ok(service.getAll()); }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return service.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestParam List<Long> outilIds, @RequestParam Long emplacementId, @RequestParam Long conteneurId) {
        try { return ResponseEntity.ok(service.update(id, outilIds, emplacementId, conteneurId)); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteAll() { service.deleteAll(); return ResponseEntity.ok(Map.of("message", "Supprimes")); }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try { service.deleteById(id); return ResponseEntity.ok(Map.of("message", "Supprime")); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
}
