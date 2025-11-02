package paperless.paperless.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    public Queue ocrQueue(
            @org.springframework.beans.factory.annotation.Value("${ocr.queue.name:${OCR_QUEUE_NAME:OCR_QUEUE}}")
            String name) {
        return new Queue(name, true);
    }

    @Bean
    public RabbitAdmin amqpAdmin(ConnectionFactory cf) {
        RabbitAdmin admin = new RabbitAdmin(cf);
        admin.setAutoStartup(false); // prevents auto-declare of the Queue bean
        return admin;
    }

    @Bean
    public ObjectMapper rabbitObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMessageConverter(conv);
        rt.setMandatory(true);
        rt.setReturnsCallback(ret -> System.err.println(
                "UNROUTABLE: code=" + ret.getReplyCode()
                        + " text=" + ret.getReplyText()
                        + " exch='" + ret.getExchange()
                        + "' key='" + ret.getRoutingKey() + "'"));
        return rt;
    }
}
