package paperless.paperless.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrProducer {

    private static final Logger log = LoggerFactory.getLogger(OcrProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final Queue ocrQueue;

    public OcrProducer(RabbitTemplate rabbitTemplate, Queue ocrQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.ocrQueue = ocrQueue;
    }

    public void send(OcrJobMessage msg) {
        log.info("Publishing OCR job: id={} file='{}' path='{}'", msg.getDocumentId(), msg.getFilename(), msg.getStoredPath());
        rabbitTemplate.convertAndSend(ocrQueue.getName(), msg);
    }
}