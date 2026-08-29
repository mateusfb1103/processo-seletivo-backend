package processo_seletivo.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.response.ItemPedidoResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(indexName = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoPedido {

    @Id
    private UUID idPedido;

    private UUID idCliente;

    private List<ItemPedidoResponse> itens;

    private BigDecimal valorTotal;

    private Status status;

    private LocalDateTime dataCriacao;

    private EnderecoDTO enderecoEntrega;
}