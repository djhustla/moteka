package main.repository;

import main.modeles.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    // Conversations où user1 est l'utilisateur
    List<Conversation> findByUser1IdOrderByLastMessageAtDesc(Long userId);

    // Conversations où user2 est l'utilisateur
    List<Conversation> findByUser2IdOrderByLastMessageAtDesc(Long userId);

    // Trouver conversation entre user1 et user2 (ordre 1)
    List<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    // Trouver conversation entre user2 et user1 (ordre inversé)
    List<Conversation> findByUser2IdAndUser1Id(Long user2Id, Long user1Id);
}