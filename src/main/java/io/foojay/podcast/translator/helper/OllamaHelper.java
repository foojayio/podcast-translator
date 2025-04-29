package io.foojay.podcast.translator.helper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OllamaHelper {

    public static boolean isOllamaRunning() {
        try {
            URL url = new URL("http://localhost:11434/api/version");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static String enhanceTranscriptionWithOllama(String rawTranscription, String model) throws IOException {
        String prompt = "You are an expert in enhancing and cleaning up transcribed text. " +
                "Fix any grammatical errors, improve punctuation, and make the text more readable while " +
                "preserving the original JSON structure. Here is the original text:\n\n" + rawTranscription;

        return callOllama(prompt, model);
    }

    public static String translateTextWithOllama(String text, String sourceLanguage, String targetLanguage, String model) throws IOException {
        String prompt = "Translate the following " + getLanguageName(sourceLanguage) + " text to " +
                getLanguageName(targetLanguage) + ". Ensure the translation is natural and preserves the original meaning.\n\n" +
                "Text to translate:\n" + text + "\n\n" +
                "Translation in " + getLanguageName(targetLanguage) + ":";

        return callOllama(prompt, model);
    }

    private static String callOllama(String prompt, String model) throws IOException {
        URL url = new URL("http://localhost:11434/api/generate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("response");
    }

    private static String getLanguageName(String languageCode) {
        // Convert ISO language code to full language name
        switch (languageCode.toLowerCase()) {
            case "en":
                return "English";
            case "fr":
                return "French";
            case "es":
                return "Spanish";
            case "de":
                return "German";
            case "it":
                return "Italian";
            case "pt":
                return "Portuguese";
            case "ru":
                return "Russian";
            case "zh":
                return "Chinese";
            case "ja":
                return "Japanese";
            default:
                return languageCode;
        }
    }
}
