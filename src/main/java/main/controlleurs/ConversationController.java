package main.controlleurs;

import main.modeles.*;
import main.services.ConversationService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(@RequestBody Map<String, Long> payload) {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Long otherUserId = payload.get("otherUserId");
        if (otherUserId == null) throw new RuntimeException("Utilisateur cible non spécifié");

        Conversation conv = conversationService.getOrCreateConversation(currentUser.getId(), otherUserId);

        // Pas de messages pour l'instant => unreadCount = 0
        ConversationDTO dto = new ConversationDTO(conv, currentUser.getId(), 0);
        return ResponseEntity.ok(dto);
    }



    /*
    // Créer une nouvelle conversation
    @PostMapping
    public ResponseEntity<ConversationDTO> createConversation(@RequestBody Map<String, Long> payload) {
        Long user1Id = payload.get("user1Id");
        Long user2Id = payload.get("user2Id");

        if(user1Id == null || user2Id == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Conversation conv = conversationService.createConversation(user1Id, user2Id);
            return ResponseEntity.ok(new ConversationDTO(conv, user1Id, 0));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

     */


    // Liste des conversations de l'utilisateur connecté
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> getConversations() {
        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Conversation> conversations = conversationService.getUserConversations(currentUser.getId());

        List<ConversationDTO> dtos = conversations.stream()
                .map(conv -> {
                    long unread = conversationService.countUnreadInConversation(conv.getId(), currentUser.getId());
                    return new ConversationDTO(conv, currentUser.getId(), unread);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Messages d'une conversation spécifique
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessagePriveDTO>> getMessages(@PathVariable Long conversationId) {
        List<MessagePrive> messages = conversationService.getMessages(conversationId);

        List<MessagePriveDTO> dtos = messages.stream()
                .map(MessagePriveDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Envoyer un message
    @PostMapping("/send")
    public ResponseEntity<MessagePriveDTO> sendMessage(@RequestBody Map<String, Object> payload) {
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = payload.get("content").toString();

        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User sender = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        MessagePrive message = conversationService.sendMessage(sender.getId(), receiverId, content);
        return ResponseEntity.ok(new MessagePriveDTO(message));
    }

    // Marquer une conversation comme lue
    @PutMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId) {
        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        conversationService.markConversationAsRead(conversationId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    // Nombre total de messages non lus
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User currentUser = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        long count = conversationService.getTotalUnread(currentUser.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}