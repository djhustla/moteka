package main.divers_services.dropbox;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dropbox.core.DbxException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dropbox")
public class DropboxController {

    /**
     * Endpoint pour récupérer la liste des liens Dropbox
     * GET: http://localhost:8080/api/dropbox/liens
     */
    @GetMapping("/liens")
    public ResponseEntity<Map<String, Object>> getLiensDropbox() {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🎯 Requête reçue pour les liens Dropbox");

            // Appel à ta méthode qui génère les liens
            List<String> liens = GestionListeDropBox.ListeLiensDossiersDropBoxLiveDossier();

            response.put("success", true);
            response.put("nombreLiens", liens.size());
            response.put("liens", liens);
            response.put("message", "Liens récupérés avec succès");

            System.out.println("✅ " + liens.size() + " liens récupérés");

            return ResponseEntity.ok(response);

        } catch (DbxException e) {
            System.err.println("❌ Erreur Dropbox: " + e.getMessage());

            response.put("success", false);
            response.put("message", "Erreur Dropbox: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "Erreur interne: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint pour vérifier la santé de l'API Dropbox
     * GET: http://localhost:8080/api/dropbox/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test simple de connexion
            List<String> liens = GestionListeDropBox.ListeLiensDossiersDropBoxLiveDossier();

            response.put("status", "UP");
            response.put("service", "Dropbox API");
            response.put("fichiersDisponibles", liens.size());
            response.put("message", "Service Dropbox opérationnel");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("service", "Dropbox API");
            response.put("error", e.getMessage());
            response.put("message", "Service Dropbox non disponible");

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Endpoint pour récupérer le nombre de fichiers
     * GET: http://localhost:8080/api/dropbox/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getFileCount() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> liens = GestionListeDropBox.ListeLiensDossiersDropBoxLiveDossier();

            response.put("success", true);
            response.put("nombreFichiers", liens.size());
            response.put("message", "Nombre de fichiers récupéré avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erreur: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}