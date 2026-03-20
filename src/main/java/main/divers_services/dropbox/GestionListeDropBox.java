package main.divers_services.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.sharing.FileLinkMetadata;
import com.dropbox.core.v2.sharing.FolderLinkMetadata;
import com.dropbox.core.v2.sharing.ListSharedLinksResult;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;

import java.util.*;

import static main.divers_services.dropbox.TokenDropBox.getValidAccessToken;


public class GestionListeDropBox
{

    // Classe auxiliaire pour stocker les fichiers avec leurs dates
    private static class FichierAvecDate {
        private final FileMetadata fichier;
        private final String cheminFichier;
        private final String nomFichier;
        private final Date date;

        public FichierAvecDate(FileMetadata fichier, String cheminFichier, String nomFichier, Date date) {
            this.fichier = fichier;
            this.cheminFichier = cheminFichier;
            this.nomFichier = nomFichier;
            this.date = date;
        }

        public FileMetadata getFichier() {
            return fichier;
        }

        public String getCheminFichier() {
            return cheminFichier;
        }

        public String getNomFichier() {
            return nomFichier;
        }

        public Date getDate() {
            return date;
        }
    }

    // Classe d'information pour la version alternative
    private static class FichierInfo {
        String chemin;
        String nom;
        FileMetadata metadata;

        FichierInfo(String chemin, String nom, FileMetadata metadata) {
            this.chemin = chemin;
            this.nom = nom;
            this.metadata = metadata;
        }
    }


    private static String extraireCheminDepuisLienPublic(DbxClientV2 client, String lienPublic) throws DbxException {
        try {
            SharedLinkMetadata metadata = client.sharing().getSharedLinkMetadata(lienPublic);
            if (metadata instanceof FolderLinkMetadata) {
                return ((FolderLinkMetadata) metadata).getPathLower();
            } else if (metadata instanceof FileLinkMetadata) {
                return ((FileLinkMetadata) metadata).getPathLower();
            }
        } catch (Exception e) {
            System.err.println("Impossible d'extraire le chemin: " + e.getMessage());
        }
        return "/";
    }

    private static String obtenirOuCreerLienPartage(DbxClientV2 client, String cheminFichierDropbox) throws DbxException {
        try {
            ListSharedLinksResult liensExistants = client.sharing().listSharedLinksBuilder()
                    .withPath(cheminFichierDropbox)
                    .withDirectOnly(true)
                    .start();
            if (!liensExistants.getLinks().isEmpty()) {
                return liensExistants.getLinks().get(0).getUrl().replace("?dl=0", "?raw=1");
            }
        } catch (Exception e) {
            // Aucun lien trouvé, continuer
        }

        try {
            SharedLinkMetadata nouveauLien = client.sharing().createSharedLinkWithSettings(cheminFichierDropbox);
            return nouveauLien.getUrl().replace("?dl=0", "?raw=1");
        } catch (Exception e) {
            ListSharedLinksResult liens = client.sharing().listSharedLinksBuilder()
                    .withPath(cheminFichierDropbox)
                    .start();
            if (!liens.getLinks().isEmpty()) {
                return liens.getLinks().get(0).getUrl().replace("?dl=0", "?raw=1");
            }
            throw e;
        }
    }




    /*Cette fonction ,avec un token d'accès, liste tous les fichiers d'un dossier public */
    public static List<String> ListeLiensDossiersDropBox(  String lienPublicDossier , String accessToken) throws DbxException
    {

        List<String> liensFichiers = new ArrayList<>();

        DbxRequestConfig config = DbxRequestConfig.newBuilder("DropboxListerApp/1.0").build();
        DbxClientV2 client = new DbxClientV2(config, accessToken);

        System.out.println("📂 Connexion à Dropbox...");

        String cheminDossier = extraireCheminDepuisLienPublic(client, lienPublicDossier);
        System.out.println("📁 Chemin du dossier: " + cheminDossier);

        ListFolderResult result = client.files().listFolderBuilder(cheminDossier)
                .withRecursive(false)
                .start();

        while (true) {
            for (Metadata entree : result.getEntries()) {
                if (entree instanceof FileMetadata) {
                    FileMetadata fichier = (FileMetadata) entree;
                    String cheminFichier = fichier.getPathLower();
                    String nomFichier = fichier.getName();

                    try {
                        String lienPartage = obtenirOuCreerLienPartage(client, cheminFichier);
                        liensFichiers.add(lienPartage);
                        System.out.println("✅ " + nomFichier + " → " + lienPartage);
                    } catch (DbxException e) {
                        System.err.println("❌ " + nomFichier + ": " + e.getMessage());
                        // Tu peux choisir d'ajouter un message d'erreur ou de sauter le fichier
                        // liensFichiers.add("ERREUR pour " + nomFichier + ": " + e.getMessage());
                    }
                }
            }
            if (!result.getHasMore()) break;
            result = client.files().listFolderContinue(result.getCursor());
        }

        return liensFichiers;
    }


