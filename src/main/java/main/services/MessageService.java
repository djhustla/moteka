package main.services;

import main.modeles.Message;
import main.modeles.User;
import main.repository.MessageRepository;
import main.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // Récupérer tous les messages
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    // Récupérer les messages d'un utilisateur
    public List<Message> getMessagesByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(messageRepository::findByUser).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Créer un message
    public Message createMessage(Long userId, String intituleMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Message message = new Message(intituleMessage, user);
        return messageRepository.save(message);
    }
}
