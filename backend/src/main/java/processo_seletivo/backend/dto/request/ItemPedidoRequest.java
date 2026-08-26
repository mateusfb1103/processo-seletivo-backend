package processo_seletivo.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoRequest {

    @NotNull(message = "idProduto é obrigatório")
    private UUID idProduto;

    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private int qtdProduto;
}