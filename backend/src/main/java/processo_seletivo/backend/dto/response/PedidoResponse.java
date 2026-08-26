package processo_seletivo.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.model.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponse {
    private UUID idPedido;
    private UUID idCliente;
    private List<ItemPedidoResponse> itens;
    private BigDecimal valorTotal;
    private Status status;
    private LocalDateTime dataCriacao;
    private EnderecoDTO enderecoEntrega;
}