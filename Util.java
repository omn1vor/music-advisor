package advisor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private Util() {}

    public static String getQueryParameter(String regex, String query) {
        if (query == null) {
            return "";
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    public static String getAppParameter(String name, String[] args) {
        Pattern pattern = Pattern.compile(String.format("(?i)-%s (\\S+)", name));
        Matcher matcher = pattern.matcher(String.join(" ", args));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

}
