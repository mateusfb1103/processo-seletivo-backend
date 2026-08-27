package processo_seletivo.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import processo_seletivo.backend.config.RabbitMQConfig;
import processo_seletivo.backend.dto.PedidoMensagem;

@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarPedidoCriado(PedidoMensagem mensagem) {
        log.info("Publicando pedido {} na fila", mensagem.getIdPedido());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, mensagem);
    }
}