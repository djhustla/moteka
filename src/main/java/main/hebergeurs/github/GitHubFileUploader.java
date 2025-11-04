package main.hebergeurs.github;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Programme pour uploader n'importe quel type de fichier sur GitHub et récupérer le lien direct
 */
@Component
public class GitHubFileUploader {

    // Injection de l'environnement Spring pour lire les properties
    @Autowired
    private Environment environment;

    // Variables qui seront initialisées après l'injection
    private String GITHUB_USERNAME;
    private String GITHUB_TOKEN;
    private String REPO_NAME;

    // Initialisation après l'injection
    @PostConstruct
    public void init() {
        this.GITHUB_USERNAME = environment.getProperty("github.username", "djhustla");
        this.GITHUB_TOKEN = environment.getProperty("github.token", "");
        this.REPO_NAME = environment.getProperty("github.repo", "music-storage");

        System.out.println("🔧 Configuration GitHub chargée:");
        System.out.println("🔧 Username: " + GITHUB_USERNAME);
        System.out.println("🔧 Repo: " + REPO_NAME);
        System.out.println("🔧 Token présent: " + (GITHUB_TOKEN != null && !GITHUB_TOKEN.isEmpty()));
    }

    /**
     * Upload n'importe quel type de fichier sur GitHub et retourne le lien direct
     *
     * @param localFilePath Chemin complet du fichier local
     * @param targetFolder Dossier cible sur GitHub (ex: "sons", "images", "documents")
     * @return URL publique du fichier uploadé, ou null en cas d'erreur
     */
    public static String uploadFileGitHub(String localFilePath, String targetFolder) {
        try {
            // 1. Vérifier que le fichier existe
            File file = new File(localFilePath);
            if (!file.exists()) {
                System.err.println("❌ Fichier non trouvé: " + localFilePath);
                return null;
            }

            if (!file.isFile()) {
                System.err.println("❌ Le chemin spécifié n'est pas un fichier: " + localFilePath);
                return null;
            }

            // 2. Obtenir les informations du fichier
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);
            long fileSize = file.length();

            System.out.println("📦 Fichier original: " + fileName);
            System.out.println("📝 Extension: " + (fileExtension.isEmpty() ? "Aucune" : fileExtension));
            System.out.println("📊 Taille: " + formatFileSize(fileSize));

            // 3. Vérifier la limite GitHub (100 MB)
            if (fileSize > 100 * 1024 * 1024) {
                System.err.println("❌ Fichier trop volumineux! GitHub limite à 100 MB");
                return null;
            }

            // 4. Lire et encoder le fichier en Base64
            System.out.println("📖 Lecture du fichier...");
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Content = Base64.getEncoder().encodeToString(fileContent);

            // 5. Générer un nom de fichier unique (timestamp + nom original nettoyé)
            String cleanFileName = cleanFileName(fileName);
            String uniqueFileName = System.currentTimeMillis() + "_" + cleanFileName;
            String remotePath = targetFolder + "/" + uniqueFileName;

            System.out.println("🧹 Fichier nettoyé: " + cleanFileName);
            System.out.println("🚀 Upload vers GitHub: " + remotePath);

            // 6. Construire l'URL de l'API GitHub
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    getInstance().GITHUB_USERNAME, getInstance().REPO_NAME, remotePath
            );

            // 7. Préparer le corps de la requête JSON
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("message", "Upload " + cleanFileName + " (" + fileExtension.toUpperCase() + ")");
            requestBody.addProperty("content", base64Content);

