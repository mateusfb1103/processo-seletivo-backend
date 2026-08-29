package processo_seletivo.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import processo_seletivo.backend.dto.response.PedidoResponse;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.model.DocumentoPedido;
import processo_seletivo.backend.repository.BuscaPedidoRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class BuscaPedidoService {

    private final BuscaPedidoRepository buscaPedidoRepository;

    public List<PedidoResponse> buscarTodos() {
        return StreamSupport.stream(
                        buscaPedidoRepository.findAll().spliterator(),
                        false
                )
                .map(this::toResponse)
                .toList();
    }

    public PedidoResponse buscarPorId(UUID idPedido) {
        DocumentoPedido pedido = buscaPedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoNaoEncontradoException(idPedido));

        return toResponse(pedido);
    }

    private PedidoResponse toResponse(DocumentoPedido pedido) {
        return PedidoResponse.builder()
                .idPedido(pedido.getIdPedido())
                .idCliente(pedido.getIdCliente())
                .itens(pedido.getItens())
                .valorTotal(pedido.getValorTotal())
                .status(pedido.getStatus())
                .dataCriacao(pedido.getDataCriacao())
                .enderecoEntrega(pedido.getEnderecoEntrega())
                .build();
    }
}