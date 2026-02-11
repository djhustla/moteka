package main.divers_services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static main.divers_services.ConversionEtTransfertService.conversionEtTransfert;

@RestController
@RequestMapping("/api/conversion")
public class ConversionController {

    @PostMapping("/upload-and-convert")
    public ResponseEntity<String> uploadAndConvert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dossierDropbox") String dossierDropbox) {

        try {
            // 1. Créer un répertoire temporaire
            Path tempDir = Files.createTempDirectory("conversion-");

            // 2. Sauvegarder le fichier uploadé
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".aif")) {
                return ResponseEntity.badRequest().body("Seuls les fichiers .aif sont acceptés");
            }

            Path tempFilePath = tempDir.resolve(originalFileName);
            Files.copy(file.getInputStream(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            // 3. Convertir et transférer
            String fichierAiffPath = tempFilePath.toAbsolutePath().toString();
            conversionEtTransfert(fichierAiffPath, dossierDropbox);

            // 4. Nettoyer les fichiers temporaires
        //    Files.deleteIfExists(tempFilePath);
        //    Files.deleteIfExists(tempDir);

            return ResponseEntity.ok("✅ Conversion et transfert réussis vers Dropbox !");

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("❌ Erreur d'upload: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Erreur de conversion: " + e.getMessage());
        }
    }
}