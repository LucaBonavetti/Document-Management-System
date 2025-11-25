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

        String prompt = """
                Summarize the following document in **clear, concise English**.

                Requirements:
                - Maximum length: **3 sentences**
                - Maximum **350 characters**
                - No bullet points, no lists, no markdown
                - No intro phrases (e.g., “Here is the summary:”)
                - Output only the summary text.

                Document:
                """ + text;

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
                        "temperature": 0.2,
                        "topK": 40,
                        "topP": 0.8,
                        "maxOutputTokens": 200
                    }
                }
                """.formatted(escape(prompt));

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

        // Parse JSON to extract only the summary text
        return extractText(response.body());
    }

    // Extracts the "text" field from Gemini's JSON response
    private String extractText(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx == -1) return json;

        int start = json.indexOf("\"", idx + 7) + 1;
        int end = json.indexOf("\"", start);

        if (start < 0 || end < 0) return json;
        return json.substring(start, end).replace("\\n", " ").trim();
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}