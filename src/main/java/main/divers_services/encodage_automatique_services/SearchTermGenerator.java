package main.divers_services.encodage_automatique_services;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SearchTermGenerator {

    public static String copierListeDansFichier(List<String> maListe, String monFichier) {
        // Écriture dans le fichier
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(monFichier))) {
            for (String line : maListe) {
                writer.write(line);
                writer.newLine();  // Ajoute une nouvelle ligne après chaque chaîne
            }
            return "Liste copiée dans le fichier " + monFichier;
        } catch (IOException e) {
            e.printStackTrace();
            return "Liste copiée dans le fichier " + monFichier;
        }
    }

    public static List<String> copierFichierDansListe(String cheminDuFichier) {
        List<String> liste = new ArrayList<>();

        try {
            Path chemin = Paths.get(cheminDuFichier);
            liste = Files.readAllLines(chemin);
        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Génère une liste de requêtes pour les réseaux sociaux d'un artiste
     */
    public static List<String> genererListeRecherche(String monString) {
        List<String> recherches = new ArrayList<>();

        // Ajout des différentes plateformes
        recherches.add(monString + " spotify");
        recherches.add(monString + " instagram");
        recherches.add(monString + " tiktok");
        recherches.add(monString + " snapchat");
        recherches.add(monString + " facebook");

        return recherches;
    }


}