package main.divers_services.dropbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;

public class TokenDropBox
{
    // ====================================================
    // 1. GESTIONNAIRE DE TOKEN
    // ====================================================

    private static String cachedAccessToken = null;
    private static long tokenExpiryTime = 0;

    // Tes identifiants Dropbox App
    private static final String APP_KEY = "ee8ls3to59pj4n7";
    private static final String APP_SECRET = "0e03idj50kpfjlv";

    // ⚠️ METS TON REFRESH_TOKEN ICI (obtenu en exécutant obtenirRefreshTokenPermanent())
    private static final String REFRESH_TOKEN = "C4iMHwxOYa4AAAAAAAAAAcuoOxJD5RLZjTfMZjng6SGPxIT7KRdI528Lv3ZGbP_w";






    // ====================================================
    // 2. MÉTHODE POUR OBTENIR UN REFRESH_TOKEN
    // ====================================================
/*
    public static void obtenirRefreshTokenPermanent() throws Exception {
        String APP_KEY = "ee8ls3to59pj4n7";
        String APP_SECRET = "0e03idj50kpfjlv";
        String REDIRECT_URI = "http://localhost:8080/callback";

        // URL d'autorisation avec token_access_type=offline pour avoir un refresh_token
        String authUrl = "https://www.dropbox.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=" + URLEncoder.encode(APP_KEY, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&token_access_type=offline";

        System.out.println("🌐 Ouverture du navigateur pour l'authentification...");
        System.out.println("URL: " + authUrl);

        // Ouvrir le navigateur
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(authUrl));
            } catch (Exception e) {
                System.out.println("⚠️ Ouvre manuellement cette URL: " + authUrl);
            }
        }

        // Créer un serveur HTTP local pour recevoir le callback
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new InetSocketAddress(8080), 0);

        final String[] authCode = new String[1];
        final Object lock = new Object();

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String response;

            if (query != null && query.contains("code=")) {
                authCode[0] = query.split("code=")[1].split("&")[0];
                response = "Code reçu ! Tu peux fermer cet onglet.";
                System.out.println("✅ Code d'autorisation reçu");
            } else {
                response = "Erreur: aucun code reçu";
            }

            try {
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
            } finally {
                exchange.close();
                synchronized (lock) {
                    lock.notify();
                }
            }
        });

        server.start();
        System.out.println("⏳ En attente de l'authentification sur http://localhost:8080/callback...");

        // Attendre le callback
        synchronized (lock) {
            try {
                lock.wait(180000); // 3 minutes timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        server.stop(0);

        if (authCode[0] == null) {
            throw new IOException("Aucun code d'autorisation reçu");
        }

        // Échanger le code contre des tokens
        String tokenUrl = "https://api.dropboxapi.com/oauth2/token";
        String params = "code=" + URLEncoder.encode(authCode[0], StandardCharsets.UTF_8) +
                "&grant_type=authorization_code" +
                "&client_id=" + URLEncoder.encode(APP_KEY, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(APP_SECRET, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(tokenUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // Parser la réponse
        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response.toString()).getAsJsonObject();

        if (json.has("refresh_token")) {
            String refreshToken = json.get("refresh_token").getAsString();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("🎉 REFRESH_TOKEN OBTENU !");
            System.out.println("=".repeat(60));
            System.out.println("\nCopie ce token et colle-le dans la variable REFRESH_TOKEN:");
            System.out.println("\n" + refreshToken);
            System.out.println("\n" + "=".repeat(60));
        } else {
            throw new IOException("Pas de refresh_token dans la réponse");
        }
    }

 */

    private static String refreshAccessToken() throws Exception {
        String tokenUrl = "https://api.dropboxapi.com/oauth2/token";

        String params = "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(REFRESH_TOKEN, "UTF-8") +
                "&client_id=" + URLEncoder.encode(APP_KEY, "UTF-8") +
                "&client_secret=" + URLEncoder.encode(APP_SECRET, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(tokenUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parser la réponse JSON
            String jsonResponse = response.toString();
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (json.has("access_token")) {
                return json.get("access_token").getAsString();
            } else {
                throw new IOException("Pas d'access_token dans la réponse");
            }
        } else {
            // Lire l'erreur
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            throw new IOException("Erreur HTTP " + responseCode + ": " + errorResponse.toString());
        }
    }


    // token ---------------------------------------------------------
    public static String getValidAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        long safetyMargin = 300000; // 5 minutes de marge

        if (cachedAccessToken == null || now > (tokenExpiryTime - safetyMargin)) {
            System.out.println("🔄 Rafraîchissement du token d'accès...");
            cachedAccessToken = refreshAccessToken();
            tokenExpiryTime = now + (14400 * 1000); // 4 heures
            System.out.println("✅ Nouveau token obtenu");
        }

        return cachedAccessToken;
    }
     //-------------------------------------------------------

}
