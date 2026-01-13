package Main.service;

import Main.model.TransactionRequest;
import Main.model.Validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TransactionService {
    private final CryptoService cryptoService;

    public TransactionService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public List<String> sendToValidators(TransactionRequest request, List<Validator> validators) {
        List<String> responses = new ArrayList<>();
        int targetCount = Math.min(3, validators.size());
        String payload = request.toJson();
        for (int i = 0; i < targetCount; i++) {
            Validator validator = validators.get(i);
            try {
                String encrypted = cryptoService.encryptForValidator(payload, validator.getPublicKey());
                String response = postTransaction(validator.getUrl(), encrypted);
                responses.add(validator.getUrl() + ": " + response);
            } catch (Exception e) {
                responses.add(validator.getUrl() + ": ERROR - " + e.getMessage());
            }
        }
        if (responses.isEmpty()) {
            responses.add("No validators available. Please refresh validator list.");
        }
        return responses;
    }

    private String postTransaction(String validatorUrl, String encryptedPayload) throws IOException {
        String endpoint = validatorUrl.endsWith("/") ? validatorUrl + "txRequest" : validatorUrl + "/txRequest";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        String body = "{\"payload\":\"" + encryptedPayload + "\"}";
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = connection.getResponseCode();
        String message = mapResponse(code);
        StringBuilder responseBody = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream()))) {
            String line;
            while (reader != null && (line = reader.readLine()) != null) {
                responseBody.append(line);
            }
        }
        if (responseBody.length() > 0) {
            return code + " " + message + " (" + responseBody + ")";
        }
        return code + " " + message;
    }

    private String mapResponse(int code) {
        return switch (code) {
            case 200 -> "Transaction Request Accepted";
            case 201 -> "Insufficient Balance";
            case 202 -> "Signature Verification Failed";
            case 404 -> "Validator not available";
            default -> "Unknown response";
        };
    }
}
