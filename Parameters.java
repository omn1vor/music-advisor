package advisor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class Parameters {
    final String redirectURI = "http://localhost:8080";
    String authEndpoint;
    String resourceEndPoint;
    int entriesPerPage;
    String CLIENT_ID;
    String CLIENT_SECRET;

    private static Parameters instance;

    private Parameters() {
        readEnv();
    }

    private void readEnv() {
        try {
            List<String> lines = Files.readAllLines(Path.of("env.txt"));
            for (String line : lines) {
                if (line.startsWith("CLIENT_ID=")) {
                    CLIENT_ID = line.replace("CLIENT_ID=", "");
                } else if (line.startsWith("CLIENT_SECRET=")) {
                    CLIENT_SECRET = line.replace("CLIENT_SECRET=", "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't read init file. Closing.");
        }
    }

    public static Parameters create(String[] args) {
        if (instance == null) {
            instance = new Parameters();
        }

        instance.authEndpoint = setParameter("access", Function.identity(),
                "https://accounts.spotify.com", args);
        instance.resourceEndPoint = setParameter("resource", Function.identity(),
                "https://api.spotify.com", args);
        instance.entriesPerPage = setParameter("page", Integer::parseInt, 5, args);

        return instance;
    }

    private static <T> T setParameter(String name, Function<String, T> mapper, T defaultValue, String[] args) {
        String value = Util.getAppParameter(name, args);
        return value.isBlank() ? defaultValue : mapper.apply(value);
    }

}
