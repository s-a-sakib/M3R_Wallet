package Main.service;

import Main.model.Validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorService {
    private static final Pattern URL_PATTERN = Pattern.compile("\"validator_url\"\\s*[:=]\\s*\"([^\"]+)\"");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\"validator_address\"\\s*[:=]\\s*\"([^\"]+)\"");
    private static final Pattern PUBLIC_KEY_PATTERN = Pattern.compile("\"public_key\"\\s*[:=]\\s*\"([^\"]+)\"");
    private static final Pattern FEE_PATTERN = Pattern.compile("\"fee\"\\s*[:=]\\s*([0-9]+)");

    public List<Validator> fetchValidators(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return parseValidators(response.toString());
    }

    public List<Validator> parseValidators(String json) {
        List<Validator> validators = new ArrayList<>();
        List<String> objects = extractObjects(json);
        for (String object : objects) {
            String url = findValue(URL_PATTERN, object);
            String address = findValue(ADDRESS_PATTERN, object);
            String publicKey = findValue(PUBLIC_KEY_PATTERN, object);
            long fee = parseFee(object);
            if (url != null && address != null && publicKey != null) {
                validators.add(new Validator(url, address, publicKey, fee));
            }
        }
        validators.sort(Comparator.comparingLong(Validator::getFee));
        return validators;
    }

    private List<String> extractObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private String findValue(Pattern pattern, String object) {
        Matcher matcher = pattern.matcher(object);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private long parseFee(String object) {
        Matcher matcher = FEE_PATTERN.matcher(object);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return 0L;
    }
}
