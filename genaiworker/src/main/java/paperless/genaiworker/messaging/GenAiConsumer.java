package paperless.genaiworker.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import paperless.genaiworker.service.GenAiWorkerService;
import paperless.paperless.messaging.GenAiJobMessage;

@Component
public class GenAiConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenAiConsumer.class);
    private final GenAiWorkerService service;

    public GenAiConsumer(GenAiWorkerService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${genai.queue.name}", containerFactory = "genAiListenerFactory")
    public void onMessage(GenAiJobMessage msg) {
        log.info("Received GenAI job for docId={} (text path = {})",
                msg.getDocumentId(), msg.getStoredTextPath());
        service.process(msg);
    }
}