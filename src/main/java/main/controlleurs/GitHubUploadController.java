package main.controlleurs;

import main.divers_services.github.GitHubFileUploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/github")
@CrossOrigin(origins = "*") // Autoriser toutes les origines (à ajuster en prod)
public class GitHubUploadController {

    private final GitHubFileUploader gitHubFileUploader;

    @Autowired
    public GitHubUploadController(GitHubFileUploader gitHubFileUploader) {
        this.gitHubFileUploader = gitHubFileUploader;
    }

    /**
     * Endpoint pour uploader un fichier vers GitHub dans le dossier "music-storage"
     *
     * @param file Fichier à uploader (Multipart)
     * @return URL du fichier sur GitHub
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadToGitHub(
            @RequestParam("file") MultipartFile file) {

        try {
            // 1. Validation du fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le fichier est vide"));
            }

            // 2. Vérifier la taille du fichier
            long fileSize = file.getSize();
            if (fileSize > 100 * 1024 * 1024) { // 100 MB
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Le fichier dépasse la limite de 100 Mo"));
            }

            // 3. Information sur le fichier
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String contentType = file.getContentType();

            System.out.println("📁 Upload reçu:");
            System.out.println("📝 Nom original: " + originalFilename);
            System.out.println("📄 Extension: " + fileExtension);
            System.out.println("📊 Taille: " + formatFileSize(fileSize));
            System.out.println("🎵 Type MIME: " + contentType);

            // 4. Sauvegarder temporairement le fichier
            File tempFile = createTempFile(file);

            try {
                // 5. Upload vers GitHub dans le dossier "music-storage"
                String targetFolder = "music-storage"; // Dossier cible fixe
                String gitHubUrl = GitHubFileUploader.uploadFileGitHub(
                        tempFile.getAbsolutePath(),
                        targetFolder
                );

                if (gitHubUrl == null) {
                    // Supprimer le fichier temporaire en cas d'erreur
                    tempFile.delete();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(createErrorResponse("Échec de l'upload vers GitHub"));
                }

                // 6. Créer la réponse
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Fichier uploadé avec succès");
                response.put("url", gitHubUrl);
                response.put("filename", originalFilename);
                response.put("size", fileSize);
                response.put("type", contentType);
                response.put("folder", targetFolder);
                response.put("timestamp", System.currentTimeMillis());

                System.out.println("✅ Upload réussi vers: " + gitHubUrl);

                return ResponseEntity.ok(response);

            } finally {
                // 7. Nettoyer le fichier temporaire
                if (tempFile.exists()) {
                    boolean deleted = tempFile.delete();
                    if (!deleted) {
                        System.err.println("⚠️ Impossible de supprimer le fichier temporaire: " + tempFile.getAbsolutePath());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur lors du traitement du fichier: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur interne: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour uploader spécifiquement des fichiers audio
     * (Compatibilité avec l'ancien code)
     */
    @PostMapping("/upload/audio")
    public ResponseEntity<Map<String, Object>> uploadAudio(
            @RequestParam("audio") MultipartFile file) {

        // Vérifier que c'est bien un fichier audio
        if (!isAudioFile(file)) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Le fichier n'est pas un fichier audio valide"));
        }

        return uploadToGitHub(file);
    }

    /**
     * Endpoint pour vérifier l'état du service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "GitHub File Uploader");
        status.put("status", "active");
        status.put("maxFileSize", "100 MB");
        status.put("targetFolder", "music-storage");
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }

    // ========== MÉTHODES UTILITAIRES PRIVÉES ==========

    /**
     * Créer un fichier temporaire à partir du MultipartFile
     */
    private File createTempFile(MultipartFile multipartFile) throws IOException {
        // Créer un nom de fichier unique pour éviter les conflits
        String originalFilename = multipartFile.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String tempFilename = UUID.randomUUID().toString() +
                (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // Créer un fichier temporaire dans le dossier temporaire du système
        Path tempDir = Files.createTempDirectory("github_upload_");
        File tempFile = new File(tempDir.toFile(), tempFilename);

        // Écrire le contenu du MultipartFile dans le fichier temporaire
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        return tempFile;
    }

    /**
     * Obtenir l'extension d'un fichier
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Formater la taille en unités lisibles
     */
    private String formatFileSize(long size) {
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
     * Créer une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Vérifier si le fichier est un fichier audio
     */
    private boolean isAudioFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        String extension = getFileExtension(filename);

        // Vérifier par type MIME
        if (contentType != null && contentType.startsWith("audio/")) {
            return true;
        }

        // Vérifier par extension
        String[] audioExtensions = {"mp3", "wav", "ogg", "m4a", "flac", "aac", "wma"};
        for (String audioExt : audioExtensions) {
            if (audioExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }
}