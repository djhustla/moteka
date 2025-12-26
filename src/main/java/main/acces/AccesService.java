package main.acces;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@Service
public class AccesService {

    private static final Logger logger = LoggerFactory.getLogger("");

    @Autowired
    private AccesRepository accesRepository;

    // Création
    @Transactional
    public Acces createAcces(Acces acces) {
        try {
            if (acces.getNomAcces() == null || acces.getNomAcces().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom d'accès est requis");
            }

            if (accesRepository.existsByNomAcces(acces.getNomAcces())) {
                throw new IllegalArgumentException("Un accès avec ce nom existe déjà");
            }

            return accesRepository.save(acces);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'accès: " + e.getMessage(), e);
        }
    }

    // Lecture de tous les accès
    public List<Acces> getAllAcces() {
        try {
            return accesRepository.findAll();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération des accès: " + e.getMessage(), e);
        }
    }

    // Lecture par ID
    public Acces getAccesById(Long id) {
        try {
            return accesRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Accès non trouvé avec l'ID: " + id));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la récupération de l'accès: " + e.getMessage(), e);
        }
    }

    // Lecture par nom d'accès - CHANGÉ POUR RETOURNER LISTE
    public List<Acces> getAccesByNom(String nomAcces) {
        try {
            return accesRepository.findByNomAccesContainingIgnoreCase(nomAcces);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche par nom: " + e.getMessage(), e);
        }
    }

    // Lecture par ligneBkVoie
    public List<Acces> getAccesByLigneBkVoie(String ligneBkVoie) {
        try {
            return accesRepository.findByLigneBkVoieContainingIgnoreCase(ligneBkVoie);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche par LigneBkVoie: " + e.getMessage(), e);
        }
    }

    // Mise à jour
    @Transactional
    public Acces updateAcces(Long id, Acces accesDetails) {
        try {
            Acces acces = getAccesById(id);

            if (!acces.getNomAcces().equals(accesDetails.getNomAcces()) &&
                    accesRepository.existsByNomAcces(accesDetails.getNomAcces())) {
                throw new IllegalArgumentException("Un accès avec ce nom existe déjà");
            }

            acces.setNomAcces(accesDetails.getNomAcces());
            acces.setLigneBkVoie(accesDetails.getLigneBkVoie());
            acces.setDescription(accesDetails.getDescription());
            acces.setLatitude(accesDetails.getLatitude());
            acces.setLongitude(accesDetails.getLongitude());

            return accesRepository.save(acces);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'accès: " + e.getMessage(), e);
        }
    }

