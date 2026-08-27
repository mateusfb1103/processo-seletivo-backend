package processo_seletivo.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "pedidos.exchange";
    public static final String QUEUE = "pedidos.criados.queue";
    public static final String ROUTING_KEY = "pedidos.criados";

    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue pedidosQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue pedidosQueue, DirectExchange pedidosExchange) {
        return BindingBuilder.bind(pedidosQueue).to(pedidosExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}