package main.services;

import main.modeles.Conversation;
import main.modeles.MessagePrive;
import main.modeles.User;
import main.repository.ConversationRepository;
import main.repository.MessagePriveRepository;
import main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConversationService {
    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private MessagePriveRepository messageRepo;

    @Autowired
    private UserRepository userRepo;

    // Créer ou récupérer une conversation entre 2 users
    public Conversation getOrCreateConversation(Long user1Id, Long user2Id) {
        // Chercher dans les deux sens
        List<Conversation> conv1 = conversationRepo.findByUser1IdAndUser2Id(user1Id, user2Id);
        if (!conv1.isEmpty()) {
            return conv1.get(0);
        }

        List<Conversation> conv2 = conversationRepo.findByUser1IdAndUser2Id(user2Id, user1Id);
        if (!conv2.isEmpty()) {
            return conv2.get(0);
        }

        // Si n'existe pas, créer
        User u1 = userRepo.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 1 non trouvé"));
        User u2 = userRepo.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 2 non trouvé"));

        Conversation newConv = new Conversation(u1, u2);
        return conversationRepo.save(newConv);
    }

    // Envoyer un message
    public MessagePrive sendMessage(Long senderId, Long receiverId, String content) {
        Conversation conv = getOrCreateConversation(senderId, receiverId);
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        MessagePrive message = new MessagePrive(conv, sender, content);
        MessagePrive saved = messageRepo.save(message);

        // Mettre à jour la date du dernier message
        conv.setLastMessageAt(new Date());
        conversationRepo.save(conv);

        return saved;
    }

    // Récupérer tous les messages d'une conversation
    public List<MessagePrive> getMessages(Long conversationId) {
        return messageRepo.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    // Marquer les messages comme lus
    public void markConversationAsRead(Long conversationId, Long currentUserId) {
        List<MessagePrive> messages = messageRepo.findByConversationIdOrderBySentAtAsc(conversationId);

        messages.stream()
                .filter(m -> !m.getSender().getId().equals(currentUserId))
                .filter(m -> !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepo.save(m);
                });
    }

    // Récupérer toutes les conversations d'un utilisateur
    public List<Conversation> getUserConversations(Long userId) {
        List<Conversation> asUser1 = conversationRepo.findByUser1IdOrderByLastMessageAtDesc(userId);
        List<Conversation> asUser2 = conversationRepo.findByUser2IdOrderByLastMessageAtDesc(userId);

        // Fusionner et trier par date
        return Stream.concat(asUser1.stream(), asUser2.stream())
                .sorted((c1, c2) -> {
                    Date d1 = c1.getLastMessageAt() != null ? c1.getLastMessageAt() : c1.getCreatedAt();
                    Date d2 = c2.getLastMessageAt() != null ? c2.getLastMessageAt() : c2.getCreatedAt();
                    return d2.compareTo(d1); // Plus récent en premier
                })
                .collect(Collectors.toList());
    }

    // Compter les messages non lus pour un utilisateur dans une conversation
    public long countUnreadInConversation(Long conversationId, Long userId) {
        List<MessagePrive> unread = messageRepo.findByConversationIdAndIsReadFalse(conversationId);
        return unread.stream()
                .filter(m -> !m.getSender().getId().equals(userId))
                .count();
    }

    // Compter tous les messages non lus pour un utilisateur
    public long getTotalUnread(Long userId) {
        List<Conversation> conversations = getUserConversations(userId);
        return conversations.stream()
                .mapToLong(conv -> countUnreadInConversation(conv.getId(), userId))
                .sum();
    }

    // Crée une conversation entre deux utilisateurs (ou récupère si elle existe déjà)
    public Conversation createConversation(Long user1Id, Long user2Id) {
        if(user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("Vous ne pouvez pas créer une conversation avec vous-même.");
        }
        return getOrCreateConversation(user1Id, user2Id);
    }

}