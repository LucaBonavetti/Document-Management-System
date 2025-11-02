package paperless.ocrworker.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import paperless.ocrworker.service.OcrWorkerService;
import paperless.paperless.messaging.OcrJobMessage;

@Component
public class OcrConsumer {
    private static final Logger log = LoggerFactory.getLogger(OcrConsumer.class);
    private final OcrWorkerService service;

    public OcrConsumer(OcrWorkerService service,
                       @org.springframework.beans.factory.annotation.Value("${ocr.queue.name}") String q) {
        this.service = service;
        log.info("Rabbit listener will consume from queue='{}'", q);
    }

    @RabbitListener(queues = "${ocr.queue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(paperless.paperless.messaging.OcrJobMessage msg) {
        log.info("Received OCR job message for file '{}'", msg.getFilename());
        service.process(msg);
    }
}