package main.divers_services.conversionsSons;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;

public class AiffToMp3Converter {

    /**
     * Convertit un fichier AIFF en MP3
     * @param aiffFilePath Chemin absolu du fichier AIFF source
     * @param outputDirPath Chemin absolu du dossier de destination
     */
    public static void convertirAiffToMp3(String aiffFilePath, String outputDirPath) {
        try {
            // Vérifier que le fichier AIFF existe
            File aiffFile = new File(aiffFilePath);
            if (!aiffFile.exists() || !aiffFile.isFile()) {
                System.err.println("Erreur : Le fichier AIFF n'existe pas : " + aiffFilePath);
                return;
            }

            // Créer le dossier de destination s'il n'existe pas
            File outputDir = new File(outputDirPath);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (created) {
                    System.out.println("Dossier créé : " + outputDirPath);
                } else {
                    System.err.println("Erreur : Impossible de créer le dossier : " + outputDirPath);
                    return;
                }
            }

            // Extraire le nom du fichier sans extension
            String fileName = aiffFile.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

            // Créer le chemin du fichier MP3 de sortie
            String mp3FilePath = outputDirPath + File.separator + baseName + ".mp3";

            // Étape 1 : Convertir AIFF en WAV temporaire
            String tempWavPath = outputDirPath + File.separator + baseName + "_temp.wav";
            convertAiffToWav(aiffFilePath, tempWavPath);

            // Étape 2 : Convertir WAV en MP3 avec FFmpeg
            convertWavToMp3WithFFmpeg(tempWavPath, mp3FilePath);

            // Supprimer le fichier WAV temporaire
            File tempWav = new File(tempWavPath);
            if (tempWav.exists()) {
                tempWav.delete();
                System.out.println("Fichier temporaire supprimé.");
            }

            System.out.println("Conversion réussie : " + mp3FilePath);

        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Convertit AIFF en WAV en utilisant l'API Java Sound
     */
    private static void convertAiffToWav(String aiffPath, String wavPath) throws Exception {
        File aiffFile = new File(aiffPath);
        File wavFile = new File(wavPath);

        AudioInputStream aiffStream = AudioSystem.getAudioInputStream(aiffFile);
        AudioFormat format = aiffStream.getFormat();

        // Conversion en format PCM si nécessaire
        AudioFormat pcmFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getSampleRate(),
                16,
                format.getChannels(),
                format.getChannels() * 2,
                format.getSampleRate(),
                false
        );

        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, aiffStream);
        AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, wavFile);

        pcmStream.close();
        aiffStream.close();

        System.out.println("AIFF converti en WAV temporaire.");
    }

    /**
     * Convertit WAV en MP3 en utilisant FFmpeg
     */
    private static void convertWavToMp3WithFFmpeg(String wavPath, String mp3Path) throws Exception {
        // Commande FFmpeg pour la conversion
        String[] command = {
                "ffmpeg",
                "-i", wavPath,
                "-codec:a", "libmp3lame",
                "-qscale:a", "2",
                "-y",  // Écraser le fichier de sortie s'il existe
                mp3Path
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Lire la sortie de FFmpeg
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("WAV converti en MP3 avec succès.");
        } else {
            throw new Exception("FFmpeg a échoué avec le code : " + exitCode);
        }
    }

    /**
     * Main pour tester la conversion avec interface graphique
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Sélection du fichier AIFF
                JFileChooser aiffChooser = new JFileChooser();
                aiffChooser.setDialogTitle("Sélectionner le fichier AIFF à convertir");
                aiffChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".aiff")
                                || f.getName().toLowerCase().endsWith(".aif");
                    }
                    @Override
                    public String getDescription() {
                        return "Fichiers AIFF (*.aiff, *.aif)";
                    }
                });

                int aiffResult = aiffChooser.showOpenDialog(null);
                if (aiffResult != JFileChooser.APPROVE_OPTION) {
                    System.out.println("Sélection du fichier AIFF annulée.");
                    return;
                }

                String aiffFilePath = aiffChooser.getSelectedFile().getAbsolutePath();
                System.out.println("Fichier AIFF sélectionné : " + aiffFilePath);

                // Sélection du dossier de destination
                JFileChooser dirChooser = new JFileChooser();
                dirChooser.setDialogTitle("Sélectionner le dossier de destination");
                dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int dirResult = dirChooser.showOpenDialog(null);
                if (dirResult != JFileChooser.APPROVE_OPTION) {
                    System.out.println("Sélection du dossier annulée.");
                    return;
                }

                String outputDirPath = dirChooser.getSelectedFile().getAbsolutePath();
                System.out.println("Dossier de destination sélectionné : " + outputDirPath);

                // Lancer la conversion
                System.out.println("\n=== Début de la conversion ===");
                convertirAiffToMp3(aiffFilePath, outputDirPath);
                System.out.println("=== Fin de la conversion ===\n");

                JOptionPane.showMessageDialog(null,
                        "Conversion terminée !\nVérifiez le dossier : " + outputDirPath,
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}