    // First In Last Out
    public static List<String> ListeLiensDossiersDropBoxLiveDossier() throws DbxException {
        String lienPublicDossier = "https://www.dropbox.com/scl/fo/p2uyk3gpy3cbcj8hf3ah5/ALr0jVmJkxdoUckJskrW49A?rlkey=uix0zs0wi9h5fqtz2l7lyidaq&st=e1yb3j39&dl=0";
        String accessToken = "";

        try {
            accessToken = getValidAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder("DropboxListerApp/1.0").build();
        DbxClientV2 client = new DbxClientV2(config, accessToken);

        System.out.println("📂 Connexion à Dropbox...");

        String cheminDossier = extraireCheminDepuisLienPublic(client, lienPublicDossier);
        System.out.println("📁 Chemin du dossier: " + cheminDossier);

        // Liste pour stocker les fichiers avec leur métadonnées
        List<FichierAvecDate> fichiersAvecDates = new ArrayList<>();

        ListFolderResult result = client.files().listFolderBuilder(cheminDossier)
                .withRecursive(false)
                .start();

        while (true) {
            for (Metadata entree : result.getEntries()) {
                if (entree instanceof FileMetadata) {
                    FileMetadata fichier = (FileMetadata) entree;
                    String cheminFichier = fichier.getPathLower();
                    String nomFichier = fichier.getName();
                    Date dateServerModified = fichier.getServerModified();

                    // Créer un objet qui associe le fichier à sa date
                    FichierAvecDate fichierAvecDate = new FichierAvecDate(
                            fichier,
                            cheminFichier,
                            nomFichier,
                            dateServerModified
                    );

                    fichiersAvecDates.add(fichierAvecDate);
                    System.out.println("📄 Fichier trouvé: " + nomFichier +
                            " - Date: " + dateServerModified);
                }
            }
            if (!result.getHasMore()) break;
            result = client.files().listFolderContinue(result.getCursor());
        }

        // Trier par date de modification serveur (du plus récent au plus ancien)
        fichiersAvecDates.sort((f1, f2) -> {
            // Ordre décroissant : le plus récent en premier
            return f2.getDate().compareTo(f1.getDate());
        });

        // Liste pour les liens finaux
        List<String> liensFichiers = new ArrayList<>();

        // Générer les liens partagés dans l'ordre trié
        for (FichierAvecDate fichierAvecDate : fichiersAvecDates) {
            try {
                String lienPartage = obtenirOuCreerLienPartage(client, fichierAvecDate.getCheminFichier());

                // Formater le lien selon votre besoin
                // lienPartage = lienPartage.replace("&dl=0", "&e=1&dl=0");

                liensFichiers.add(lienPartage);

                System.out.println("✅ Fichier " + fichierAvecDate.getNomFichier() +
                        " (modifié: " + fichierAvecDate.getDate() + ") → " + lienPartage);

            } catch (DbxException e) {
                System.err.println("❌ Erreur pour " + fichierAvecDate.getNomFichier() +
                        ": " + e.getMessage());
                // Ajouter un placeholder ou ignorer selon votre besoin
                // liensFichiers.add("ERREUR pour " + fichierAvecDate.getNomFichier());
            }
        }

        System.out.println("🎯 Total de fichiers triés: " + liensFichiers.size() +
                " (du plus récent au plus ancien)");

        return liensFichiers;
    }



    // ordre inversé
    public static List<String> ListeLiensDossiersDropBoxLiveDossierTrieInverse() throws DbxException {
        String lienPublicDossier = "https://www.dropbox.com/scl/fo/p2uyk3gpy3cbcj8hf3ah5/ALr0jVmJkxdoUckJskrW49A?rlkey=uix0zs0wi9h5fqtz2l7lyidaq&st=e1yb3j39&dl=0";
        String accessToken = "";

        try {
            accessToken = getValidAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DbxRequestConfig config = DbxRequestConfig.newBuilder("DropboxListerApp/1.0").build();
        DbxClientV2 client = new DbxClientV2(config, accessToken);

        System.out.println("📂 Connexion à Dropbox pour tri inverse...");

        String cheminDossier = extraireCheminDepuisLienPublic(client, lienPublicDossier);
        System.out.println("📁 Chemin du dossier: " + cheminDossier);

        // Map pour stocker temporairement les fichiers avec leur timestamp
        Map<Long, FichierInfo> fichiersMap = new TreeMap<>(Collections.reverseOrder());

        ListFolderResult result = client.files().listFolderBuilder(cheminDossier)
                .withRecursive(false)
                .start();

        while (true) {
            for (Metadata entree : result.getEntries()) {
                if (entree instanceof FileMetadata) {
                    FileMetadata fichier = (FileMetadata) entree;
                    Date dateServerModified = fichier.getServerModified();
                    long timestamp = dateServerModified.getTime();

                    // Utiliser le timestamp comme clé (gère les doublons avec un offset)
                    while (fichiersMap.containsKey(timestamp)) {
                        timestamp++; // Offset minimal pour éviter les collisions
                    }

                    fichiersMap.put(timestamp, new FichierInfo(
                            fichier.getPathLower(),
                            fichier.getName(),
                            fichier
                    ));

                    System.out.println("📄 " + fichier.getName() +
                            " - Timestamp: " + timestamp);
                }
            }
            if (!result.getHasMore()) break;
            result = client.files().listFolderContinue(result.getCursor());
        }

        // Liste finale des liens dans l'ordre inverse
        List<String> liensFichiers = new ArrayList<>();

        for (FichierInfo info : fichiersMap.values()) {
            try {
                String lienPartage = obtenirOuCreerLienPartage(client, info.chemin);
                // lienPartage = lienPartage.replace("&dl=0", "&e=1&dl=0");

                liensFichiers.add(lienPartage);
                System.out.println("✅ Ajouté: " + info.nom);

            } catch (DbxException e) {
                System.err.println("❌ Impossible de créer le lien pour: " + info.nom);
            }
        }

        System.out.println("🎯 " + liensFichiers.size() + " fichiers triés du plus récent au plus ancien");
        return liensFichiers;
    }









    // ====================================================
    // 4. POINT D'ENTRÉE PRINCIPAL
    // ====================================================

}
