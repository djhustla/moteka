package main.API_Music;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class MusicBrainz {

    private static final String BASE_URL   = "https://musicbrainz.org/ws/2";
    private static final String USER_AGENT = "MotekApp/1.0 (contact@moteka.com)";
    private static final HttpClient   http   = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
/*
    public static void main(String[] args) throws Exception {
        List<String> liens = getLiensSociaux("rihanna");
        System.out.println("Instagram : " + liens.get(0));
        System.out.println("Facebook  : " + liens.get(1));
        System.out.println("TikTok    : " + liens.get(2));
        System.out.println("Snapchat  : " + liens.get(3));
        System.out.println("Spotify   : " + liens.get(4));
        System.out.println("YouTube   : " + liens.get(5));
    }

 */

    /**
     * Prend un nom d'artiste et retourne une List<String> de 6 éléments :
     * [0] Instagram
     * [1] Facebook
     * [2] TikTok
     * [3] Snapchat
     * [4] Spotify
     * [5] YouTube
     *
     * Chaque élément vaut null si le lien n'est pas trouvé.
     */
    public static List<String> getLiensSociaux(String nomArtiste) throws Exception {

        String[] result = {"", "", "", "", "", ""}; // String vide si non trouvé

        // ── ÉTAPE 1 : MBID ────────────────────────────────────────────────────
        JsonNode res1    = appel("artist/?query=" + nomArtiste.replace(" ", "+") + "&limit=1&fmt=json");
        JsonNode artists = res1.path("artists");
        if (!artists.isArray() || artists.size() == 0) return Arrays.asList(result);

        String mbid = artists.get(0).path("id").asText(null);
        if (mbid == null) return Arrays.asList(result);

        // ── ÉTAPE 2 : URL relations ───────────────────────────────────────────
        Thread.sleep(1100); // respect rate limit MusicBrainz (1 req/s)
        JsonNode res2      = appel("artist/" + mbid + "?inc=url-rels&fmt=json");
        JsonNode relations = res2.path("relations");
        if (!relations.isArray()) return Arrays.asList(result);

        for (JsonNode rel : relations) {
            String url = rel.path("url").path("resource").asText("");
            if (url.isEmpty()) continue;

            if      (url.contains("instagram.com"))    result[0] = url;
            else if (url.contains("facebook.com"))     result[1] = url;
            else if (url.contains("tiktok.com"))       result[2] = url;
            else if (url.contains("snapchat.com"))     result[3] = url;
            else if (url.contains("open.spotify.com")) result[4] = url;
            else if (url.contains("youtube.com"))      result[5] = url;
        }

        return Arrays.asList(result);
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────
    private static JsonNode appel(String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + query))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("MusicBrainz HTTP " + response.statusCode());

        return mapper.readTree(response.body());
    }
}