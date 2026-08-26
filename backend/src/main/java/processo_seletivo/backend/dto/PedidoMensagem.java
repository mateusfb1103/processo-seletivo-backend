package processo_seletivo.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoMensagem implements Serializable {
    private UUID idPedido;
    private EnderecoDTO enderecoEntrega;
}