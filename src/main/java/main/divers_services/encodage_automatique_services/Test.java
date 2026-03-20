package main.divers_services.encodage_automatique_services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static main.divers_services.encodage_automatique_services.GoogleSearchSimple.getPremierLien;
import static main.divers_services.encodage_automatique_services.GoogleSearchSimple.getRandomUserAgent;
import static main.divers_services.encodage_automatique_services.SearchTermGenerator.copierListeDansFichier;
import static main.divers_services.encodage_automatique_services.SearchTermGenerator.genererListeRecherche;

public class Test {

    public static void main(String[] args) {
        // 1. Liste des artistes à traiter
        List<String> artistes = Arrays.asList("50 cent", "lil wayne", "future", "snoop dogg", "lil durk", "rick ross", "manau");
        List<String> resultatsFinaux = new ArrayList<>();

        Random random = new Random();

        for (String artiste : artistes) {
            // Une identité fixe par artiste pour la discrétion
            String identity = getRandomUserAgent();
            List<String> motsCles = genererListeRecherche(artiste);

            StringBuilder ligneArtiste = new StringBuilder(artiste + " ");

            System.out.println("\n--- Traitement de : " + artiste + " ---");

            for (int i = 0; i < motsCles.size(); i++) {
                try {
                    String itemRecherche = motsCles.get(i);
                    String lien = getPremierLien(itemRecherche, identity);

                    // Filtre : on ignore si contient "topic" ou "tag"
                    if (lien.contains("topic") || lien.contains("tag") || lien.equals("Aucun lien trouvé")) {
                        lien = "NON_TROUVE";
                    }

                    // Construction de la chaîne selon ton format
                    ligneArtiste.append(lien);

                    if (i == 0) {
                        ligneArtiste.append(" | "); // Après Spotify
                    } else if (i < motsCles.size() - 1) {
                        ligneArtiste.append(" ; "); // Entre les réseaux
                    }

                    // Pause de sécurité pour ne pas être banni
                    int pause = 7000 + random.nextInt(7000);
                    Thread.sleep(pause);

                } catch (Exception e) {
                    System.err.println("Erreur pour " + artiste + " : " + e.getMessage());
                }
            }

            // Ajout de la ligne complète à la liste finale
            resultatsFinaux.add(ligneArtiste.toString());
            System.out.println("Ligne générée : " + ligneArtiste.toString());
        }

        copierListeDansFichier(resultatsFinaux,"g:\\test.txt");
        // Affichage final de tous les résultats
        System.out.println("\n================ RESULTATS FINAUX ================");
        for (String res : resultatsFinaux) {
            System.out.println(res);
        }
    }
}