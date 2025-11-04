// CommentaireService.java
package main.services;

import main.modeles.Commentaire;
import main.modeles.Message;
import main.modeles.User;
import main.repository.CommentaireRepository;
import main.repository.MessageRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentaireService {

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    // Ajouter un commentaire
    @Transactional
    public Commentaire ajouterCommentaire(Long userId, Long messageId, String texte) {
        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        // Vérifier que le message existe
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé avec l'ID: " + messageId));

        // Vérifier que le texte n'est pas vide
        if (texte == null || texte.trim().isEmpty()) {
            throw new RuntimeException("Le texte du commentaire ne peut pas être vide");
        }

        // Créer et sauvegarder le commentaire
        Commentaire commentaire = new Commentaire(user, message, texte.trim());
        return commentaireRepository.save(commentaire);
    }

    // Récupérer tous les commentaires d'un message
    public List<Commentaire> getCommentairesParMessage(Long messageId) {
        if (!messageRepository.existsById(messageId)) {
            throw new RuntimeException("Message non trouvé avec l'ID: " + messageId);
        }

        return commentaireRepository.findByMessageIdOrderByCreatedAtDesc(messageId);
    }

    // Récupérer tous les commentaires d'un utilisateur
    public List<Commentaire> getCommentairesParUtilisateur(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId);
        }

        return commentaireRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Supprimer un commentaire (vérification que l'utilisateur est propriétaire)
    @Transactional
    public void supprimerCommentaire(Long commentaireId, Long userId) {
        Commentaire commentaire = commentaireRepository.findByIdAndUserId(commentaireId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Commentaire non trouvé ou vous n'êtes pas autorisé à le supprimer"));

        commentaireRepository.delete(commentaire);
    }

    // Supprimer un commentaire (version admin - sans vérification de propriété)
    @Transactional
    public void supprimerCommentaireAdmin(Long commentaireId) {
        if (!commentaireRepository.existsById(commentaireId)) {
            throw new RuntimeException("Commentaire non trouvé avec l'ID: " + commentaireId);
        }

        commentaireRepository.deleteById(commentaireId);
    }

    // Compter les commentaires d'un message
    public int compterCommentairesParMessage(Long messageId) {
        return commentaireRepository.countByMessageId(messageId);
    }

    // Récupérer un commentaire par son ID
    public Optional<Commentaire> getCommentaireById(Long commentaireId) {
        return commentaireRepository.findById(commentaireId);
    }

    // Mettre à jour un commentaire
    @Transactional
    public Commentaire mettreAJourCommentaire(Long commentaireId, Long userId, String nouveauTexte) {
        Commentaire commentaire = commentaireRepository.findByIdAndUserId(commentaireId, userId)
                .orElseThrow(() -> new RuntimeException(
                        "Commentaire non trouvé ou vous n'êtes pas autorisé à le modifier"));

        if (nouveauTexte == null || nouveauTexte.trim().isEmpty()) {
            throw new RuntimeException("Le texte du commentaire ne peut pas être vide");
        }

        commentaire.setTexte(nouveauTexte.trim());
        return commentaireRepository.save(commentaire);
    }
}