package main.divers_services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Component
public class EnvoyerMail {

    private static String apiKey;
    private static String apiSecret;
    private static final String API_URL = "https://api.mailjet.com/v3.1/send";

    // Setters pour injecter les valeurs des properties dans les variables static
    @Value("${mailjet.api.key}")
    public void setApiKey(String value) {
        apiKey = value;
    }

    @Value("${mailjet.api.secret}")
    public void setApiSecret(String value) {
        apiSecret = value;
    }

    /**
     * Envoie un email via l'API Mailjet
     * @param destinataire Adresse email du destinataire
     * @param message Contenu du message
     */
    public static void envoyerEmail(String destinataire, String message) {
        try {
            // Construction du JSON selon la documentation Mailjet v3.1
            String jsonBody = String.format("""
                {
                    "Messages": [
                        {
                            "From": {
                                "Email": "djhustla@outlook.fr",
                                "Name": "moteka"
                            },
                            "To": [
                                {
                                    "Email": "%s"
                                }
                            ],
                            "Subject": "code de validation",
                            "TextPart": "%s"
                        }
                    ]
                }
                """, destinataire, message.replace("\"", "\\\"").replace("\n", "\\n"));

            // Encodage de l'authentification Basic (API_KEY:API_SECRET en base64)
            String auth = apiKey + ":" + apiSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            // Création de la requête HTTP
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + encodedAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Envoi de la requête
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Status: " + response.statusCode());
            System.out.println("Response: " + response.body());

            if (response.statusCode() == 200) {
                System.out.println("✅ Email envoyé avec succès à " + destinataire);
            } else {
                System.err.println("❌ Erreur : " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
