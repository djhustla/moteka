package main.repository;

import main.modeles.Message;
import main.modeles.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUser(User user); // récupérer messages par utilisateur
}
