package processo_seletivo.backend.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import processo_seletivo.backend.config.RabbitMQConfig;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.PedidoMensagem;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.model.Endereco;
import processo_seletivo.backend.model.Entrega;
import processo_seletivo.backend.repository.EntregaRepository;
import processo_seletivo.backend.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntregaListener {

    private final EntregaRepository entregaRepository;
    private final PedidoRepository pedidoRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receberPedido(PedidoMensagem mensagem) {

        log.info(
                "Mensagem recebida para o pedido {}",
                mensagem.getIdPedido()
        );

        if (!pedidoRepository.existsById(mensagem.getIdPedido())) {
            throw new PedidoNaoEncontradoException(mensagem.getIdPedido());
        }

        if (entregaRepository.existsByIdPedido(mensagem.getIdPedido())) {
            log.info(
                    "Entrega para pedido {} ja existe. Ignorando mensagem duplicada.",
                    mensagem.getIdPedido()
            );
            return;
        }

        Entrega entrega = new Entrega();

        entrega.setIdEntrega(UUID.randomUUID());
        entrega.setIdPedido(mensagem.getIdPedido());
        entrega.setEnderecoEntrega(
                toEndereco(mensagem.getEnderecoEntrega())
        );
        entrega.setDataCriacao(LocalDateTime.now());

        entregaRepository.save(entrega);

        log.info(
                "Entrega {} criada para o pedido {}",
                entrega.getIdEntrega(),
                mensagem.getIdPedido()
        );
    }

    private Endereco toEndereco(EnderecoDTO dto) {
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