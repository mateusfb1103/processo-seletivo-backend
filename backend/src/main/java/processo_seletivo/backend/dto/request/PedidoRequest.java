package processo_seletivo.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import processo_seletivo.backend.dto.EnderecoDTO;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequest {

    @NotNull(message = "idCliente é obrigatório")
    private UUID idCliente;

    @NotEmpty(message = "Pedido deve conter ao menos um item")
    @Valid
    private List<ItemPedidoRequest> itens;

    @NotNull(message = "Endereço de entrega é obrigatório")
    @Valid
    private EnderecoDTO enderecoEntrega;
}