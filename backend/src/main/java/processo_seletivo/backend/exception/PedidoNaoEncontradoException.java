package processo_seletivo.backend.exception;

import java.util.UUID;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(UUID idPedido) {
        super("Pedido nao encontrado: " + idPedido);
    }
}