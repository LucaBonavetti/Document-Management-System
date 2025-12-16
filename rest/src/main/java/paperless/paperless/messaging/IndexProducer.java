package paperless.paperless.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IndexProducer {

    private final RabbitTemplate template;
    private final String queue;

    public IndexProducer(RabbitTemplate template,
                         @Value("${INDEX_QUEUE_NAME:index-jobs}") String queue) {
        this.template = template;
        this.queue = queue;
    }

    public void send(IndexJobMessage msg) {
        template.convertAndSend(queue, msg);
    }
}
