package main.controlleurs;

import main.modeles.Message;
import main.modeles.MessageDTO;
import main.modeles.User;
import main.repository.UserRepository;
import main.services.MessageService;
import main.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // ✅ Récupérer tous les messages → renvoie DTO
    @GetMapping
    public List<MessageDTO> getAllMessages() {
        return messageService.getAllMessages()
                .stream()
                .map(MessageDTO::new)
                .collect(Collectors.toList());
    }

    // ✅ Récupérer les messages d'un utilisateur → renvoie DTO
    @GetMapping("/user/{userId}")
    public List<MessageDTO> getMessagesByUser(@PathVariable Long userId) {
        return messageService.getMessagesByUserId(userId)
                .stream()
                .map(MessageDTO::new)
                .collect(Collectors.toList());
    }

    // ✅ Créer un message pour un utilisateur → renvoie DTO
    @PostMapping("/user/{userId}")
    public MessageDTO createMessage(@PathVariable Long userId, @RequestBody String intituleMessage) {
        Message message = messageService.createMessage(userId, intituleMessage);
        return new MessageDTO(message);
    }





}
