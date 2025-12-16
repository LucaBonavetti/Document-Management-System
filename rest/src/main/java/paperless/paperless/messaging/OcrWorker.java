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

    private final IndexProducer indexProducer;

    public OcrWorker(IndexProducer indexProducer) {
        this.indexProducer = indexProducer;
    }

    @RabbitListener(queues = "${OCR_QUEUE_NAME:ocr-jobs}")
    public void handle(OcrJobMessage job) {
        try {
            if (job == null) {
                log.warn("Received null OCR job message.");
                return;
            }
            Path p = Paths.get(job.getStoredPath());
            String content = extractText(job.getContentType(), p, job.getFilename());

            IndexJobMessage idx = new IndexJobMessage();
            idx.setDocumentId(job.getDocumentId());
            idx.setFilename(job.getFilename());
            idx.setContentType(job.getContentType());
            idx.setSize(job.getSize());
            idx.setUploadedAt(job.getUploadedAt());
            idx.setStoredPath(job.getStoredPath());
            idx.setTextContent(content);

            indexProducer.send(idx);

            log.info("OCR worker processed documentId={} -> indexed message sent", job.getDocumentId());
        } catch (Exception ex) {
            log.error("Failed to process OCR job for documentId={}, error={}",
                    job != null ? job.getDocumentId() : "null", ex.toString(), ex);
        }
    }

    private String extractText(String contentType, Path path, String filename) {
        try {
            if (contentType != null && contentType.startsWith("text/")) {
                return Files.readString(path);
            }
        } catch (Exception ignored) {
        }
        // Fallback: filename tokens become searchable terms
        return filename == null ? "" : filename.replace('_', ' ').replace('-', ' ');
    }
}
