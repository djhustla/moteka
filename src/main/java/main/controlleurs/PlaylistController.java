package main.controlleurs;

import main.divers_services.github.GitHubDirectoryLister;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    /**
     * Endpoint GET pour lister les fichiers dans le dépôt playlist_list
     * URL: GET /api/playlists
     */
    @GetMapping
    public Map<String, Object> getPlaylists() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Appel à votre fonction GitHub


            System.out.println("hoooooooooo");

            List<String> playlists = GitHubDirectoryLister.listPlaylistFiles();


            System.out.println("haaaaaaaaaaaoooooooooo");

            response.put("success", true);
            response.put("playlists", playlists);
            response.put("count", playlists.size());
            response.put("message", "Liste des playlists récupérée avec succès");

        } catch (Exception e) {
            response.put("success", false);
            response.put("playlists", List.of());
            response.put("count", 0);
            response.put("error", "Erreur lors de la récupération des playlists: " + e.getMessage());
        }

        return response;
    }

    /**
     * Endpoint GET pour lister les fichiers d'un dossier spécifique
     * URL: GET /api/playlists/folder/{folderName}
     */
    @GetMapping("/folder/{folderName}")
    public Map<String, Object> getFolderContent(@PathVariable String folderName) {
        Map<String, Object> response = new HashMap<>();

        try {

            List<String> files = GitHubDirectoryLister.listDirectory(folderName);


            response.put("success", true);
            response.put("folder", folderName);
            response.put("files", files);
            response.put("count", files.size());
            response.put("message", "Contenu du dossier '" + folderName + "' récupéré avec succès");

        } catch (Exception e) {
            response.put("success", false);
            response.put("folder", folderName);
            response.put("files", List.of());
            response.put("count", 0);
            response.put("error", "Erreur lors de la récupération du dossier: " + e.getMessage());
        }

        return response;
    }
}

/*

package main.controlleurs;

import main.hebergeurs.infinityfree.FTPDirectoryLister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPlaylists() {
        try {
            List<String> playlistNames = FTPDirectoryLister.listDirectory("mes_playlists");

            // Créer une réponse structurée
            List<Map<String, String>> playlists = playlistNames.stream()
                    .map(name -> {
                        Map<String, String> playlist = new HashMap<>();
                        playlist.put("name", name);
                        playlist.put("url", "https://monappmusique.42web.io/mes_playlists/" + name + ".txt");
                        return playlist;
                    })
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", playlists.size());
            response.put("playlists", playlists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Impossible de charger les playlists");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

 */