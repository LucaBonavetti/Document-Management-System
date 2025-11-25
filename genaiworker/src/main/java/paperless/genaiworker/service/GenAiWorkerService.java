package paperless.genaiworker.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import paperless.genaiworker.config.MinioConfig;
import paperless.genaiworker.gemini.GeminiClient;
import paperless.paperless.messaging.GenAiJobMessage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GenAiWorkerService {

    private static final Logger log = LoggerFactory.getLogger(GenAiWorkerService.class);

    private final MinioClient minio;
    private final MinioConfig minioCfg;
    private final GeminiClient gemini;
    private final RestTemplate restTemplate;

    @Value("${rest.base-url:http://web:80}")
    private String restBaseUrl;

    public GenAiWorkerService(MinioClient minio, MinioConfig cfg, GeminiClient gemini, RestTemplate restTemplate) {
        this.minio = minio;
        this.minioCfg = cfg;
        this.gemini = gemini;
        this.restTemplate = restTemplate;
    }

    public void process(GenAiJobMessage msg) {
        try {
            String bucket = minioCfg.getBucketName();
            String textPath = msg.getStoredTextPath();

            log.info("Fetching OCR text from MinIO: {}", textPath);

            InputStream in = minio.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(textPath).build()
            );
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            log.info("Calling Gemini for summary (text length = {})", text.length());
            String summary = gemini.summarize(text);

            if (summary != null) {
                summary = summary.replaceAll("\\s+", " ").trim();
                if (summary.length() > 350) {
                    summary = summary.substring(0, 350);
                }
            }

            log.info("SUMMARY:\n{}", summary);

            // Call REST API to store the summary
            String url = restBaseUrl + "/api/documents/" + msg.getDocumentId() + "/summary";
            log.info("Sending summary to REST service: {}", url);

            restTemplate.postForEntity(
                    url,
                    Map.of("summary", summary),
                    Void.class
            );

        } catch (Exception e) {
            log.error("GenAI processing failed for document {}", msg.getDocumentId(), e);
        }
    }
}