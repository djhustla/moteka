package main.divers.acces;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController


@RequestMapping("/api/acces")
//@CrossOrigin(origins = "*")
public class AccesController {

    @Autowired
    private AccesService accesService;

    @Autowired
    private AccesRepository accesRepository;

    @GetMapping("/lignes-voies")
    public List<String> getListeLignesVoies() {
        return accesRepository.findAllLigneBkVoie();
    }



    @PostMapping
    public ResponseEntity<?> createAcces(@RequestBody Acces acces) {
        try {
            Acces createdAcces = accesService.createAcces(acces);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAcces);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAcces() {
        try {
            List<Acces> accesList = accesService.getAllAcces();
            return ResponseEntity.ok(accesList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccesById(@PathVariable Long id) {
        try {
            Acces acces = accesService.getAccesById(id);
            return ResponseEntity.ok(acces);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/nom/{nomAcces}")
    public ResponseEntity<?> getAccesByNom(@PathVariable String nomAcces) {
        try {
            List<Acces> accesList = accesService.getAccesByNom(nomAcces);
            return ResponseEntity.ok(accesList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/ligne/{ligneBkVoie}")
    public ResponseEntity<?> getAccesByLigneBkVoie(@PathVariable String ligneBkVoie) {
        try {
            List<Acces> accesList = accesService.getAccesByLigneBkVoie(ligneBkVoie);
            return ResponseEntity.ok(accesList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/description/{description}")
    public ResponseEntity<?> getAccesByDescription(@PathVariable String description) {
        try {
            List<Acces> accesList = accesService.getAccesByDescription(description);
            return ResponseEntity.ok(accesList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAcces(@PathVariable Long id, @RequestBody Acces accesDetails) {
        try {
            Acces updatedAcces = accesService.updateAcces(id, accesDetails);
            return ResponseEntity.ok(updatedAcces);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAcces(@PathVariable Long id) {
        try {
            accesService.deleteAcces(id);
            return ResponseEntity.ok().body(createSuccessResponse("Accès supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/recherche-approximative")
    public ResponseEntity<?> rechercheApproximative(@RequestParam String ligneBkVoie) {
        try {
            // Validation simple
            if (ligneBkVoie == null || ligneBkVoie.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"Le paramètre 'ligneBkVoie' est requis\"}");
            }

            // Appel du service
            Acces resultat = accesService.rechercheApproximative(ligneBkVoie);

            if (resultat == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Aucun accès trouvé pour cette ligne\"}");
            }

            return ResponseEntity.ok(resultat);

        } catch (IllegalArgumentException e) {
            // Erreur de validation
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            // Erreur serveur
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"Erreur interne du serveur\"}");
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    // NOUVEAU ENDPOINT : Effacer tous les accès
    // NOUVEAU ENDPOINT : Effacer tous les accès (version sécurisée avec confirmation)
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllAcces(@RequestParam(defaultValue = "false") boolean confirm) {
        try {
            // Vérification de sécurité
            if (!confirm) {
                Map<String, String> warning = new HashMap<>();
                warning.put("warning", "Cette action supprimera TOUS les accès. Pour confirmer, ajoutez ?confirm=true à l'URL");
                warning.put("example", "DELETE /api/acces/all?confirm=true");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(warning);
            }

            // Récupère le nombre d'accès avant suppression
            List<Acces> allAcces = accesService.getAllAcces();
            int count = allAcces.size();

            // Si aucun accès à supprimer
            if (count == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Aucun accès à supprimer");
                response.put("count", 0);
                response.put("deleted", 0);
                return ResponseEntity.ok(response);
            }

            // Log avant suppression (facultatif)
            System.out.println("Suppression de " + count + " accès...");

            // Appelle la méthode optimisée deleteAllAcces() du service
            accesService.deleteAllAcces();

            // Log après suppression (facultatif)
            System.out.println("Suppression terminée.");

            // Retourne le nombre d'accès supprimés
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tous les accès ont été supprimés avec succès");
            response.put("count", count);
            response.put("deleted", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors de la suppression : " + e.getMessage()));
        }
    }



    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
}