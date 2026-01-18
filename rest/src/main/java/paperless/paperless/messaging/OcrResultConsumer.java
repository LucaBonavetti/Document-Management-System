package paperless.paperless.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import paperless.paperless.bl.service.DocumentIndexingService;

@Component
public class OcrResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(OcrResultConsumer.class);

    private final DocumentIndexingService indexingService;

    public OcrResultConsumer(DocumentIndexingService indexingService,
                             @Value("${ocr.result.queue.name}") String q) {
        this.indexingService = indexingService;
        log.info("Rabbit listener will consume OCR results from queue='{}'", q);
    }

    @RabbitListener(queues = "${ocr.result.queue.name}")
    public void onMessage(OcrResultMessage msg) {
        if (msg == null) {
            log.warn("Received null OCR result message.");
            return;
        }
        log.info("Received OCR result for documentId={} textKey='{}'",
                msg.getDocumentId(), msg.getTextKey());

        indexingService.handleOcrResult(msg);
    }
}
