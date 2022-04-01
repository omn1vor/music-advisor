package advisor;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.BiConsumer;


public class App {
    private boolean authenticated = false;
    private final Parameters params;
    private HttpServer server;
    private String ACCESS_TOKEN;
    private String authCode = "";

    App(Parameters params) {
        this.params = params;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void auth() {
        if (!authCode.isBlank()) {
            queryForToken();
            return;
        }

        startServer();
        System.out.println("use this link to request the access code:");
        System.out.printf("%s/authorize?client_id=%s&response_type=code&redirect_uri=%s%n",
                params.authEndpoint, params.CLIENT_ID, params.redirectURI);
        System.out.println("waiting for code...");

        while (authCode.isBlank()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }

        if (!authCode.isBlank()) {
            stopServer();
            queryForToken();
        }
    }

    public void startServer() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);

            server.createContext("/", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = Util.getQueryParameter("code=([\\w\\d.-]+)", query);
                String responseText;

                if (code.isEmpty()) {
                    responseText = "Authorization code not found. Try again.";
                } else {
                    authCode = code;
                    responseText = "Got the code. Return back to your program.";
                }

                exchange.sendResponseHeaders(200, responseText.length());
                exchange.getResponseBody().write(responseText.getBytes());
                exchange.getResponseBody().close();
            });
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        server.stop(1);
    }

    public List<Entry> getNewReleases() {
        String path = params.resourceEndPoint + "/v1/browse/new-releases?limit=50";
        return queryAll(path, "albums", (response, result) -> {
            for (JsonElement item : response.get("albums").getAsJsonObject().get("items").getAsJsonArray()) {
                Entry entry = new Entry();
                JsonObject album = item.getAsJsonObject();
                entry.setTitle(album.get("name").getAsString());
                List<String> artists = new ArrayList<>();
                for (JsonElement artistItem : album.get("artists").getAsJsonArray()) {
                    artists.add(artistItem.getAsJsonObject().get("name").getAsString());
                }
                entry.setArtists(artists);
                entry.setURL(getURL(album));
                result.add(entry);
            }
        });
    }

    public List<Entry> getFeaturedPlaylists() {
        String path = params.resourceEndPoint + "/v1/browse/featured-playlists?limit=50";
        return queryAll(path, "playlists", (response, result) -> {
            for (JsonElement item : response.get("playlists").getAsJsonObject().get("items").getAsJsonArray()) {
                Entry entry = new Entry();
                JsonObject album = item.getAsJsonObject();
                entry.setTitle(album.get("name").getAsString());
                entry.setURL(getURL(album));
                result.add(entry);
            }
        });
    }

    public List<Entry> getCategories() {
        String path = params.resourceEndPoint + "/v1/browse/categories?limit=50";
        return queryAll(path, "categories", (response, result) -> {
            for (JsonElement item : response.get("categories").getAsJsonObject().get("items").getAsJsonArray()) {
                Entry entry = new Entry();
                entry.setCompact(true);
                JsonObject category = item.getAsJsonObject();
                entry.setTitle(category.get("name").getAsString());
                result.add(entry);
            }
        });

    }

    public List<Entry> getPlaylist(String category) {
        Map<String, String> catalogue = getCatalogue();
        String name = category.strip().toLowerCase();
        if (!catalogue.containsKey(name)) {
            Entry entry = new Entry();
            entry.setCompact(true);
            entry.setTitle("Unknown category name.");
            return List.of(entry);
        }
        String path = String.format("%s/v1/browse/categories/%s/playlists?limit=50",
                params.resourceEndPoint, catalogue.get(name));

        return queryAll(path, "playlists", (response, result) -> {
            for (JsonElement item : response.get("playlists").getAsJsonObject().get("items").getAsJsonArray()) {
                Entry entry = new Entry();
                JsonObject playlist = item.getAsJsonObject();
                entry.setTitle(playlist.get("name").getAsString());
                entry.setURL(getURL(playlist));
                result.add(entry);
            }
        });
    }

    private Map<String, String> getCatalogue() {
        Map<String, String> catalogue = new HashMap<>();
        String path = params.resourceEndPoint + "/v1/browse/categories?limit=50";

        do {
            JsonObject response = query(path);
            if (response == null) {
                return catalogue;
            }
            for (JsonElement item : response.get("categories").getAsJsonObject().get("items").getAsJsonArray()) {
                JsonObject cat = item.getAsJsonObject();
                catalogue.put(cat.get("name").getAsString().toLowerCase(), cat.get("id").getAsString());
            }
            JsonElement next = response.get("categories").getAsJsonObject().get("next");
            path = next.isJsonNull() ? null : next.getAsString();
        } while (path != null);

        return catalogue;
    }

    private String getURL(JsonObject object) {
        return object.get("external_urls").getAsJsonObject().get("spotify").getAsString();
    }

    private void queryForToken() {
        System.out.println("making http request for access_token...");

        HttpClient client = HttpClient.newHttpClient();
        String body = String.format("grant_type=authorization_code&code=%s&redirect_uri=%s",
                authCode, params.redirectURI);
        String authString = String.format("%s:%s", params.CLIENT_ID, params.CLIENT_SECRET);
        String authHeader = Base64.getEncoder().encodeToString(authString.getBytes());
        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + authHeader)
                .uri(URI.create(String.format("%s/api/token", params.authEndpoint)))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject jsonBody = JsonParser.parseString(response.body()).getAsJsonObject();
                ACCESS_TOKEN = jsonBody.get("access_token").getAsString();
                authenticated = true;
                System.out.println("Success!");
            } else {
                System.out.println("Wrong credentials or server error.");
                System.out.println("response: " + response.body());
            }
        } catch (JsonSyntaxException e) {
            System.out.println("Could not parse response body.");
        } catch (Exception e) {
            System.out.println("We cannot access the site. Please, try later.");
        }
    }

    private JsonObject query(String path) {
        if (!isAuthenticated()) {
            System.out.println("Please, provide access for application.");
            return null;
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + ACCESS_TOKEN)
                .uri(URI.create(path))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            if (response.statusCode() == 200) {
                if (body.get("error") != null) {
                    System.out.println(body.get("error").getAsJsonObject().get("message").getAsString());
                    return null;
                }
                return body;
            } else {
                System.out.println(body.get("error").getAsJsonObject().get("message").getAsString());
                return null;
            }
        } catch (JsonSyntaxException e) {
            System.out.println("Could not parse response body.");
        } catch (Exception e) {
            System.out.println("We cannot access the site. Please, try later.");
        }
        return null;
    }

    private List<Entry> queryAll(String url, String name, BiConsumer<JsonObject, List<Entry>> consumer) {
        List<Entry> result = new ArrayList<>();
        String path = url;
        do {
            JsonObject response = query(path);
            if (response == null) {
                return result;
            }
            consumer.accept(response, result);
            JsonElement next = response.get(name).getAsJsonObject().get("next");
            path = next.isJsonNull() ? null : next.getAsString();
        } while (path != null);

        return result;
    }
}

class Entry {
    private String title;
    private List<String> artists;
    private String URL;
    private boolean compact = false;

    public Entry() {

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getArtists() {
        return artists;
    }

    public void setArtists(List<String> artists) {
        this.artists = artists;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
    }
}

