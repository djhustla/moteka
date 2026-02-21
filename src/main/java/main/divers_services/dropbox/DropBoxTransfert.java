package main.divers_services.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionStartResult;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.function.Consumer;

import static main.divers_services.dropbox.TokenDropBox.getValidAccessToken;

public class DropBoxTransfert
{
    public static String transfertFichierFtp(String filePath, String nomDossier) throws Exception {
        File file = new File(filePath);
        String dropboxPath = "/" + (nomDossier != null && !nomDossier.trim().isEmpty() ? nomDossier + "/" : "") + file.getName();
        String ACCESS_TOKEN = getValidAccessToken();

        System.out.println("=".repeat(70));
        System.out.println("📤 UPLOAD DROPBOX - " + file.getName());
        System.out.println("📁 Destination: /" + (nomDossier != null && !nomDossier.trim().isEmpty() ? nomDossier : "racine"));
        System.out.println("=".repeat(70));

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-uploader").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        try (InputStream in = new FileInputStream(file)) {
            long fileSize = file.length();
            byte[] buffer = new byte[8 * 1024 * 1024];
            int bytesRead = in.read(buffer);

            // Affiche la barre de progression initiale
            System.out.print("⏳ Upload: [");
            for (int i = 0; i < 50; i++) System.out.print("░");
            System.out.print("] 0%");

            long startTime = System.currentTimeMillis();
            UploadSessionStartResult sessionStart = client.files()
                    .uploadSessionStart()
                    .uploadAndFinish(new ByteArrayInputStream(buffer, 0, bytesRead));

            String sessionId = sessionStart.getSessionId();
            long uploaded = bytesRead;
            UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

            // Première mise à jour
            updateConsoleProgressBar(uploaded, fileSize, startTime);

            while (uploaded < fileSize) {
                long remaining = fileSize - uploaded;
                int chunkSize = (int) Math.min(buffer.length, remaining);

                bytesRead = in.read(buffer, 0, chunkSize);
                if (bytesRead == -1) break;

                client.files().uploadSessionAppendV2(cursor)
                        .uploadAndFinish(new ByteArrayInputStream(buffer, 0, bytesRead));

                uploaded += bytesRead;
                cursor = new UploadSessionCursor(sessionId, uploaded);

                // Mise à jour en temps réel
                updateConsoleProgressBar(uploaded, fileSize, startTime);
            }

            // Affiche 100%
            System.out.print("\r✅ Upload: [");
            for (int i = 0; i < 50; i++) System.out.print("█");
            System.out.print("] 100% ");

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;
            System.out.println("(" + formatSize(fileSize) + " en " + duration + "s)");

            // Finalisation
            System.out.println("📋 Finalisation de l'upload...");
            CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath).build();
            FileMetadata metadata = client.files().uploadSessionFinish(cursor, commitInfo).finish();

            SharedLinkMetadata sharedLinkMetadata = client.sharing()
                    .createSharedLinkWithSettings(metadata.getPathLower());

            String lien = sharedLinkMetadata.getUrl().replace("?dl=0", "?dl=1");

            System.out.println("=".repeat(70));
            System.out.println("🔗 LIEN DROPBOX:");
            System.out.println(lien);
            System.out.println("=".repeat(70));

            return lien;

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Méthode pour mettre à jour la barre de progression en console
    private static void updateConsoleProgressBar(long uploaded, long total, long startTime) {
        double percent = (double) uploaded / total;
        int width = 50;
        int progress = (int) (width * percent);

        // Calcul du temps écoulé et vitesse
        long elapsedTime = System.currentTimeMillis() - startTime;
        double speed = (elapsedTime > 0) ? (uploaded / 1024.0 / 1024.0) / (elapsedTime / 1000.0) : 0;

        StringBuilder bar = new StringBuilder();
        bar.append("\r");

        // Choix de l'emoji selon la progression
        if (percent < 0.33) bar.append("⏳ ");
        else if (percent < 0.66) bar.append("📤 ");
        else bar.append("⚡ ");

        bar.append("Upload: [");
        for (int i = 0; i < width; i++) {
            bar.append(i < progress ? "█" : "░");
        }
        bar.append("] ");

        // Pourcentage
        bar.append(String.format("%5.1f%% ", percent * 100));

        // Taille
        bar.append("(").append(formatSize(uploaded)).append(" / ").append(formatSize(total)).append(") ");

        // Vitesse
        bar.append(String.format("%.1f MB/s ", speed));

        // Temps estimé restant
        if (percent > 0.01 && speed > 0) {
            double remainingMB = (total - uploaded) / 1024.0 / 1024.0;
            int remainingSeconds = (int) (remainingMB / speed);
            if (remainingSeconds < 60) {
                bar.append(remainingSeconds).append("s ");
            } else {
                bar.append(remainingSeconds / 60).append("m ");
            }
        }

        System.out.print(bar.toString());
    }

    // Méthode utilitaire pour formatter la taille
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    public static void main(String[] args) {
        try {
            // Chemin du fichier local
            String filePath = "C:\\Users\\Win\\Desktop\\c\\dj hustla - root Warmup.mp3";
            // Dossier destination Dropbox
            String dossierDestination = "live";

            // Appel de la fonction d'upload
            String lienDropbox = transfertFichierFtp(filePath, dossierDestination);

            System.out.println("✅ Upload terminé avec succès !");
            System.out.println("🔗 Lien Dropbox : " + lienDropbox);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'exécution : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
