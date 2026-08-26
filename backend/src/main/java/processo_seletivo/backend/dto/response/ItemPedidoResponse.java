package processo_seletivo.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedidoResponse {
    private UUID idProduto;
    private String nomeProduto;
    private int qtdProduto;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;
}