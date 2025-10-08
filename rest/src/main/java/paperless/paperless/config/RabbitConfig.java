package paperless.paperless.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    @Bean
    public Queue ocrQueue(@Value("${ocr.queue.name}") String queueName) {
        log.info("Declaring OCR queue: {}", queueName);
        // durable = true -> survive broker restarts
        return new Queue(queueName, true);
    }
}