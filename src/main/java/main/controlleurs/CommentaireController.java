package main.controlleurs;



import main.modeles.Commentaire;
import main.modeles.CommentaireDTO;
import main.modeles.User;
import main.services.CommentaireService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

        import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/commentaires")
public class CommentaireController {

    @Autowired
    private CommentaireService commentaireService;

    @Autowired
    private UserService userService;

    // Ajouter un commentaire à un message
    @PostMapping("/message/{messageId}")
    public ResponseEntity<Map<String, Object>> ajouterCommentaire(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> request) {

        try {
            String texte = request.get("texte");
            if (texte == null || texte.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Le texte du commentaire est requis"));
            }

            // Récupérer l'utilisateur connecté
            String username = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Ajouter le commentaire
            Commentaire commentaire = commentaireService.ajouterCommentaire(
                    currentUser.getId(), messageId, texte);

            // Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commentaire ajouté avec succès");
            response.put("commentaire", new CommentaireDTO(commentaire));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Récupérer tous les commentaires d'un message
    @GetMapping("/message/{messageId}")
    public ResponseEntity<Map<String, Object>> getCommentairesParMessage(
            @PathVariable Long messageId) {

        try {
            List<Commentaire> commentaires = commentaireService.getCommentairesParMessage(messageId);

            List<CommentaireDTO> dtos = commentaires.stream()
                    .map(CommentaireDTO::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("messageId", messageId);
            response.put("totalCommentaires", dtos.size());
            response.put("commentaires", dtos);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Récupérer tous les commentaires de l'utilisateur connecté
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMesCommentaires() {

        try {
            String username = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Commentaire> commentaires = commentaireService
                    .getCommentairesParUtilisateur(currentUser.getId());

            List<CommentaireDTO> dtos = commentaires.stream()
                    .map(CommentaireDTO::new)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("totalCommentaires", dtos.size());
            response.put("commentaires", dtos);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Supprimer un commentaire (seul le propriétaire peut supprimer)
    @DeleteMapping("/{commentaireId}")
    public ResponseEntity<Map<String, String>> supprimerCommentaire(
            @PathVariable Long commentaireId) {

        try {
            String username = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            commentaireService.supprimerCommentaire(commentaireId, currentUser.getId());

            return ResponseEntity.ok(Map.of("message", "Commentaire supprimé avec succès"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Mettre à jour un commentaire
    @PutMapping("/{commentaireId}")
    public ResponseEntity<Map<String, Object>> mettreAJourCommentaire(
            @PathVariable Long commentaireId,
            @RequestBody Map<String, String> request) {

        try {
            String nouveauTexte = request.get("texte");
            if (nouveauTexte == null || nouveauTexte.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Le texte du commentaire est requis"));
            }

            String username = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Commentaire commentaire = commentaireService.mettreAJourCommentaire(
                    commentaireId, currentUser.getId(), nouveauTexte);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commentaire mis à jour avec succès");
            response.put("commentaire", new CommentaireDTO(commentaire));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Compter les commentaires d'un message
    @GetMapping("/message/{messageId}/count")
    public ResponseEntity<Map<String, Object>> compterCommentairesParMessage(
            @PathVariable Long messageId) {

        try {
            int count = commentaireService.compterCommentairesParMessage(messageId);

            Map<String, Object> response = new HashMap<>();
            response.put("messageId", messageId);
            response.put("totalCommentaires", count);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint admin - Supprimer n'importe quel commentaire
    @DeleteMapping("/admin/{commentaireId}")
    public ResponseEntity<Map<String, String>> supprimerCommentaireAdmin(
            @PathVariable Long commentaireId) {

        try {
            commentaireService.supprimerCommentaireAdmin(commentaireId);
            return ResponseEntity.ok(Map.of("message", "Commentaire supprimé par l'administrateur"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
