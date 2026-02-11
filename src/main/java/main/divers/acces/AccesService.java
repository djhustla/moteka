package main.divers.acces;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            acces.setAdresse(accesDetails.getAdresse() );



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

            // 2. Utiliser une regex pour extraire ligne, indice optionnel, BK et BH
            Pattern pattern = Pattern.compile("Ligne\\s+(\\d+)([A-Z]?)\\s+BK\\s+(\\d+)\\.(\\d+)");
            Matcher matcher = pattern.matcher(ligneBkVoie.trim());

            if (!matcher.matches()) {
                throw new IllegalArgumentException("Format invalide. Exemple: 'Ligne 17 BK 1.200' ou 'Ligne 2X BK 4.300'");
            }

            int ligneRecherche;
            String indiceRecherche;
            int bkRecherche;
            int bhRecherche;

            try {
                ligneRecherche = Integer.parseInt(matcher.group(1));     // "2"
                indiceRecherche = matcher.group(2);                       // "X" ou ""
                bkRecherche = Integer.parseInt(matcher.group(3));        // "4"
                bhRecherche = Integer.parseInt(matcher.group(4));        // "300"
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Format numérique invalide");
            }

            // 3. Calculer la valeur de recherche
            int valeurRecherche = bkRecherche * 1000 + bhRecherche; // 4×1000 + 300 = 4300

            // 4. Récupérer tous les accès de la même ligne (en fonction de l'indice)
            List<Acces> accesMemeLigne = accesRepository.findAll().stream()
                    .filter(acces -> {
                        if (acces.getLigneBkVoie() == null) return false;

                        // Utiliser une regex similaire pour parser l'accès existant
                        Pattern accesPattern = Pattern.compile("Ligne\\s+(\\d+)([A-Z]?)\\s+BK\\s+(\\d+)\\.(\\d+)");
                        Matcher accesMatcher = accesPattern.matcher(acces.getLigneBkVoie().trim());

                        if (!accesMatcher.matches()) return false;

                        try {
                            int ligneAcces = Integer.parseInt(accesMatcher.group(1));
                            String indiceAcces = accesMatcher.group(2);

                            // Si la ligne ne correspond pas, on élimine
                            if (ligneAcces != ligneRecherche) {
                                return false;
                            }

                            // LOGIQUE STRICTE :
                            // - Si recherche sans indice (ex: "Ligne 4") → seulement accès sans indice
                            // - Si recherche avec indice (ex: "Ligne 5C") → seulement accès avec cet indice exact
                            if (indiceRecherche.isEmpty()) {
                                // Recherche sans indice → on veut seulement les accès sans indice
                                return indiceAcces.isEmpty();
                            } else {
                                // Recherche avec indice → on veut seulement les accès avec cet indice exact
                                return indiceRecherche.equals(indiceAcces);
                            }

                        } catch (NumberFormatException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (accesMemeLigne.isEmpty()) {
                return null;
            }

            // 5. Trouver l'accès le plus proche
            Acces accesPlusProche = null;
            int differenceMinimale = Integer.MAX_VALUE;

            for (Acces acces : accesMemeLigne) {
                try {
                    Pattern accesPattern = Pattern.compile("Ligne\\s+\\d+[A-Z]?\\s+BK\\s+(\\d+)\\.(\\d+)");
                    Matcher accesMatcher = accesPattern.matcher(acces.getLigneBkVoie().trim());

                    if (!accesMatcher.matches()) continue;

                    int bkAcces = Integer.parseInt(accesMatcher.group(1));
                    int bhAcces = Integer.parseInt(accesMatcher.group(2));
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


}