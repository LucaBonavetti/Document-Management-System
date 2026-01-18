package paperless.paperless.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean
    public Queue ocrQueue(@Value("${OCR_QUEUE_NAME:ocr-jobs}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true); // makes unroutable messages visible
        template.setReturnsCallback(ret -> System.err.println(
                "UNROUTABLE: code=" + ret.getReplyCode()
                        + " text=" + ret.getReplyText()
                        + " exch='" + ret.getExchange()
                        + "' key='" + ret.getRoutingKey() + "'"
        ));
        return template;
    }
}
