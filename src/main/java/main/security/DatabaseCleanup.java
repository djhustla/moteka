package main.security;

import java.sql.*;

public class DatabaseCleanup {

    private static final String DB_URL = "jdbc:postgresql://ep-misty-brook-ag3lmvlw-pooler.c-2.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require";
    private static final String DB_USER = "neondb_owner";
    private static final String DB_PASSWORD = "npg_ArLb5gdDm0Gc";

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connexion à la BD réussie\n");

            dropAllData(conn);

            conn.close();
            System.out.println("\nConnexion fermée");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trouvé");
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    private static void dropAllData(Connection conn) throws SQLException {
        conn.setAutoCommit(false);

        try {
            System.out.println("Suppression de toutes les données...\n");

            int count1 = execute(conn, "DELETE FROM messages_prives");
            System.out.println("- " + count1 + " messages privés supprimés");

            int count2 = execute(conn, "DELETE FROM messages");
            System.out.println("- " + count2 + " messages supprimés");

            int count3 = execute(conn, "DELETE FROM conversations");
            System.out.println("- " + count3 + " conversations supprimées");

            int count4 = execute(conn, "DELETE FROM users");
            System.out.println("- " + count4 + " utilisateurs supprimés");

            conn.commit();
            System.out.println("\nSuppression terminée avec succès");

        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    private static int execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate();
        }
    }
}