package main;

import main.divers_services.EnvoiSms;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SecurityLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityLearningApplication.class, args);
    }

    @Bean
    public CommandLineRunner testSms() {
        return args -> {
            System.out.println("--- Démarrage du test d'envoi SMS ---");

            // Appelle ta méthode statique
            // Assure-toi que le numéro est au format international (ex: +32...)
            EnvoiSms.envoyerSms("+32498645488", "Le serveur Spring Boot est en ligne !");

            System.out.println("--- Fin du test d'envoi SMS ---");
        };
    }
}