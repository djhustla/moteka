package mesImports;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class fonctionsUtiles
{



    private static void collectFiles(File dir, List<String> fileNames) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Appel récursif pour les sous-dossiers
                collectFiles(file, fileNames);
            } else {
                // Récupération du nom du fichier sans extension
                String name = file.getName();
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    name = name.substring(0, lastDot);
                }
                fileNames.add(name);
            }
        }
    }


    private static void deleteMatchingFiles(File dir, List<String> targets) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // On descend récursivement dans les sous-dossiers
                deleteMatchingFiles(file, targets);
            } else {
                // On récupère le nom du fichier sans extension
                String name = file.getName();
                int lastDot = name.lastIndexOf('.');
                if (lastDot > 0) {
                    name = name.substring(0, lastDot);
                }

                // Si le nom figure dans la liste B → on supprime
                if (targets.contains(name)) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Fichier supprimé : " + file.getAbsolutePath());
                    } else {
                        System.err.println("Échec suppression : " + file.getAbsolutePath());
                    }
                }
            }
        }
    }


    public static void filtrerDossiersParListe(String folderPath, List<String> fileNamesWithoutExt) {
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Le chemin fourni n'est pas un dossier valide : " + folderPath);
            return;
        }

        deleteMatchingFiles(folder, fileNamesWithoutExt);
    }

    public static List<String> listFichiersDossiers(String folderPath) {
        List<String> fileNames = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Le chemin fourni n'est pas un dossier valide : " + folderPath);
            return fileNames;
        }

        collectFiles(folder, fileNames);
        return fileNames;
    }


    private static void parcourirEtEcrire(File dossier, BufferedWriter writer) throws IOException {
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) return;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                // Si c'est un dossier, on appelle récursivement la fonction
                parcourirEtEcrire(fichier, writer);
            } else {
                // On ne prend que les fichiers texte : extensions autorisées
                if (isTexte(fichier)) {
                    // Écrire l'en-tête avec le nom et le chemin complet
                    writer.write("===== " + fichier.getName() + " | " + fichier.getAbsolutePath() + " =====");
                    writer.newLine();

                    // Lire le fichier et écrire son contenu
                    try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
                        String ligne;
                        while ((ligne = br.readLine()) != null) {
                            writer.write(ligne);
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        writer.write("[ERREUR] Impossible de lire le fichier : " + fichier.getAbsolutePath());
                        writer.newLine();
                    }

                    writer.newLine();
                    writer.flush(); // On s'assure que tout est bien écrit
                }
            }
        }
    }

    /**
     * Vérifie si le fichier a une extension considérée comme "texte"
     */
    private static boolean isTexte(File fichier) {
        String[] extensions = {".txt", ".java", ".json", ".xml", ".html", ".css", ".js", ".md", ".properties"};
        String nom = fichier.getName().toLowerCase();
        for (String ext : extensions) {
            if (nom.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fonction principale : concatène tous les fichiers texte d'un dossier dans un fichier unique
     */
    public static void concatenerFichiersTexteDossier(String cheminDossier) {
        File dossier = new File(cheminDossier);
        if (!dossier.exists() || !dossier.isDirectory()) {
            System.out.println("Le chemin fourni n'est pas un dossier valide : " + cheminDossier);
            return;
        }

        File fichierSortie = new File("C:\\Users\\Win\\Desktop\\concat_result.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fichierSortie, false))) {
            parcourirEtEcrire(dossier, writer);
            System.out.println("Concaténation terminée. Résultat dans : " + fichierSortie.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
