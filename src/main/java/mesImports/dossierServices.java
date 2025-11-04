package mesImports;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//import static MusicServices.poubelle.SpotifySearch.fromDossierToSpotifyIdList;

public class dossierServices {

    public static <T> void enleverDoublons(List<T> liste) {
        Set<T> set = new LinkedHashSet<>(liste); // garde l'ordre et supprime les doublons
        liste.clear(); // on vide la liste originale
        liste.addAll(set); // on recharge sans doublons
    }


    public static List <String> supprimerDoublon(List <String> liste)
    {
        Set<String> setSansDoublons = new LinkedHashSet<>(liste); // garde l'ordre
        List<String> listeSansDoublons = new ArrayList<>(setSansDoublons);
        return listeSansDoublons;

    }

    public static String choisirDossier(String demande) {
        JFileChooser selecteur = new JFileChooser();
        selecteur.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        selecteur.setDialogTitle(demande);

        int resultat = selecteur.showOpenDialog(null);
        if (resultat == JFileChooser.APPROVE_OPTION) {
            File dossier = selecteur.getSelectedFile();
            return dossier.getAbsolutePath();
        }
        return null;
    }

    public static void copierFichier(String sourceFilePath, String destinationDir) throws IOException {
        File sourceFile = new File(sourceFilePath);
        File destinationDirectory = new File(destinationDir);

        // Vérifier si le fichier source existe
        if (!sourceFile.exists()) {
            throw new IOException("Le fichier source n'existe pas : " + sourceFilePath);
        }

        // Vérifier si le répertoire cible existe, sinon le créer
        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdirs()) {
                throw new IOException("Impossible de créer le répertoire cible : " + destinationDir);
            }
        }

        // Créer un fichier cible avec le même nom que le fichier source
        File destinationFile = new File(destinationDirectory, sourceFile.getName());

        // Copier le fichier
        try (FileInputStream inputStream = new FileInputStream(sourceFile);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("Fichier copié avec succès dans : " + destinationFile.getAbsolutePath());
    }

    public static String creationDossier(String dossier) {

        File directory = new File(dossier);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (result) {
                return "Le répertoire a été créé avec succès à : " + dossier;
            } else {
                return "Échec de la création du répertoire.";
            }
        } else {
            return "Le répertoire existe déjà.";
        }
    }

    public static String fichierSelection(String typeFichier, String messageDemande) {
        String cheminFichier = null;

        // Création d'une frame cachée juste pour l'ancrage du JFileChooser
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(0, 0); // rend la frame invisible
        frame.setLocationRelativeTo(null); // centre la fenêtre

        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers " + typeFichier, typeFichier);
        fileChooser.setFileFilter(filter);

        // Affiche la boîte de dialogue avec un texte personnalisé pour le bouton
        int returnValue = fileChooser.showDialog(frame, messageDemande);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            cheminFichier = selectedFile.getAbsolutePath();
        } else {
            System.out.println("Aucun fichier n'a été sélectionné.");
        }

        frame.dispose();
        return cheminFichier;
    }

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

    public static String dateBoiteDeDialogue() {
        // Afficher une boîte de dialogue pour demander à l'utilisateur de saisir une valeur
        String inputValue = JOptionPane.showInputDialog(null,
                "Veuillez entrer la date du mix  :",
                "Saisie de valeur",
                JOptionPane.QUESTION_MESSAGE);

        // Vérifier si l'utilisateur a annulé la saisie ou n'a rien saisi
        if (inputValue != null && !inputValue.isEmpty()) {
            // Afficher la valeur saisie dans une boîte de dialogue
           /* JOptionPane.showMessageDialog(null,
                    "Vous avez saisi : " + inputValue,
                    "Valeur saisie",
                    JOptionPane.INFORMATION_MESSAGE);*/
            return inputValue;

        } else {
            // Si l'utilisateur a annulé ou n'a rien saisi
            JOptionPane.showMessageDialog(null,
                    "Aucune valeur saisie.",
                    "Valeur non saisie",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    public static void compressFolder(String sourceFolderPath  /*, String zipFilePath*/) throws IOException {

        String zipFilePath = sourceFolderPath + ".zip";
        Path sourceFolder = Paths.get(sourceFolderPath);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Files.walk(sourceFolder).forEach(path -> {
                String zipEntryName = sourceFolder.relativize(path).toString().replace("\\", "/");
                if (Files.isDirectory(path)) {
                    if (!zipEntryName.isEmpty()) {
                        try {
                            zipOut.putNextEntry(new ZipEntry(zipEntryName + "/"));
                            zipOut.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        zipOut.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zipOut);
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static String dateString(Date date) {
        String t = "" + date;
        t = t.substring(8, 10) + " " + t.substring(4, 7) + " " + t.substring(t.length() - 4);
        return t;
    }

    public static void afficherListe(List<String> maListe) {
        for (String element : maListe)
            System.out.println(element);
    }

    public static List<String> divisionString(String input, String separateur) {

        String[] parts = input.split(separateur);
        return Arrays.asList(parts);
    }

    public static void renommerFichier(String oldFilePath, String newFilePath) {
        // Créer un objet File pour le fichier actuel
        File oldFile = new File(oldFilePath);

        // Créer un objet File pour le fichier renommé
        File newFile = new File(newFilePath);

        // Vérifier si le fichier existe déjà
        if (!oldFile.exists()) {
            System.out.println("Le fichier spécifié n'existe pas.");
            return;
        }

        // Vérifier si le nouveau nom existe déjà
        if (newFile.exists()) {
            System.out.println("Un fichier avec le nouveau nom existe déjà.");
            return;
        }

        // Renommer le fichier
        boolean success = oldFile.renameTo(newFile);

        if (success) {
            System.out.println("Le fichier a été renommé avec succès !");
        } else {
            System.out.println("Le renommage du fichier a échoué.");
        }
    }

    public static List<String> listeFichiers(String directoryPath) {
        // Créer un objet File pour le répertoire
        File directory = new File(directoryPath);

        // Vérifier si le chemin est bien un répertoire
        if (!directory.isDirectory()) {
            System.out.println("Le chemin spécifié n'est pas un répertoire.");
            return null;
        }

        // Récupérer la liste des fichiers et des sous-répertoires dans ce répertoire
        File[] filesList = directory.listFiles();

        if (filesList != null && filesList.length > 0) {
            System.out.println("Liste des fichiers dans le dossier " + directoryPath + ":");

            List<String> temp = new ArrayList<>();
            for (File file : filesList) {
                temp.add(file.getName());
            }
            return temp;

        } else {
            return null;
        }
    }

    public static List<String> listFichiersDansSousDossiers(String directoryName, String extension) {

        List<String> listeFichier = new ArrayList<>();
        File directory = new File(directoryName);
        File[] fileList = directory.listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    {
                        String temp = file.getAbsolutePath();
                        if (temp.contains(extension))
                            listeFichier.add(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    listeFichier.addAll(listFichiersDansSousDossiers(file.getAbsolutePath(), extension));
                }
            }
        } else {
            System.out.println("Le répertoire est vide ou n'existe pas.");
        }
        return listeFichier;
    }

    public static void renommerTelechargementYoutube(String nomFichier) {
        String temp = new String(nomFichier);
        if (temp.contains("y2mate")) {
            temp = temp.replace("y2mate.com - ", "");
            temp = temp.replace("  ", " - ");

            temp = temp.replace(" Audio", "");
            temp = temp.replace(" Visualizer", "");
            temp = temp.replace(" Official", "");
            temp = temp.replace(" Officiel", "");
            temp = temp.replace(" Video", "");
            temp = temp.replace(" Clip", "");


        }
        if (temp.contains("utomp3.")) {
            temp = temp.replace("utomp3.com - ", "");
            temp = temp.replace("  ", " - ");

            temp = temp.replace(" Audio", "");
            temp = temp.replace(" Visualizer", "");
            temp = temp.replace(" Official", "");
            temp = temp.replace(" Officiel", "");
            temp = temp.replace(" Video", "");
            temp = temp.replace(" Clip", "");


        }
        if (temp.contains(" myfreemp3.vip "))
            temp = temp.replace(" myfreemp3.vip ", "");
        renommerFichier(nomFichier, temp);
    }



    public static String dossierParentRelatif(String nomFichier) {
        File file = new File(nomFichier);

        if (!file.exists()) {
            System.out.println("Le fichier spécifié n'existe pas.");
            return null;
        }

        File parentDirectory = file.getParentFile();
        if (parentDirectory == null) {
            System.out.println("Le fichier est dans le répertoire racine.");
            return null;
        } else {
            String temp = parentDirectory.getAbsolutePath();
            temp = temp.substring(temp.lastIndexOf("\\") + 1);
            return temp;
        }
    }


    // Méthode pour lister les sous-dossiers contenant "bon" à partir d'un chemin sous forme de String
    public static List<String> listerSousDossiersBon(String chemin) {
        // Créer un objet File à partir du chemin
        File repertoire = new File(chemin);
        List<String> liste = new ArrayList<>();

        // Vérifier si le chemin est un répertoire valide
        if (repertoire.exists() && repertoire.isDirectory()) {
            // Récupérer la liste des fichiers et sous-dossiers
            File[] fichiers = repertoire.listFiles();

            // Si le répertoire n'est pas vide ou inaccessible
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.isDirectory()) {

                        if (fichier.getName().contains("bon"))

                            // Ajouter le nom du sous-dossier à la liste si "bon" est dans le nom
                            liste.add(fichier.getAbsolutePath());
                        // Appel récursif pour explorer les sous-dossiers
                        liste.addAll(listerSousDossiersBon(fichier.getAbsolutePath()));
                    }
                }
            }
        } else {
            System.out.println("Le chemin spécifié n'est pas un répertoire valide.");
            return null;
        }

        // Retourner la liste des sous-dossiers contenant "bon"
        return liste;
    }

    public static boolean effacerFichier(String cheminFichier) {
        // Créer un objet File à partir du nom ou chemin du fichier
        File fichier = new File(cheminFichier);

        // Vérifier si le fichier existe et tenter de le supprimer
        if (fichier.exists() /*&& fichier.isFile()*/) {
            return fichier.delete();  // Supprime le fichier et retourne true si la suppression est réussie
        } else {
            System.out.println("Le fichier spécifié n'existe pas ou n'est pas un fichier.");
            return false;
        }
    }

    public static void effacerListFichier(List<String> listeFichier) {
        for (String fichier : listeFichier)
            effacerFichier(fichier);
    }


    public static void fairePause(int nbSecondes) {
        try {
            // Pause de 3 secondes (3000 millisecondes)
            int temp = nbSecondes * 1000;
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // Gérer l'exception si le thread est interrompu pendant le sommeil
            e.printStackTrace();
        }
    }

    // Méthode pour lire un fichier et copier les lignes dans une liste
    public static List<String> lireFichierDansListe(String cheminFichier) {
        List<String> lignes = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(cheminFichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                lignes.add(ligne);  // Ajoute chaque ligne à la liste
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lignes;
    }


    // cette fonction renvoi la liste des fichier MP3 presend dans un dossier
    public static List<String> parcourirDossierMp3(File dossier) {
        List<String> fichiersMp3 = new ArrayList<>();
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) return fichiersMp3;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                fichiersMp3.addAll(parcourirDossierMp3(fichier));
            } else if (fichier.getName().toLowerCase().endsWith(".mp3")) {
                String nomFichier = fichier.getName();
                nomFichier = nomFichier.substring(0, nomFichier.length() - 4); // remove ".mp3"
                nomFichier = nomFichier.replaceFirst("^\\d+\\s+", ""); // remove leading number + space

                fichiersMp3.add(nomFichier);
            }
        }
        return fichiersMp3;
    }

    // renvoi la liste des fichiers dans un dossier (juste dans le dossiers , pas les sous dossiers)
    public static List<String> listerNomsFichiersDansDossiers(String nomDeDossier) {
        List<String> nomsFichiers = new ArrayList<>();
        File dossier = new File(nomDeDossier);

        if (dossier.exists() && dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.isFile()) {
                        nomsFichiers.add(fichier.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("Le dossier spécifié n'existe pas ou n'est pas un dossier.");
        }

        return nomsFichiers;
    }

    // renvoi la liste des fichiers dans un dossier (juste dans le dossiers et fichiers direct, pas les sous dossiers et sous fichiers)
    public static List<String> listerNomsFichiersEtDossiersDansDossiers(String dossierA) {
        List<String> contenu = new ArrayList<>();

        File dossier = new File(dossierA);
        if (dossier.exists() && dossier.isDirectory()) {
            File[] fichiersEtDossiers = dossier.listFiles();
            if (fichiersEtDossiers != null) {
                for (File f : fichiersEtDossiers) {
                    contenu.add(f.getAbsolutePath()); // ajoute seulement le nom (pas le chemin complet)
                }
            }
        } else {
            System.out.println("Le chemin spécifié n'est pas un dossier valide : " + dossierA);
        }

        return contenu;
    }

    public static List<String> trouverFichiersAvecExtension(String cheminDossier, String extension) {
        List<String> fichiersTrouves = new ArrayList<>();
        File dossier = new File(cheminDossier);

        if (!dossier.exists() || !dossier.isDirectory()) {
            System.out.println("Le dossier spécifié est invalide : " + cheminDossier);
            return fichiersTrouves;
        }

        chercherRecursivement(dossier, extension.toLowerCase(), fichiersTrouves);

        /*
        for(String fichier : fichiersTrouves)
        {System.out.println(fichier);}
        */

        return fichiersTrouves;
    }

    private static void chercherRecursivement(File dossier, String extension, List<String> resultats) {
        File[] fichiers = dossier.listFiles();
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isDirectory()) {
                    chercherRecursivement(fichier, extension, resultats);
                } else if (fichier.getName().toLowerCase().endsWith(extension)) {
                    resultats.add(fichier.getAbsolutePath());
                }
            }
        }
    }

    public static String convertirDate(String dateJJMMAAAA) {
        if (dateJJMMAAAA == null || dateJJMMAAAA.length() != 8) {
            throw new IllegalArgumentException("La date doit être au format JJMMAAAA.");
        }

        int jour = Integer.parseInt(dateJJMMAAAA.substring(0, 2));
        int mois = Integer.parseInt(dateJJMMAAAA.substring(2, 4));
        int annee = Integer.parseInt(dateJJMMAAAA.substring(4, 8));

        String[] moisNoms = {
                "Jan", "Fév", "Mars", "Avr", "Mai", "Juin",
                "Juil", "Août", "Sept", "Oct", "Nov", "Déc"
        };

        if (mois < 1 || mois > 12) {
            throw new IllegalArgumentException("Mois invalide.");
        }

        return String.format("%d %s %d", jour, moisNoms[mois - 1], annee);
    }


    public static List<String> getElementsInListe2NotInListe1(List<String> liste01, List<String> liste02) {


        List<String> result = new ArrayList<>();

        for (String element2 : liste02) {
            boolean found = false;
            for (String element1 : liste01) {

                System.out.println(element2 + " ?= " + element1);


                if (element2.equalsIgnoreCase(element1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.add(element2);
            }
        }

        return result;


    }

    public static List<String> fusionnerListes(List<String> liste1, List<String> liste2) {
        List<String> resultat = new ArrayList<>(liste1);  // On copie liste1 dans la nouvelle liste
        resultat.addAll(liste2);                          // On ajoute tous les éléments de liste2
        return resultat;
    }


    private static boolean contientChaine(File fichier, String chaine) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.contains(chaine)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Ignorer les erreurs de lecture (binaire, permissions, etc.)
        }
        return false;
    }


    public static List<String> rechercherFichiersContenantChaine(String dossier, String chaineCaractere) {
        List<String> fichiersCorrespondants = new ArrayList<>();
        File dossierRacine = new File(dossier);

        if (!dossierRacine.exists() || !dossierRacine.isDirectory()) {
            System.err.println("Le chemin spécifié n'est pas un dossier valide.");
            return fichiersCorrespondants;
        }

        // Parcours récursif
        File[] fichiers = dossierRacine.listFiles();
        if (fichiers == null) return fichiersCorrespondants;

        for (File fichier : fichiers) {
            if (fichier.isDirectory()) {
                fichiersCorrespondants.addAll(rechercherFichiersContenantChaine(fichier.getAbsolutePath(), chaineCaractere));
            } else {
                if (contientChaine(fichier, chaineCaractere)) {
                    fichiersCorrespondants.add(fichier.getAbsolutePath());
                }
            }
        }

        return fichiersCorrespondants;
    }


    public static boolean supprimerFichier(String cheminFichier) {
        File fichier = new File(cheminFichier);
        if (fichier.exists()) {
            return fichier.delete();
        } else {
            System.out.println("Le fichier n'existe pas : " + cheminFichier);
            return false;
        }
    }

    private static void supprimerRecursif(File fichier) {
        if (fichier.isDirectory()) {
            File[] fichiers = fichier.listFiles();
            if (fichiers != null) {
                for (File f : fichiers) {
                    supprimerRecursif(f);
                }
            }
        }

        if (!fichier.delete()) {
            System.out.println("Échec de la suppression : " + fichier.getAbsolutePath());
        }
    }

    public static void supprimerDossier(String cheminDossier) {
        File dossier = new File(cheminDossier);

        if (!dossier.exists()) {
            System.out.println("Le dossier n'existe pas : " + cheminDossier);
            return;
        }

        supprimerRecursif(dossier);
        System.out.println("Dossier supprimé : " + cheminDossier);
    }



    public static String nettoyerString(String input) {
        if (input == null) return "";

        // Supprimer le contenu entre parenthèses () et les parenthèses
        String sansParentheses = input.replaceAll("\\s*\\([^)]*\\)", "");

        // Supprimer le contenu entre crochets [] et les crochets
        String sansCrochets = sansParentheses.replaceAll("\\s*\\[[^]]*\\]", "");

        // Supprimer tous les caractères "|"
        String sansBarres = sansCrochets.replace("|", "");

        // Nettoyer les espaces superflus
        return sansBarres.trim().replaceAll(" +", " ");
    }

    public class DateSelector {

        public static String selectDate() {
            LocalDate currentDate = LocalDate.now();
            int currentYear = currentDate.getYear();
            int currentMonth = currentDate.getMonthValue();
            int currentDay = currentDate.getDayOfMonth();

            // Création des listes déroulantes
            JComboBox<Integer> dayBox = new JComboBox<>();
            JComboBox<String> monthBox = new JComboBox<>(new String[]{
                    "01 - Janvier", "02 - Février", "03 - Mars", "04 - Avril",
                    "05 - Mai", "06 - Juin", "07 - Juillet", "08 - Août",
                    "09 - Septembre", "10 - Octobre", "11 - Novembre", "12 - Décembre"
            });

            Integer[] years = new Integer[50];
            for (int i = 0; i < 50; i++) {
                years[i] = currentYear - 25 + i;
            }
            JComboBox<Integer> yearBox = new JComboBox<>(years);

            // Sélection par défaut
            monthBox.setSelectedIndex(currentMonth - 1);
            yearBox.setSelectedItem(currentYear);

            // Remplissage des jours selon mois et année sélectionnés
            Runnable updateDays = () -> {
                int selectedMonth = monthBox.getSelectedIndex() + 1;
                int selectedYear = (Integer) yearBox.getSelectedItem();
                YearMonth ym = YearMonth.of(selectedYear, selectedMonth);
                int maxDays = ym.lengthOfMonth();

                dayBox.removeAllItems();
                for (int i = 1; i <= maxDays; i++) {
                    dayBox.addItem(i);
                }

                // Réajustement du jour courant si possible
                if (currentDay <= maxDays) {
                    dayBox.setSelectedItem(currentDay);
                } else {
                    dayBox.setSelectedItem(maxDays);
                }
            };

            // Mises à jour des jours quand mois ou année change
            monthBox.addActionListener(e -> updateDays.run());
            yearBox.addActionListener(e -> updateDays.run());

            updateDays.run(); // initialisation au lancement

            // Construction du panel
            JPanel panel = new JPanel(new GridLayout(3, 2));
            panel.add(new JLabel("Jour :"));
            panel.add(dayBox);
            panel.add(new JLabel("Mois :"));
            panel.add(monthBox);
            panel.add(new JLabel("Année :"));
            panel.add(yearBox);

            int result = JOptionPane.showConfirmDialog(null, panel, "Sélectionnez une date", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                int year = (Integer) yearBox.getSelectedItem();
                int month = monthBox.getSelectedIndex() + 1;
                int day = (Integer) dayBox.getSelectedItem();
                return String.format("%04d%02d%02d", year, month, day);
            }

            return null; // Annulation
        }


    }


    public static String removeAccents(String input) {
            if (input == null) return null;

            // Normaliser la chaîne pour séparer les accents
            String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

            // Supprimer tous les caractères diacritiques (accents)
            String cleaned = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            return cleaned;
        }














    public static Date stringToDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("Format de date invalide : " + dateString, e);
        }
    }

    public static String dateToString(String inputDate) {
        // Format d'entrée
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

        // Format de sortie en français
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);

        try {
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "❌ Format de date invalide";
        }
    }




}
