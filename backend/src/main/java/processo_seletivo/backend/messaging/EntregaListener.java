package processo_seletivo.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import processo_seletivo.backend.config.RabbitMQConfig;
import processo_seletivo.backend.dto.PedidoMensagem;
import processo_seletivo.backend.model.Endereco;
import processo_seletivo.backend.model.Entrega;
import processo_seletivo.backend.repository.EntregaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntregaListener {

    private final EntregaRepository entregaRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receberPedido(PedidoMensagem mensagem) {
        log.info("Mensagem recebida para o pedido {}", mensagem.getIdPedido());

        Entrega entrega = new Entrega();
        entrega.setIdEntrega(UUID.randomUUID());
        entrega.setIdPedido(mensagem.getIdPedido());
        entrega.setEnderecoEntrega(toEndereco(mensagem.getEnderecoEntrega()));
        entrega.setDataCriacao(LocalDateTime.now());

        entregaRepository.save(entrega);

        log.info("Entrega {} criada para o pedido {}", entrega.getIdEntrega(), mensagem.getIdPedido());
    }

    private Endereco toEndereco(processo_seletivo.backend.dto.EnderecoDTO dto) {
        return Endereco.builder()
                .logradouro(dto.getLogradouro())
                .numero(dto.getNumero())
                .bairro(dto.getBairro())
                .cep(dto.getCep())
                .cidade(dto.getCidade())
                .estado(dto.getEstado())
                .complemento(dto.getComplemento())
                .build();
    }
}