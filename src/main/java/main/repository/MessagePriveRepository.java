package main.repository;

import main.modeles.MessagePrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagePriveRepository extends JpaRepository<MessagePrive, Long> {
    // Tous les messages d'une conversation, triés par date
    List<MessagePrive> findByConversationIdOrderBySentAtAsc(Long conversationId);

    // Messages non lus d'une conversation pour un destinataire
    List<MessagePrive> findByConversationIdAndIsReadFalse(Long conversationId);
}