package main.divers.rangementOutils.controlleurs;
import main.divers.rangementOutils.modeles.Emplacement;
import main.divers.rangementOutils.services.EmplacementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/emplacements")
public class EmplacementController {
    @Autowired private EmplacementService emplacementService;
    @PostMapping
    public ResponseEntity<?> create(@RequestParam int x, @RequestParam int y) {
        try { return ResponseEntity.ok(emplacementService.create(x, y)); }
        catch (RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
    @GetMapping
    public ResponseEntity<?> getAll() { return ResponseEntity.ok(emplacementService.getAll()); }
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return emplacementService.getById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestParam int x, @RequestParam int y) {
        try { return ResponseEntity.ok(emplacementService.update(id, x, y)); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteAll() { emplacementService.deleteAll(); return ResponseEntity.ok(Map.of("message", "Supprimes")); }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try { emplacementService.deleteById(id); return ResponseEntity.ok(Map.of("message", "Supprime")); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }
}
