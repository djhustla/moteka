package main.API_Music;

import com.neovisionaries.i18n.CountryCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AlbumType;
import se.michaelthelin.spotify.enums.ModelObjectType;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.special.SearchResult;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyController {

    private final SpotifyApi spotifyApi;
    private String accessToken = null;
    private long tokenExpirationTime = 0;

    // Injection des clés depuis application.properties → _env
    public SpotifyController(
            @Value("${spotify.client.id}") String clientId,
            @Value("${spotify.client.secret}") String clientSecret) {

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
    }

    private synchronized void refreshAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            spotifyApi.setAccessToken(accessToken);
            return;
        }

        System.out.println("Demande d'un nouveau token Spotify...");

        ClientCredentialsRequest request = spotifyApi.clientCredentials().build();
        ClientCredentials credentials = request.execute();

        accessToken = credentials.getAccessToken();
        tokenExpirationTime = System.currentTimeMillis() + (credentials.getExpiresIn() * 1000) - 60000;

        spotifyApi.setAccessToken(accessToken);
        System.out.println("Nouveau token obtenu: " + accessToken.substring(0, 15) + "...");
    }


    // renvoi URL photo
    @GetMapping("/artiste/photo")
    public ResponseEntity<Map<String, Object>> getArtistPhoto(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);

            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("Récupération de l'artiste avec ID: " + artistId);

            Artist artist = spotifyApi.getArtist(artistId).build().execute();

            String photoUrl = null;
            if (artist.getImages() != null && artist.getImages().length > 0) {
                photoUrl = artist.getImages()[0].getUrl();
            }

            response.put("success", true);
            response.put("artistId", artistId);
            response.put("artistName", artist.getName());
            response.put("photoUrl", photoUrl);

            System.out.println("Succès: " + artist.getName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    // dernier single
    @GetMapping("/artiste/dernier-single")
    public ResponseEntity<Map<String, Object>> getDernierSingle(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<AlbumSimplified> paging = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("single")
                    .limit(50)
                    .build()
                    .execute();

            AlbumSimplified[] singles = paging.getItems();

            if (singles == null || singles.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun single trouvé");
                return ResponseEntity.ok(response);
            }

            List<AlbumSimplified> singlesTries = Arrays.stream(singles)
                    .filter(s -> s.getReleaseDate() != null)
                    .sorted((a, b) -> b.getReleaseDate().compareTo(a.getReleaseDate()))
                    .collect(java.util.stream.Collectors.toList());

            AlbumSimplified singleTrouve = null;
            TrackSimplified trackTrouvee = null;

            for (AlbumSimplified single : singlesTries) {

                Paging<TrackSimplified> tracksPaging = spotifyApi
                        .getAlbumsTracks(single.getId())
                        .limit(50)
                        .build()
                        .execute();

                TrackSimplified[] tracks = tracksPaging.getItems();

                if (tracks == null || tracks.length == 0) continue;

                if (tracks.length != 1) {
                    System.out.println("Ignoré (EP " + tracks.length + " titres) : " + single.getName());
                    continue;
                }

                TrackSimplified track = tracks[0];
                ArtistSimplified[] artistes = track.getArtists();

                if (artistes == null || artistes.length == 0) continue;

                String premierArtisteId = artistes[0].getId();

                if (!artistId.equals(premierArtisteId)) {
                    System.out.println("Ignoré (featured) : " + track.getName()
                            + " → artiste principal : " + artistes[0].getName());
                    continue;
                }

                singleTrouve = single;
                trackTrouvee = track;
                System.out.println("Vrai single trouvé : " + track.getName()
                        + " | date : " + single.getReleaseDate()
                        + " | artiste principal : " + artistes[0].getName());
                break;
            }

            if (singleTrouve == null) {
                response.put("success", false);
                response.put("error", "Aucun single trouvé où l'artiste est artiste principal");
                return ResponseEntity.ok(response);
            }

            Track trackComplet = spotifyApi.getTrack(trackTrouvee.getId()).build().execute();

            String photoUrl = "";
            if (singleTrouve.getImages() != null && singleTrouve.getImages().length > 0) {
                photoUrl = singleTrouve.getImages()[0].getUrl();
            }

            response.put("success",    true);
            response.put("trackId",    trackComplet.getId());
            response.put("titre",      trackComplet.getName());
            response.put("photoUrl",   photoUrl);
            response.put("dateSortie", singleTrouve.getReleaseDate());
            response.put("duree",      formatDuration(trackComplet.getDurationMs()));
            response.put("popularite", trackComplet.getPopularity());
            response.put("preview",    trackComplet.getPreviewUrl() != null ? trackComplet.getPreviewUrl() : "");
            response.put("urlSpotify", trackComplet.getExternalUrls().get("spotify"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Helper durée ms → "3:42" ──────────────────────────────────────────────
    private String formatDuration(int ms) {
        int total = ms / 1000;
        return String.format("%d:%02d", total / 60, total % 60);
    }

    private String extractArtistId(String spotifyUrl) {
        if (spotifyUrl == null || spotifyUrl.isEmpty()) return null;
        Pattern pattern = Pattern.compile("artist/([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(spotifyUrl);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    // dernier album
    @GetMapping("/artiste/dernier-album")
    public ResponseEntity<Map<String, Object>> getDernierAlbum(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<AlbumSimplified> pagingAlbum = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("album")
                    .market(CountryCode.FR)
                    .limit(1)
                    .offset(0)
                    .build()
                    .execute();

            Paging<AlbumSimplified> pagingSingle = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("single")
                    .market(CountryCode.FR)
                    .limit(1)
                    .offset(0)
                    .build()
                    .execute();

            AlbumSimplified dernierAlbum  = (pagingAlbum.getItems()  != null && pagingAlbum.getItems().length  > 0) ? pagingAlbum.getItems()[0]  : null;
            AlbumSimplified dernierSingle = (pagingSingle.getItems() != null && pagingSingle.getItems().length > 0) ? pagingSingle.getItems()[0] : null;

            AlbumSimplified retenu = null;

            if (dernierAlbum == null && dernierSingle == null) {
                response.put("success", false);
                response.put("error", "Aucun album ou EP trouvé pour cet artiste");
                return ResponseEntity.ok(response);
            } else if (dernierAlbum == null) {
                retenu = dernierSingle;
            } else if (dernierSingle == null) {
                retenu = dernierAlbum;
            } else {
                retenu = dernierAlbum.getReleaseDate().compareTo(dernierSingle.getReleaseDate()) >= 0
                        ? dernierAlbum : dernierSingle;
            }

            String albumPhotoUrl = "";
            if (retenu.getImages() != null && retenu.getImages().length > 0) {
                albumPhotoUrl = retenu.getImages()[0].getUrl();
            }

            String type = retenu.getAlbumType() != null
                    ? retenu.getAlbumType().toString().toLowerCase() : "album";

            Paging<TrackSimplified> tracksPaging = spotifyApi
                    .getAlbumsTracks(retenu.getId())
                    .limit(50)
                    .build()
                    .execute();

            TrackSimplified[] tracks = tracksPaging.getItems();

            if (tracks == null || tracks.length == 0) {
                response.put("success", false);
                response.put("error", "Aucune piste trouvée");
                return ResponseEntity.ok(response);
            }

            List<Map<String, String>> trackList = new ArrayList<>();
            for (TrackSimplified track : tracks) {
                Map<String, String> t = new LinkedHashMap<>();
                t.put("nom",        track.getName());
                t.put("urlSpotify", track.getExternalUrls().get("spotify"));
                trackList.add(t);
            }

            response.put("success",       true);
            response.put("albumNom",      retenu.getName());
            response.put("albumPhotoUrl", albumPhotoUrl);
            response.put("dateSortie",    retenu.getReleaseDate());
            response.put("nbTitres",      tracks.length);
            response.put("type",          type);
            response.put("tracks",        trackList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // dernier EP
    @GetMapping("/artiste/dernier-ep")
    public ResponseEntity<Map<String, Object>> getDernierEP(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<AlbumSimplified> paging = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("single")
                    .market(CountryCode.FR)
                    .limit(50)
                    .offset(0)
                    .build()
                    .execute();

            AlbumSimplified[] items = paging.getItems();

            if (items == null || items.length == 0) {
                response.put("success", false);
                response.put("error", "Aucune publication trouvée pour cet artiste");
                return ResponseEntity.ok(response);
            }

            AlbumSimplified epTrouve = null;
            TrackSimplified[] epTracks = null;

            for (AlbumSimplified item : items) {
                if (item == null || item.getReleaseDate() == null) continue;

                Paging<TrackSimplified> tracksPaging = spotifyApi
                        .getAlbumsTracks(item.getId())
                        .limit(50)
                        .build()
                        .execute();

                TrackSimplified[] tracks = tracksPaging.getItems();
                if (tracks == null) continue;

                if (tracks.length >= 4 && tracks.length <= 6) {
                    epTrouve  = item;
                    epTracks  = tracks;
                    break;
                }
            }

            if (epTrouve == null) {
                response.put("success", false);
                response.put("error", "Aucun EP trouvé pour cet artiste");
                return ResponseEntity.ok(response);
            }

            String photoUrl = "";
            if (epTrouve.getImages() != null && epTrouve.getImages().length > 0) {
                photoUrl = epTrouve.getImages()[0].getUrl();
            }

            List<Map<String, String>> trackList = new ArrayList<>();
            for (TrackSimplified track : epTracks) {
                Map<String, String> t = new LinkedHashMap<>();
                t.put("nom",        track.getName());
                t.put("urlSpotify", track.getExternalUrls().get("spotify"));
                trackList.add(t);
            }

            response.put("success",    true);
            response.put("titre",      epTrouve.getName());
            response.put("photoUrl",   photoUrl);
            response.put("dateSortie", epTrouve.getReleaseDate());
            response.put("nbTitres",   epTracks.length);
            response.put("type",       "ep");
            response.put("tracks",     trackList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // dernier projet (album OU EP, le plus récent des deux)
    @GetMapping("/artiste/dernier-projet")
    public ResponseEntity<Map<String, Object>> getDernierProjet(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<AlbumSimplified> pagingAlbum = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("album")
                    .market(CountryCode.FR)
                    .limit(1)
                    .offset(0)
                    .build()
                    .execute();

            AlbumSimplified dernierAlbum = (pagingAlbum.getItems() != null && pagingAlbum.getItems().length > 0)
                    ? pagingAlbum.getItems()[0] : null;

            Paging<AlbumSimplified> pagingSingles = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("single")
                    .market(CountryCode.FR)
                    .limit(50)
                    .offset(0)
                    .build()
                    .execute();

            AlbumSimplified dernierEp = null;
            TrackSimplified[] epTracks = null;

            if (pagingSingles.getItems() != null) {
                for (AlbumSimplified item : pagingSingles.getItems()) {
                    if (item == null || item.getReleaseDate() == null) continue;

                    Paging<TrackSimplified> tPaging = spotifyApi
                            .getAlbumsTracks(item.getId())
                            .limit(50)
                            .build()
                            .execute();

                    TrackSimplified[] tracks = tPaging.getItems();
                    if (tracks == null) continue;

                    if (tracks.length >= 4 && tracks.length <= 6) {
                        dernierEp = item;
                        epTracks  = tracks;
                        break;
                    }
                }
            }

            AlbumSimplified retenu = null;
            String typeRetenu = null;
            TrackSimplified[] retenuTracks = null;

            if (dernierAlbum == null && dernierEp == null) {
                response.put("success", false);
                response.put("error", "Aucun projet trouvé pour cet artiste");
                return ResponseEntity.ok(response);
            } else if (dernierAlbum == null) {
                retenu       = dernierEp;
                typeRetenu   = "ep";
                retenuTracks = epTracks;
            } else if (dernierEp == null) {
                retenu     = dernierAlbum;
                typeRetenu = "album";
            } else {
                if (dernierAlbum.getReleaseDate().compareTo(dernierEp.getReleaseDate()) >= 0) {
                    retenu     = dernierAlbum;
                    typeRetenu = "album";
                } else {
                    retenu       = dernierEp;
                    typeRetenu   = "ep";
                    retenuTracks = epTracks;
                }
            }

            if (retenuTracks == null) {
                Paging<TrackSimplified> tPaging = spotifyApi
                        .getAlbumsTracks(retenu.getId())
                        .limit(50)
                        .build()
                        .execute();
                retenuTracks = tPaging.getItems();
            }

            if (retenuTracks == null || retenuTracks.length == 0) {
                response.put("success", false);
                response.put("error", "Aucune piste trouvée");
                return ResponseEntity.ok(response);
            }

            String photoUrl = "";
            if (retenu.getImages() != null && retenu.getImages().length > 0) {
                photoUrl = retenu.getImages()[0].getUrl();
            }

            List<Map<String, String>> trackList = new ArrayList<>();
            for (TrackSimplified track : retenuTracks) {
                Map<String, String> t = new LinkedHashMap<>();
                t.put("nom",        track.getName());
                t.put("urlSpotify", track.getExternalUrls().get("spotify"));
                trackList.add(t);
            }

            response.put("success",    true);
            response.put("titre",      retenu.getName());
            response.put("photoUrl",   photoUrl);
            response.put("dateSortie", retenu.getReleaseDate());
            response.put("nbTitres",   retenuTracks.length);
            response.put("type",       typeRetenu);
            response.put("tracks",     trackList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Top 5
    @GetMapping("/artiste/top5")
    public ResponseEntity<Map<String, Object>> getTop5Tracks(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Track[] topTracks = spotifyApi
                    .getArtistsTopTracks(artistId, CountryCode.FR)
                    .build()
                    .execute();

            if (topTracks == null || topTracks.length == 0) {
                response.put("success", false);
                response.put("error", "Aucune track trouvée pour cet artiste");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> trackList = new ArrayList<>();
            int limit = Math.min(5, topTracks.length);

            for (int i = 0; i < limit; i++) {
                Track track = topTracks[i];

                Map<String, Object> t = new LinkedHashMap<>();
                t.put("trackId",    track.getId());
                t.put("nom",        track.getName());
                t.put("popularite", track.getPopularity());
                t.put("duree",      formatDuration(track.getDurationMs()));
                t.put("urlSpotify", track.getExternalUrls().get("spotify"));
                t.put("preview",    track.getPreviewUrl() != null ? track.getPreviewUrl() : "");

                String photoUrl = "";
                if (track.getAlbum() != null && track.getAlbum().getImages().length > 0) {
                    photoUrl = track.getAlbum().getImages()[0].getUrl();
                }
                t.put("photoUrl", photoUrl);
                t.put("albumNom", track.getAlbum() != null ? track.getAlbum().getName() : "");

                trackList.add(t);
            }

            response.put("success", true);
            response.put("tracks",  trackList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // renvoi l url d un artiste
    @GetMapping("/artiste/search")
    public ResponseEntity<Map<String, Object>> searchArtiste(@RequestParam String nom) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            if (nom == null || nom.isBlank()) {
                response.put("success", false);
                response.put("error", "Paramètre 'nom' requis");
                return ResponseEntity.badRequest().body(response);
            }

            Artist[] artists = spotifyApi.searchItem(nom, ModelObjectType.ARTIST.getType())
                    .limit(1)
                    .build()
                    .execute()
                    .getArtists()
                    .getItems();

            if (artists == null || artists.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun artiste trouvé pour : " + nom);
                return ResponseEntity.ok(response);
            }

            Artist artist = artists[0];
            String spotifyUrl = artist.getExternalUrls().get("spotify");

            String photoUrl = "";
            if (artist.getImages() != null && artist.getImages().length > 0) {
                photoUrl = artist.getImages()[0].getUrl();
            }

            response.put("success",    true);
            response.put("nom",        artist.getName());
            response.put("artistId",   artist.getId());
            response.put("spotifyUrl", spotifyUrl);
            response.put("photoUrl",   photoUrl);
            response.put("popularite", artist.getPopularity());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/artiste/searchChoix")
    public ResponseEntity<Map<String, Object>> searchArtisteChoix(@RequestParam String nom) {
        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            if (nom == null || nom.isBlank()) {
                response.put("success", false);
                response.put("error", "Paramètre 'nom' requis");
                return ResponseEntity.badRequest().body(response);
            }

            Artist[] artists = spotifyApi.searchItem(nom, ModelObjectType.ARTIST.getType())
                    .limit(10)
                    .build()
                    .execute()
                    .getArtists()
                    .getItems();

            if (artists == null || artists.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun artiste trouvé pour : " + nom);
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> artistesList = new ArrayList<>();

            for (Artist artist : artists) {
                Map<String, Object> artistInfo = new HashMap<>();
                artistInfo.put("nom",        artist.getName());
                artistInfo.put("artistId",   artist.getId());
                artistInfo.put("spotifyUrl", artist.getExternalUrls().get("spotify"));
                artistInfo.put("photoUrl",   artist.getImages().length > 0 ? artist.getImages()[0].getUrl() : "");
                artistInfo.put("popularite", artist.getPopularity());
                artistInfo.put("followers",  artist.getFollowers().getTotal());
                artistesList.add(artistInfo);
            }

            response.put("success",   true);
            response.put("resultats", artistesList);
            response.put("total",     artistesList.size());
            response.put("message",   artistesList.size() + " artiste(s) trouvé(s)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/artiste/searchTrack")
    public ResponseEntity<Map<String, Object>> searchTrack(@RequestParam String titre) {
        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            if (titre == null || titre.isBlank()) {
                response.put("success", false);
                response.put("error", "Le nom du titre est requis");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<Track> trackPaging = spotifyApi.searchItem(titre, ModelObjectType.TRACK.getType())
                    .limit(10)
                    .build()
                    .execute()
                    .getTracks();

            Track[] tracks = trackPaging.getItems();

            if (tracks == null || tracks.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun titre trouvé");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> trackList = new ArrayList<>();

            for (Track track : tracks) {
                Map<String, Object> trackInfo = new HashMap<>();
                trackInfo.put("titre",      track.getName());
                trackInfo.put("trackId",    track.getId());
                trackInfo.put("spotifyUrl", track.getExternalUrls().get("spotify"));
                trackInfo.put("artistUrl",  track.getArtists()[0].getExternalUrls().get("spotify"));
                trackInfo.put("artiste",    track.getArtists()[0].getName());
                trackInfo.put("photoUrl",   track.getAlbum().getImages().length > 0 ? track.getAlbum().getImages()[0].getUrl() : "");
                trackInfo.put("albumNom",   track.getAlbum().getName());
                trackList.add(trackInfo);
            }

            response.put("success",   true);
            response.put("resultats", trackList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // afficher tout
    @GetMapping("/artiste/toutes-publications")
    public ResponseEntity<Map<String, Object>> getToutesPublications(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            List<Publication> toutesPublications = new ArrayList<>();
            int offset = 0;
            int limit = 50;
            boolean hasMore = true;

            while (hasMore) {
                Paging<AlbumSimplified> paging = spotifyApi.getArtistsAlbums(artistId)
                        .album_type("album,single,compilation")
                        .limit(limit)
                        .offset(offset)
                        .build()
                        .execute();

                AlbumSimplified[] albums = paging.getItems();

                if (albums == null || albums.length == 0) {
                    hasMore = false;
                    break;
                }

                for (AlbumSimplified album : albums) {
                    try {
                        Album albumComplet = spotifyApi.getAlbum(album.getId()).build().execute();

                        Paging<TrackSimplified> tracksPaging = spotifyApi
                                .getAlbumsTracks(album.getId())
                                .limit(50)
                                .build()
                                .execute();

                        TrackSimplified[] tracks = tracksPaging.getItems();
                        String typePublication = determinerTypePublication(albumComplet);

                        if (AlbumType.SINGLE.equals(albumComplet.getAlbumType()) && tracks.length == 1) {
                            Track trackComplet = spotifyApi.getTrack(tracks[0].getId()).build().execute();

                            Publication pub = new Publication();
                            pub.setId(albumComplet.getId());
                            pub.setTitre(trackComplet.getName());
                            pub.setType("single");
                            pub.setDateSortie(albumComplet.getReleaseDate());
                            pub.setPhotoUrl(albumComplet.getImages().length > 0 ? albumComplet.getImages()[0].getUrl() : "");
                            pub.setNbPistes(1);
                            pub.setTrackIds(List.of(trackComplet.getId()));
                            pub.setTrackNames(List.of(trackComplet.getName()));
                            pub.setAlbumName(albumComplet.getName());
                            pub.setUrlSpotify(trackComplet.getExternalUrls().get("spotify"));
                            toutesPublications.add(pub);
                        } else {
                            Publication pub = new Publication();
                            pub.setId(albumComplet.getId());
                            pub.setTitre(albumComplet.getName());
                            pub.setType(typePublication);
                            pub.setDateSortie(albumComplet.getReleaseDate());
                            pub.setPhotoUrl(albumComplet.getImages().length > 0 ? albumComplet.getImages()[0].getUrl() : "");
                            pub.setNbPistes(tracks.length);

                            List<String> trackIds   = new ArrayList<>();
                            List<String> trackNames = new ArrayList<>();
                            for (TrackSimplified t : tracks) {
                                trackIds.add(t.getId());
                                trackNames.add(t.getName());
                            }

                            pub.setTrackIds(trackIds);
                            pub.setTrackNames(trackNames);
                            pub.setAlbumName(albumComplet.getName());
                            pub.setUrlSpotify(albumComplet.getExternalUrls().get("spotify"));
                            toutesPublications.add(pub);
                        }

                    } catch (Exception e) {
                        System.err.println("Erreur sur l'album " + album.getId() + ": " + e.getMessage());
                    }
                }

                offset += limit;
                hasMore = paging.getNext() != null;
            }

            toutesPublications.sort((a, b) -> {
                if (a.getDateSortie() == null) return 1;
                if (b.getDateSortie() == null) return -1;
                return b.getDateSortie().compareTo(a.getDateSortie());
            });

            if (toutesPublications.isEmpty()) {
                response.put("success", false);
                response.put("error", "Aucune publication trouvée pour cet artiste");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> publicationsList = new ArrayList<>();
            for (Publication pub : toutesPublications) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id",         pub.getId());
                p.put("titre",      pub.getTitre());
                p.put("type",       pub.getType());
                p.put("dateSortie", pub.getDateSortie());
                p.put("photoUrl",   pub.getPhotoUrl());
                p.put("nbPistes",   pub.getNbPistes());
                p.put("albumName",  pub.getAlbumName());
                p.put("urlSpotify", pub.getUrlSpotify());

                List<Map<String, String>> pistes = new ArrayList<>();
                for (int i = 0; i < pub.getTrackIds().size(); i++) {
                    Map<String, String> piste = new HashMap<>();
                    piste.put("trackId", pub.getTrackIds().get(i));
                    piste.put("nom",     pub.getTrackNames().get(i));
                    pistes.add(piste);
                }
                p.put("pistes", pistes);
                publicationsList.add(p);
            }

            response.put("success",      true);
            response.put("publications", publicationsList);
            response.put("total",        publicationsList.size());
            response.put("artisteId",    artistId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Classe interne pour structurer les publications
    private static class Publication {
        private String id, titre, type, dateSortie, photoUrl, albumName, urlSpotify;
        private int nbPistes;
        private List<String> trackIds, trackNames;

        public String getId()                        { return id; }
        public void   setId(String id)               { this.id = id; }
        public String getTitre()                     { return titre; }
        public void   setTitre(String titre)         { this.titre = titre; }
        public String getType()                      { return type; }
        public void   setType(String type)           { this.type = type; }
        public String getDateSortie()                { return dateSortie; }
        public void   setDateSortie(String d)        { this.dateSortie = d; }
        public String getPhotoUrl()                  { return photoUrl; }
        public void   setPhotoUrl(String p)          { this.photoUrl = p; }
        public int    getNbPistes()                  { return nbPistes; }
        public void   setNbPistes(int n)             { this.nbPistes = n; }
        public List<String> getTrackIds()            { return trackIds; }
        public void   setTrackIds(List<String> l)    { this.trackIds = l; }
        public List<String> getTrackNames()          { return trackNames; }
        public void   setTrackNames(List<String> l)  { this.trackNames = l; }
        public String getAlbumName()                 { return albumName; }
        public void   setAlbumName(String a)         { this.albumName = a; }
        public String getUrlSpotify()                { return urlSpotify; }
        public void   setUrlSpotify(String u)        { this.urlSpotify = u; }
    }

    private String determinerTypePublication(Album album) {
        String albumType = album.getAlbumType().name();
        if ("single".equalsIgnoreCase(albumType))      return "single";
        if ("compilation".equalsIgnoreCase(albumType)) return "compilation";
        if (album.getTracks() != null && album.getTracks().getTotal() <= 6) return "ep";
        return "album";
    }

    @GetMapping("/searchAll")
    public ResponseEntity<Map<String, Object>> searchAll(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            refreshAccessToken();

            SearchResult searchResult = spotifyApi.searchItem(query, "track,artist,album,playlist")
                    .limit(5)
                    .build()
                    .execute();

            List<Map<String, Object>> globalList = new ArrayList<>();

            for (Track t : searchResult.getTracks().getItems()) {
                if (t == null) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("type",  "track");
                map.put("title", t.getName());
                Image[] imgs = t.getAlbum().getImages();
                map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                map.put("url",   t.getExternalUrls().get("spotify"));
                globalList.add(map);
            }

            for (Artist a : searchResult.getArtists().getItems()) {
                if (a == null) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("type",  "artist");
                map.put("title", a.getName());
                Image[] imgs = a.getImages();
                map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                map.put("url",   a.getExternalUrls().get("spotify"));
                globalList.add(map);
            }

            for (AlbumSimplified alb : searchResult.getAlbums().getItems()) {
                if (alb == null) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("type",  "album");
                map.put("title", alb.getName());
                Image[] imgs = alb.getImages();
                map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                map.put("url",   alb.getExternalUrls().get("spotify"));
                globalList.add(map);
            }

            for (PlaylistSimplified pl : searchResult.getPlaylists().getItems()) {
                if (pl == null) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("type",  "playlist");
                map.put("title", pl.getName());
                Image[] imgs = pl.getImages();
                map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                map.put("url",   pl.getExternalUrls().get("spotify"));
                globalList.add(map);
            }

            response.put("success", true);
            response.put("data",    globalList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error",   e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/recherche-par-type")
    public ResponseEntity<Map<String, Object>> rechercheParType(
            @RequestParam String query,
            @RequestParam String type) {

        Map<String, Object> response = new HashMap<>();
        try {
            refreshAccessToken();

            // "tout" → on cherche sur tous les types
            String spotifyType = type.equals("tout") ? "track,artist,album,playlist" : type;

            SearchResult searchResult = spotifyApi.searchItem(query, spotifyType)
                    .limit(20)
                    .build()
                    .execute();

            List<Map<String, Object>> globalList = new ArrayList<>();

            // 1. TITRES
            if (searchResult.getTracks() != null && searchResult.getTracks().getItems() != null) {
                for (Track t : searchResult.getTracks().getItems()) {
                    if (t == null) continue;
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "track");
                    map.put("title", t.getName());
                    Image[] imgs = t.getAlbum().getImages();
                    map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                    map.put("url", t.getExternalUrls().get("spotify"));
                    globalList.add(map);
                }
            }

            // 2. ARTISTES
            if (searchResult.getArtists() != null && searchResult.getArtists().getItems() != null) {
                for (Artist a : searchResult.getArtists().getItems()) {
                    if (a == null) continue;
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "artist");
                    map.put("title", a.getName());
                    Image[] imgs = a.getImages();
                    map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                    map.put("url", a.getExternalUrls().get("spotify"));
                    globalList.add(map);
                }
            }

            // 3. ALBUMS (inclut les EP côté Spotify)
            if (searchResult.getAlbums() != null && searchResult.getAlbums().getItems() != null) {
                for (AlbumSimplified alb : searchResult.getAlbums().getItems()) {
                    if (alb == null) continue;
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "album");
                    map.put("title", alb.getName());
                    Image[] imgs = alb.getImages();
                    map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                    map.put("url", alb.getExternalUrls().get("spotify"));
                    globalList.add(map);
                }
            }

            // 4. PLAYLISTS
            if (searchResult.getPlaylists() != null && searchResult.getPlaylists().getItems() != null) {
                for (PlaylistSimplified pl : searchResult.getPlaylists().getItems()) {
                    if (pl == null) continue;
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "playlist");
                    map.put("title", pl.getName());
                    Image[] imgs = pl.getImages();
                    map.put("image", imgs != null && imgs.length > 0 ? imgs[0].getUrl() : "");
                    map.put("url", pl.getExternalUrls().get("spotify"));
                    globalList.add(map);
                }
            }

            response.put("success", true);
            response.put("data", globalList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 3 derniers singles
    @GetMapping("/artiste/les-3-derniers-singles")
    public ResponseEntity<Map<String, Object>> getLes3DerniersSingles(@RequestParam String url) {

        Map<String, Object> response = new HashMap<>();

        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Paging<AlbumSimplified> paging = spotifyApi.getArtistsAlbums(artistId)
                    .album_type("single")
                    .limit(50)
                    .build()
                    .execute();

            AlbumSimplified[] singles = paging.getItems();

            if (singles == null || singles.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun single trouvé");
                return ResponseEntity.ok(response);
            }

            List<AlbumSimplified> singlesTries = Arrays.stream(singles)
                    .filter(s -> s.getReleaseDate() != null)
                    .sorted((a, b) -> b.getReleaseDate().compareTo(a.getReleaseDate()))
                    .collect(java.util.stream.Collectors.toList());

            List<Map<String, Object>> resultList = new ArrayList<>();

            for (AlbumSimplified single : singlesTries) {

                if (resultList.size() >= 3) break;

                Paging<TrackSimplified> tracksPaging = spotifyApi
                        .getAlbumsTracks(single.getId())
                        .limit(50)
                        .build()
                        .execute();

                TrackSimplified[] tracks = tracksPaging.getItems();

                if (tracks == null || tracks.length == 0) continue;
                if (tracks.length != 1) continue;

                TrackSimplified track = tracks[0];
                Track trackComplet = spotifyApi.getTrack(track.getId()).build().execute();

                String photoUrl = "";
                if (single.getImages() != null && single.getImages().length > 0) {
                    photoUrl = single.getImages()[0].getUrl();
                }

                Map<String, Object> t = new LinkedHashMap<>();
                t.put("trackId",    trackComplet.getId());
                t.put("titre",      trackComplet.getName());
                t.put("photoUrl",   photoUrl);
                t.put("dateSortie", single.getReleaseDate());
                t.put("duree",      formatDuration(trackComplet.getDurationMs()));
                t.put("popularite", trackComplet.getPopularity());
                t.put("preview",    trackComplet.getPreviewUrl() != null ? trackComplet.getPreviewUrl() : "");
                t.put("urlSpotify", trackComplet.getExternalUrls().get("spotify"));

                resultList.add(t);
            }

            if (resultList.isEmpty()) {
                response.put("success", false);
                response.put("error", "Aucun single trouvé");
                return ResponseEntity.ok(response);
            }

            response.put("success", true);
            response.put("total",   resultList.size());
            response.put("singles", resultList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Pays de l'artiste ─────────────────────────────────────────────────────
    /**
     * GET /api/spotify/artiste/pays?url=https://open.spotify.com/artist/...
     * Retourne le pays de l'artiste (champ "country" non dispo via API standard,
     * donc on retourne les marchés disponibles → on en déduit le pays principal)
     * Spotify ne fournit pas de champ "pays" direct → on retourne les genres + markets
     * En pratique : on récupère l'artiste complet et on retourne les genres Spotify
     * qui permettent souvent de déduire l'origine (ex: "afrobeats", "french hip hop")
     */
    @GetMapping("/artiste/pays")
    public ResponseEntity<Map<String, Object>> getArtistePays(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Artist artist = spotifyApi.getArtist(artistId).build().execute();

            // Spotify ne fournit pas de champ pays direct sur l'objet Artist standard.
            // On retourne le nom + genres comme proxy (la vraie origine est dans MusicBrainz).
            response.put("success",    true);
            response.put("artistId",   artistId);
            response.put("artistName", artist.getName());
            // Note : l'API Spotify Client Credentials ne retourne pas de champ "country"
            // sur l'artiste. Ce champ est disponible uniquement via l'API Web avec OAuth user.
            response.put("pays",       null);
            response.put("note",       "L'API Spotify (Client Credentials) ne fournit pas le pays de l'artiste. Utiliser MusicBrainz pour ce champ.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ── Genres de l'artiste ───────────────────────────────────────────────────
    /**
     * GET /api/spotify/artiste/genres?url=https://open.spotify.com/artist/...
     * Retourne la liste des genres Spotify associés à l'artiste
     */
    @GetMapping("/artiste/genres")
    public ResponseEntity<Map<String, Object>> getArtisteGenres(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            refreshAccessToken();

            String artistId = extractArtistId(url);
            if (artistId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide");
                return ResponseEntity.badRequest().body(response);
            }

            Artist artist = spotifyApi.getArtist(artistId).build().execute();

            String[] genres = artist.getGenres();

            response.put("success",    true);
            response.put("artistId",   artistId);
            response.put("artistName", artist.getName());
            response.put("genres",     genres != null ? Arrays.asList(genres) : new ArrayList<>());
            response.put("total",      genres != null ? genres.length : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


    /**
     * GET /api/spotify/track/artists?url=https://open.spotify.com/track/...
     *
     * À PARTIR DE L'URL D'UN MORCEAU SPOTIFY,
     * RETOURNE LA LISTE COMPLÈTE DES ARTISTES (NOM + LIEN COMPLET + PHOTO)
     */
    @GetMapping("/track/artists")
    public ResponseEntity<Map<String, Object>> getTrackArtists(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Rafraîchir le token d'accès Spotify
            refreshAccessToken();

            // 2. Extraire l'ID du morceau depuis l'URL
            String trackId = extractTrackId(url);
            if (trackId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide. Format attendu : https://open.spotify.com/track/...");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Récupérer les informations du morceau via l'API Spotify
            Track track = spotifyApi.getTrack(trackId).build().execute();

            if (track == null) {
                response.put("success", false);
                response.put("error", "Morceau non trouvé avec l'ID : " + trackId);
                return ResponseEntity.status(404).body(response);
            }

            // 4. Récupérer la liste des artistes associés au morceau
            ArtistSimplified[] artistesSimplifies = track.getArtists();

            if (artistesSimplifies == null || artistesSimplifies.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun artiste trouvé pour ce morceau");
                return ResponseEntity.ok(response);
            }

            // 5. Construire la liste détaillée des artistes (avec URL complète + photo)
            List<Map<String, String>> artistesList = new ArrayList<>();

            for (ArtistSimplified artisteSimplifie : artistesSimplifies) {
                Map<String, String> artisteInfo = new LinkedHashMap<>();

                // Récupérer l'ID et construire l'URL Spotify complète
                String artisteId = artisteSimplifie.getId();
                String artisteUrl = "https://open.spotify.com/artist/" + artisteId;

                // Récupérer les informations complètes de l'artiste pour obtenir la photo
                String photoUrl = "";
                try {
                    Artist artisteComplet = spotifyApi.getArtist(artisteId).build().execute();
                    if (artisteComplet != null && artisteComplet.getImages() != null && artisteComplet.getImages().length > 0) {
                        photoUrl = artisteComplet.getImages()[0].getUrl();
                    }
                } catch (Exception e) {
                    // En cas d'erreur, on laisse photoUrl vide (non bloquant)
                    System.out.println("Impossible de récupérer la photo pour l'artiste: " + artisteSimplifie.getName());
                }

                artisteInfo.put("nom", artisteSimplifie.getName());
                artisteInfo.put("url", artisteUrl);
                artisteInfo.put("id", artisteId);
                artisteInfo.put("photoUrl", photoUrl); // ✅ AJOUT DE LA PHOTO

                artistesList.add(artisteInfo);
            }

            // 6. Ajouter les informations du morceau lui-même (optionnel mais utile)
            Map<String, Object> trackInfo = new LinkedHashMap<>();
            trackInfo.put("titre", track.getName());
            trackInfo.put("id", track.getId());
            trackInfo.put("url", track.getExternalUrls().get("spotify"));

            // 7. Construire la réponse finale
            response.put("success", true);
            response.put("track", trackInfo);
            response.put("artists", artistesList);
            response.put("totalArtists", artistesList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur lors de la récupération des artistes : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Méthode utilitaire pour extraire l'ID d'un morceau depuis une URL Spotify
     * @param spotifyUrl URL du morceau (ex: https://open.spotify.com/track/2x8YbqyVGjW9LlwlfUG7WI)
     * @return L'ID du morceau ou null si non trouvé
     */
    private String extractTrackId(String spotifyUrl) {
        if (spotifyUrl == null || spotifyUrl.isEmpty()) return null;

        // Pattern pour capturer l'ID après "track/"
        Pattern pattern = Pattern.compile("track/([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(spotifyUrl);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * GET /api/spotify/track/summary?url=https://open.spotify.com/track/...
     *
     * Retourne un résumé formaté d'un morceau Spotify :
     * 1. "artistePrincipal AutreArtiste(s) - Titre"
     * 2. L'URL de la photo de l'album
     */
    @GetMapping("/track/summary")
    public ResponseEntity<Map<String, Object>> getTrackSummary(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. Rafraîchir le token d'accès Spotify
            refreshAccessToken();

            // 2. Extraire l'ID du morceau depuis l'URL
            String trackId = extractTrackId(url);
            if (trackId == null) {
                response.put("success", false);
                response.put("error", "URL Spotify invalide. Format attendu : https://open.spotify.com/track/...");
                return ResponseEntity.badRequest().body(response);
            }

            // 3. Récupérer les informations du morceau via l'API Spotify
            Track track = spotifyApi.getTrack(trackId).build().execute();

            if (track == null) {
                response.put("success", false);
                response.put("error", "Morceau non trouvé avec l'ID : " + trackId);
                return ResponseEntity.status(404).body(response);
            }

            // 4. Récupérer les artistes
            ArtistSimplified[] artistesSimplifies = track.getArtists();
            if (artistesSimplifies == null || artistesSimplifies.length == 0) {
                response.put("success", false);
                response.put("error", "Aucun artiste trouvé pour ce morceau");
                return ResponseEntity.ok(response);
            }

            // 5. Construire la chaîne des artistes
            StringBuilder artistesStr = new StringBuilder();
            for (int i = 0; i < artistesSimplifies.length; i++) {
                if (i > 0) {
                    // Si c'est le dernier, on met " & " sinon " "
                    if (i == artistesSimplifies.length - 1) {
                        artistesStr.append(" & ");
                    } else {
                        artistesStr.append(" ");
                    }
                }
                artistesStr.append(artistesSimplifies[i].getName());
            }

            // 6. Récupérer la photo du morceau (via l'album)
            String photoUrl = "";
            AlbumSimplified album = track.getAlbum();
            if (album != null && album.getImages() != null && album.getImages().length > 0) {
                photoUrl = album.getImages()[0].getUrl();
            }

            // 7. Construire le résumé final
            String summary = artistesStr.toString() + " - " + track.getName();

            // 8. Préparer la réponse
            response.put("success", true);
            response.put("trackId", track.getId());
            response.put("summary", summary);
            response.put("photoUrl", photoUrl);
            response.put("artistes", artistesStr.toString());
            response.put("titre", track.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Erreur lors de la récupération : " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