    // Suppression
    @Transactional
    public void deleteAcces(Long id) {
        try {
            Acces acces = getAccesById(id);
            accesRepository.delete(acces);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de l'accès: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAllAcces() {
        try {
            logger.info("Suppression de tous les accès...");
            long countBefore = accesRepository.count();

            // Supprime tous les enregistrements
            accesRepository.deleteAll();

            long countAfter = accesRepository.count();
            logger.info("Suppression terminée. {} accès supprimés.", countBefore - countAfter);

        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de tous les accès: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la suppression de tous les accès: " + e.getMessage(), e);
        }
    }

    // Recherche par description
    public List<Acces> getAccesByDescription(String description) {
        try {
            return accesRepository.findByDescriptionContainingIgnoreCase(description);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche par description: " + e.getMessage(), e);
        }
    }

    public Acces rechercheApproximative(String ligneBkVoie) {
        try {
            // 1. Validation basique
            if (ligneBkVoie == null || ligneBkVoie.trim().isEmpty()) {
                throw new IllegalArgumentException("La ligneBkVoie ne peut pas être vide");
            }

            // 2. Extraire ligne, BK, BH du paramètre (ex: "Ligne 17 BK 1.200")
            String[] parties = ligneBkVoie.split(" ");
            if (parties.length < 4) {
                throw new IllegalArgumentException("Format invalide. Exemple: 'Ligne 17 BK 1.200'");
            }

            int ligneRecherche;
            try {
                ligneRecherche = Integer.parseInt(parties[1]); // "17"
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Numéro de ligne invalide");
            }

            // 3. Extraire BK et BH (ex: "1.200")
            String[] bkBh = parties[3].split("\\.");
            if (bkBh.length < 2) {
                throw new IllegalArgumentException("Format BK.BH invalide. Exemple: '1.200'");
            }

            int bkRecherche;
            int bhRecherche;
            try {
                bkRecherche = Integer.parseInt(bkBh[0]); // "1"
                bhRecherche = Integer.parseInt(bkBh[1]); // "200"
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("BK ou BH invalide");
            }

            // 4. Calculer la valeur de recherche
            int valeurRecherche = bkRecherche * 1000 + bhRecherche; // 1×1000 + 200 = 1200

            // 5. Récupérer tous les accès de la même ligne
            List<Acces> accesMemeLigne = accesRepository.findAll().stream()
                    .filter(acces -> {
                        if (acces.getLigneBkVoie() == null) return false;
                        // Vérifier si c'est la même ligne
                        String[] accesParties = acces.getLigneBkVoie().split(" ");
                        if (accesParties.length < 2) return false;
                        try {
                            int ligneAcces = Integer.parseInt(accesParties[1]);
                            return ligneAcces == ligneRecherche;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (accesMemeLigne.isEmpty()) {
                return null;
            }

            // 6. Trouver l'accès le plus proche
            Acces accesPlusProche = null;
            int differenceMinimale = Integer.MAX_VALUE;

            for (Acces acces : accesMemeLigne) {
                try {
                    String[] accesParties = acces.getLigneBkVoie().split(" ");
                    if (accesParties.length < 4) continue;

                    String[] accesBkBh = accesParties[3].split("\\.");
                    if (accesBkBh.length < 2) continue;

                    int bkAcces = Integer.parseInt(accesBkBh[0]);
                    int bhAcces = Integer.parseInt(accesBkBh[1]);
                    int valeurAcces = bkAcces * 1000 + bhAcces;

                    int difference = Math.abs(valeurAcces - valeurRecherche);

                    if (difference < differenceMinimale) {
                        differenceMinimale = difference;
                        accesPlusProche = acces;
                    }

                } catch (NumberFormatException e) {
                    // Ignorer les accès avec format invalide
                    continue;
                }
            }

            return accesPlusProche;

        } catch (IllegalArgumentException e) {
            // Relancer pour que le contrôleur gère
            throw e;
        } catch (Exception e) {
            // Log l'erreur
            System.err.println("Erreur inattendue dans rechercheApproximative: " + e.getMessage());
            return null;
        }
    }
    /*
    public Acces rechercheApproximative(String ligneBkVoie) {
        logger.debug("Debug avec paramètres: {} et {}");
        System.err.print("vide0----------------------------");
        try {
            // 1. Valider le paramètre d'entrée
            if (ligneBkVoie == null || ligneBkVoie.trim().isEmpty()) {
                throw new IllegalArgumentException("La ligneBkVoie ne peut pas être vide");
            }

            // 2. Extraire le numéro de ligne (tout avant "BK")
            String ligneNumero = extraireNumeroLigne(ligneBkVoie);
            if (ligneNumero == null) {
                return null; // Format invalide
            }

            // 3. Rechercher tous les accès qui commencent par ce numéro de ligne
            List<Acces> accesMemeLigne = accesRepository.findByLigneBkVoieContainingIgnoreCase("ligne " + ligneNumero + " BK");

            if (accesMemeLigne.isEmpty()) {
                System.err.println("vide1----------------------------");
                return null; // Aucun accès trouvé pour cette ligne
            }

            // 4. Extraire la valeur BK cible du paramètre
            Double valeurCible = extraireValeurBK(ligneBkVoie);
            if (valeurCible == null) {
                System.err.println("vide2----------------------------");
                return accesMemeLigne.get(0); // Retourne le premier si impossible d'extraire la valeur
            }

            // 5. Trouver l'accès avec la valeur BK la plus proche
            Acces accesPlusProche = null;
            Double differenceMinimale = Double.MAX_VALUE;

            System.err.println("vide3----------------------------");

            for (Acces acces : accesMemeLigne) {
                Double valeurAcces = extraireValeurBK(acces.getLigneBkVoie());
                if (valeurAcces != null) {
                    double difference = Math.abs(valeurAcces - valeurCible);
                    if (difference < differenceMinimale) {
                        differenceMinimale = difference;
                        accesPlusProche = acces;
                    }
                }
                System.err.println("vide4----------------------------");
            }

            return accesPlusProche;

        } catch (Exception e) {
            // Log l'erreur (vous pouvez utiliser un logger ici)
            System.err.println("Erreur dans rechercheApproximative: " + e.getMessage());
            return null;
        }
    }

     */

    /**
     * Extrait le numéro de ligne d'une chaîne ligneBkVoie
     */
    /*
    private String extraireNumeroLigne(String ligneBkVoie) {
        try {
            // Format attendu: "ligne X BK Y.ZZZ"
            if (!ligneBkVoie.toLowerCase().contains("ligne") || !ligneBkVoie.toUpperCase().contains("BK")) {
                return null;
            }

            // Extraire tout ce qui se trouve entre "ligne" et "BK"
            String[] parties = ligneBkVoie.split("BK")[0].split("ligne");
            if (parties.length < 2) {
                return null;
            }

            // Nettoyer et extraire le numéro
            String numero = parties[1].trim().split("\\s+")[0];
            return numero;
        } catch (Exception e) {
            return null;
        }
    }

     */

    /**
     * Extrait la valeur numérique après BK
     */
    /*
    private Double extraireValeurBK(String ligneBkVoie) {
        try {
            // Format attendu: "ligne X BK Y.ZZZ"
            if (!ligneBkVoie.toUpperCase().contains("BK")) {
                return null;
            }

            // Prendre la partie après "BK"
            String[] parties = ligneBkVoie.split("BK");
            if (parties.length < 2) {
                return null;
            }

            // Extraire le premier nombre après BK
            String partieNumerique = parties[1].trim();
            String nombre = partieNumerique.split("\\s+")[0];

            return Double.parseDouble(nombre);
        } catch (Exception e) {
            return null;
        }
    }

     */

}