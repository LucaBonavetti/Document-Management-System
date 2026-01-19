package paperless.ocrworker.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import paperless.ocrworker.messaging.OcrResultMessage;

@Component
public class OcrResultProducer {

    private static final Logger log = LoggerFactory.getLogger(OcrResultProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final Queue ocrResultQueue;

    public OcrResultProducer(RabbitTemplate rabbitTemplate,
                             @Qualifier("ocrResultQueue") Queue ocrResultQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.ocrResultQueue = ocrResultQueue;
    }

    public void send(OcrResultMessage msg) {
        String q = ocrResultQueue.getName();
        log.info("Publishing OCR result to queue='{}' documentId={} textKey='{}'",
                q, msg.getDocumentId(), msg.getTextKey());
        rabbitTemplate.convertAndSend("", q, msg);
    }
}
