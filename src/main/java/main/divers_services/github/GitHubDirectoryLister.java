package main.divers_services.github;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubDirectoryLister {

    private static final String GITHUB_USERNAME = "djhustla";
    private static final String REPO_NAME = "music-storage";

    // Injection du token via Spring
    private static String githubToken;

    @Value("${github.token}")
    public void setGithubToken(String token) {
        githubToken = token;
    }

    /**
     * Liste le contenu du dossier playlist_list sur GitHub
     */
    public static List<String> listPlaylistFiles() {
        List<String> files = new ArrayList<>();

        // Vérifier que le token est disponible
        if (githubToken == null || githubToken.isEmpty()) {
            System.out.println("❌ Token GitHub non configuré");
            System.out.println("💡 Définissez la variable GITHUB_TOKEN dans votre .env ou variables d'environnement");
            return files;
        }

        try {
            // URL pour le dossier playlist_list
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/playlist_list",
                    GITHUB_USERNAME, REPO_NAME
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();

                System.out.println("📂 Contenu de playlist_list:");

                for (JsonElement element : jsonArray) {
                    String fileName = element.getAsJsonObject().get("name").getAsString();
                    String type = element.getAsJsonObject().get("type").getAsString();

                    // Ne lister que les fichiers
                    if ("file".equals(type)) {
                        // Supprimer l'extension .txt comme dans votre fonction FTP
                        String cleanName = fileName.replace(".txt", "");
                        files.add(cleanName);
                        System.out.println("📄 " + fileName + " → " + cleanName);
                    }
                }
            } else if (response.statusCode() == 404) {
                System.out.println("❌ Le dossier playlist_list n'existe pas encore");
                System.out.println("💡 Créez-le d'abord sur GitHub");
            } else {
                System.out.println("❌ Erreur GitHub: " + response.statusCode());
                System.out.println("Message: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }

        files.remove(".gitkeep");

        return files;
    }

    /**
     * Version générique si vous voulez lister d'autres dossiers
     */
    public static List<String> listDirectory(String folderPath) {
        List<String> files = new ArrayList<>();

        if (githubToken == null || githubToken.isEmpty()) {
            System.out.println("❌ Token GitHub non configuré");
            return files;
        }

        try {
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    GITHUB_USERNAME, REPO_NAME, folderPath
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();

                for (JsonElement element : jsonArray) {
                    String fileName = element.getAsJsonObject().get("name").getAsString();
                    String type = element.getAsJsonObject().get("type").getAsString();

                    if ("file".equals(type)) {
                        files.add(fileName.replace(".txt", ""));
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }

        return files;
    }

    // Méthode utilitaire pour vérifier la configuration
    public static void checkConfiguration() {
        if (githubToken == null || githubToken.isEmpty()) {
            System.out.println("⚠️  Configuration GitHub: Token non défini");
        } else if (githubToken.startsWith("ghp_")) {
            System.out.println("✅ Configuration GitHub: Token détecté");
        } else {
            System.out.println("⚠️  Configuration GitHub: Token peut être invalide");
        }
    }
}