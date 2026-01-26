// CommentaireRepository.java
package main.repository;

import main.modeles.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    // Trouver tous les commentaires d'un message, triés du plus récent au plus ancien
    List<Commentaire> findByMessageIdOrderByCreatedAtDesc(Long messageId);

    // Trouver tous les commentaires d'un utilisateur
    List<Commentaire> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Compter le nombre de commentaires pour un message
    int countByMessageId(Long messageId);

    // Trouver un commentaire spécifique avec vérification de l'utilisateur
    Optional<Commentaire> findByIdAndUserId(Long id, Long userId);

    // Supprimer tous les commentaires d'un message (utile pour la suppression de message)
    void deleteByMessageId(Long messageId);

    // Supprimer tous les commentaires d'un utilisateur (utile pour la suppression de user)
    void deleteByUserId(Long userId);



}