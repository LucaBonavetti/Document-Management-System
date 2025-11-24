package paperless.genaiworker.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import paperless.genaiworker.config.MinioConfig;
import paperless.genaiworker.gemini.GeminiClient;
import paperless.paperless.messaging.GenAiJobMessage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class GenAiWorkerService {

    private static final Logger log = LoggerFactory.getLogger(GenAiWorkerService.class);

    private final MinioClient minio;
    private final MinioConfig minioCfg;
    private final GeminiClient gemini;

    public GenAiWorkerService(MinioClient minio, MinioConfig cfg, GeminiClient gemini) {
        this.minio = minio;
        this.minioCfg = cfg;
        this.gemini = gemini;
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

            log.info("SUMMARY RESPONSE:\n{}", summary);

            // TODO: call REST server to store summary in DB

        } catch (Exception e) {
            log.error("GenAI processing failed", e);
        }
    }
}