            // 8. Créer la requête HTTP PUT
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + getInstance().GITHUB_TOKEN)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            // 9. Envoyer la requête
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // 10. Vérifier la réponse
            if (response.statusCode() == 201) {
                // Construire l'URL publique du fichier
                String publicUrl = String.format(
                        "https://raw.githubusercontent.com/%s/%s/main/%s",
                        getInstance().GITHUB_USERNAME, getInstance().REPO_NAME, remotePath
                );

                System.out.println("✅ Upload réussi!");
                System.out.println("🔗 URL publique: " + publicUrl);
                return publicUrl;

            } else if (response.statusCode() == 401) {
                System.err.println("❌ Erreur d'authentification - Vérifiez votre token GitHub");
                return null;

            } else if (response.statusCode() == 404) {
                System.err.println("❌ Repository non trouvé - Créez d'abord le repo '" + getInstance().REPO_NAME + "'");
                return null;

            } else {
                System.err.println("❌ Erreur HTTP " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return null;
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur de lecture du fichier: " + e.getMessage());
            e.printStackTrace();
            return null;

        } catch (InterruptedException e) {
            System.err.println("❌ Requête interrompue: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Upload un fichier dans le dossier "sons" par défaut
     * (Méthode de compatibilité avec l'ancien code)
     */
    public static String uploadMp3(String localFilePath) {
        return uploadFileGitHub(localFilePath, "sons");
    }

    // === MÉTHODES UTILITAIRES (inchangées) ===

    /**
     * Nettoyer le nom du fichier : remplacer les espaces par des underscores
     * et supprimer les caractères spéciaux problématiques
     */
    private static String cleanFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "file";
        }

        // Remplacer les espaces par des underscores
        String cleaned = fileName.replaceAll("\\s+", "_");

        // Supprimer les caractères spéciaux problématiques pour les URLs
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "");

        // Limiter la longueur du nom de fichier
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }

        // S'assurer que le nom n'est pas vide après nettoyage
        if (cleaned.isEmpty()) {
            cleaned = "file_" + System.currentTimeMillis();
        }

        return cleaned;
    }

    /**
     * Obtenir l'extension d'un fichier
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Formater la taille du fichier en unités lisibles
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " bytes";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Obtenir le type MIME d'un fichier basé sur son extension
     */
    public static String getMimeType(String fileName) {
        String extension = getFileExtension(fileName);

        switch (extension) {
            // Audio
            case "mp3": return "audio/mpeg";
            case "wav": return "audio/wav";
            case "ogg": return "audio/ogg";
            case "m4a": return "audio/mp4";
            case "flac": return "audio/flac";

            // Images
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "webp": return "image/webp";
            case "svg": return "image/svg+xml";

            // Vidéo
            case "mp4": return "video/mp4";
            case "avi": return "video/x-msvideo";
            case "mov": return "video/quicktime";
            case "webm": return "video/webm";

            // Documents
            case "pdf": return "application/pdf";
            case "txt": return "text/plain";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt": return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            // Archive
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            case "7z": return "application/x-7z-compressed";

            default: return "application/octet-stream";
        }
    }

    /**
     * Vérifier si un fichier existe déjà sur GitHub
     */
    public static boolean fileExists(String remotePath) {
        try {
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    getInstance().GITHUB_USERNAME, getInstance().REPO_NAME, remotePath
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + getInstance().GITHUB_TOKEN)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Supprimer un fichier sur GitHub
     */
    public static boolean deleteFile(String remotePath) {
        try {
            // 1. Récupérer le SHA du fichier (nécessaire pour la suppression)
            String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    getInstance().GITHUB_USERNAME, getInstance().REPO_NAME, remotePath
            );

            HttpClient client = HttpClient.newHttpClient();

            // GET pour récupérer le SHA
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + getInstance().GITHUB_TOKEN)
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(
                    getRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (getResponse.statusCode() != 200) {
                System.err.println("❌ Fichier non trouvé");
                return false;
            }

            // Parser le SHA
            JsonObject json = com.google.gson.JsonParser.parseString(getResponse.body()).getAsJsonObject();
            String sha = json.get("sha").getAsString();

            // 2. Supprimer le fichier
            JsonObject deleteBody = new JsonObject();
            deleteBody.addProperty("message", "Delete " + remotePath);
            deleteBody.addProperty("sha", sha);

            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + getInstance().GITHUB_TOKEN)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(deleteBody.toString()))
                    .build();

            HttpResponse<String> deleteResponse = client.send(
                    deleteRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (deleteResponse.statusCode() == 200) {
                System.out.println("✅ Fichier supprimé: " + remotePath);
                return true;
            } else {
                System.err.println("❌ Erreur suppression: " + deleteResponse.statusCode());
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            return false;
        }
    }

    // === GESTION DE L'INSTANCE POUR ACCÈS STATIQUE ===

    private static GitHubFileUploader instance;

    @Autowired
    public GitHubFileUploader(Environment environment) {
        this.environment = environment;
    }

    private static GitHubFileUploader getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GitHubFileUploader n'est pas encore initialisé par Spring");
        }
        return instance;
    }

    @PostConstruct
    private void registerInstance() {
        GitHubFileUploader.instance = this;
    }

    /**
     * Programme principal - Exemples d'utilisation
     */
    public static void main(String[] args) {
        System.out.println("📁 GitHub File Uploader - Tous types de fichiers\n");

        // Exemple 1: Upload d'un fichier audio avec espaces
        System.out.println("=== EXEMPLE 1: Fichier Audio avec espaces ===");
        String audioUrl = uploadFileGitHub("C:\\Users\\Win\\Desktop\\00 jj.mp3", "sons");
        if (audioUrl != null) {
            System.out.println("✅ Audio: " + audioUrl);
        }

        // Exemple 2: Test avec différents noms de fichiers problématiques
        System.out.println("\n=== EXEMPLE 2: Tests de nettoyage de noms ===");

        String[] testFiles = {
                "mon fichier avec espaces.mp3",
                "fichier-avec-tirets.wav",
                "fichier_avec_underscores.png",
                "fichier@avec#caractères&spéciaux!.pdf",
                "  fichier avec espaces au début et fin  .txt",
                "FICHIER EN MAJUSCULES.MP3"
        };

        for (String testFile : testFiles) {
            String cleaned = cleanFileName(testFile);
            System.out.println("Avant: '" + testFile + "' → Après: '" + cleaned + "'");
        }

        System.out.println("\n🎉 Terminé! Les espaces sont maintenant remplacés par des underscores.");
    }
}