package main;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class SecurityLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityLearningApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("=== NETTOYAGE DE LA TABLE LIENS_RESEAUX ===");

            try {
                // Vérifier si la table existe
                String checkTable = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_name = 'liens_reseaux'
                ) as table_exists
                """;

                Boolean tableExists = jdbcTemplate.queryForObject(checkTable, Boolean.class);

                if (Boolean.TRUE.equals(tableExists)) {
                    System.out.println("Table 'liens_reseaux' détectée...");

                    // 1. Supprimer la contrainte FK
                    System.out.println("Suppression de la contrainte de clé étrangère...");
                    try {
                        jdbcTemplate.execute("ALTER TABLE liens_reseaux DROP CONSTRAINT IF EXISTS fk97ojj7i7nwtsfnq12lvhl9rc6");
                        System.out.println("✅ Contrainte supprimée");
                    } catch (Exception e) {
                        System.out.println("ℹ️ Contrainte déjà supprimée ou inexistante");
                    }

                    // 2. Supprimer la table
                    System.out.println("Suppression de la table...");
                    jdbcTemplate.execute("DROP TABLE IF EXISTS liens_reseaux CASCADE");
                    System.out.println("✅ Table 'liens_reseaux' supprimée avec succès !");

                } else {
                    System.out.println("ℹ️ Table 'liens_reseaux' n'existe pas (déjà supprimée)");
                }

            } catch (Exception e) {
                System.out.println("⚠️ Erreur lors du nettoyage: " + e.getMessage());
            }

            System.out.println("=== NETTOYAGE TERMINÉ ===");
        };
    }

}