package main.divers.rangementOutils.services;
import main.divers.rangementOutils.modeles.*;
import main.divers.rangementOutils.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class ContenuEmplacementService {
    @Autowired private ContenuEmplacementRepository contenuEmplacementRepository;
    @Autowired private OutilRepository outilRepository;
    @Autowired private EmplacementRepository emplacementRepository;
    @Autowired private ConteneurRepository conteneurRepository;
    public ContenuEmplacement create(List<Long> outilIds, Long emplacementId, Long conteneurId) {
        List<Outil> outils = outilRepository.findAllById(outilIds);
        Emplacement emplacement = emplacementRepository.findById(emplacementId).orElseThrow();
        Conteneur conteneur = conteneurRepository.findById(conteneurId).orElseThrow();
        return contenuEmplacementRepository.save(new ContenuEmplacement(outils, emplacement, conteneur));
    }
    public List<ContenuEmplacement> getAll() { return contenuEmplacementRepository.findAll(); }
    public Optional<ContenuEmplacement> getById(Long id) { return contenuEmplacementRepository.findById(id); }
    public ContenuEmplacement update(Long id, List<Long> outilIds, Long emplacementId, Long conteneurId) {
        ContenuEmplacement c = contenuEmplacementRepository.findById(id).orElseThrow();
        c.setListeOutils(outilRepository.findAllById(outilIds));
        c.setEmplacement(emplacementRepository.findById(emplacementId).orElseThrow());
        c.setConteneur(conteneurRepository.findById(conteneurId).orElseThrow());
        return contenuEmplacementRepository.save(c);
    }
    public void deleteAll() { contenuEmplacementRepository.deleteAll(); }
    public void deleteById(Long id) { contenuEmplacementRepository.deleteById(id); }
}
