package mesImports;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class M3UToCrateConverter {

    public static void main(String[] args) {


        String m3uFilePath = "C:\\Users\\Win\\Documents\\New Folder\\crate\\dj hustla%%ajout%%hiphop.m3u";
        File m3uFile = new File(m3uFilePath);

        if (!m3uFile.exists()) {
            System.err.println("Erreur: Le fichier " + m3uFilePath + " n'existe pas");
            System.exit(1);
        }

        try {
            // Lire le fichier M3U
            List<String> tracks = parseM3U(m3uFile);
            System.out.println("Nombre de pistes trouvées: " + tracks.size());

            // Générer le nom du fichier crate (même nom que le M3U)
            String crateName = m3uFile.getName().replaceFirst("[.][^.]+$", "");
            String crateFilePath = m3uFile.getParent() + File.separator + crateName + ".crate";

            // Créer le fichier crate
            createCrateFile(tracks, crateFilePath);

            System.out.println("Fichier crate créé avec succès: " + crateFilePath);

        } catch (IOException e) {
            System.err.println("Erreur lors de la conversion: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> parseM3U(File m3uFile) throws IOException {
        List<String> tracks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(m3uFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Ignorer les lignes vides et les métadonnées
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Convertir les backslashes en forward slashes pour Serato
                String track = line.replace("\\", "/");
                tracks.add(track);
            }
        }

        return tracks;
    }

    private static void createCrateFile(List<String> tracks, String outputPath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // En-tête Serato
        String header = "vrsn81.0/Serato ScratchLive Crate";
        writeUTF16LE(baos, header);

        // Définition des colonnes
        writeColumnDefinitions(baos);

        // Écrire les pistes
        for (String track : tracks) {
            writeTrack(baos, track);
        }

        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(baos.toByteArray());
        }
    }

    private static void writeColumnDefinitions(ByteArrayOutputStream baos) throws IOException {
        // Colonnes: song, artist, bpm, key, album, length, comment
        String[] columns = {"song", "artist", "bpm", "key", "album", "length", "comment"};
        int[] sizes = {500, 500, 48, 48, 500, 500, 500}; // 500 = 0x01F4, 48 = 0x30

        for (int i = 0; i < columns.length; i++) {
            // ovct marker + column name
            writeUTF16LE(baos, "ovct");
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            writeUTF16LE(baos, columns[i]);

            // tvcn marker + size
            writeUTF16LE(baos, "tvcn");
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);

            // Taille de la colonne
            if (sizes[i] == 500) {
                baos.write(0x01);
                baos.write(0x00);
                baos.write(0xF4);
                baos.write(0x00);
            } else { // 48
                baos.write(0x00);
                baos.write(0x00);
                baos.write(0x30);
                baos.write(0x00);
            }
        }
    }

    private static void writeTrack(ByteArrayOutputStream baos, String trackPath) throws IOException {
        // otrk marker
        writeUTF16LE(baos, "otrk");
        baos.write(0x00);
        baos.write(0x00);

        // Longueur du chemin
        int pathLength = trackPath.length();
        if (pathLength > 255) {
            baos.write(0x01);
            baos.write(0x00);
            baos.write(pathLength & 0xFF);
            baos.write((pathLength >> 8) & 0xFF);
        } else {
            baos.write(0x00);
            baos.write(0x00);
            baos.write(pathLength);
            baos.write(0x00);
        }

        // ptrk marker
        writeUTF16LE(baos, "ptrk");
        baos.write(0x00);
        baos.write(0x00);

        // Longueur du chemin (répété)
        if (pathLength > 255) {
            baos.write(0x01);
            baos.write(0x00);
            baos.write(pathLength & 0xFF);
            baos.write((pathLength >> 8) & 0xFF);
        } else {
            baos.write(0x00);
            baos.write(0x00);
            baos.write(pathLength);
            baos.write(0x00);
        }

        // Le chemin lui-même
        writeUTF16LE(baos, trackPath);
    }

    private static void writeUTF16LE(ByteArrayOutputStream baos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_16LE);
        baos.write(bytes);
    }
}