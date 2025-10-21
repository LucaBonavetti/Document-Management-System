package paperless.paperless.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class OcrWorker {

    @RabbitListener(queues = "${OCR_QUEUE_NAME:ocr-jobs}")
    public void handle(OcrJobMessage job) {
        try {
            if (job == null) {
                log.warn("Received null OCR job message.");
                return;
            }
            Path p = Paths.get(job.getStoredPath());
            long size = Files.exists(p) ? Files.size(p) : -1L;

            // Stub "OCR": we just log the file size to demonstrate worker consumption.
            log.info("OCR worker processed documentId={} filename='{}' contentType={} storedPath='{}' sizeBytes={}",
                    job.getDocumentId(), job.getFilename(), job.getContentType(), job.getStoredPath(), size);

            // TODO: place OCR result handling here (e.g., publish result message)
        } catch (Exception ex) {
            // Never let exceptions bubble out of the listener; log and continue.
            log.error("Failed to process OCR job for documentId={}, error={}",
                    job != null ? job.getDocumentId() : "null", ex.toString(), ex);
        }
    }
}
