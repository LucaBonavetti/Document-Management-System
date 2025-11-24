package paperless.genaiworker.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class GeminiClient {

    @Value("${google.api.key}")
    private String apiKey;

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    public String summarize(String text) throws Exception {

        String jsonBody = """
                {
                    "contents": [
                        {
                            "parts": [
                                { "text": "%s" }
                            ]
                        }
                    ],
                    "generationConfig": {
                        "temperature": 0.3,
                        "topK": 40,
                        "topP": 0.8,
                        "maxOutputTokens": 300
                    }
                }
                """.formatted(escape(text));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini failure " + response.statusCode() +
                    ": " + response.body());
        }

        return response.body(); // we'll parse later if needed
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}