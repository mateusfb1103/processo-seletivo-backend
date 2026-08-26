package processo_seletivo.backend.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ENTREGAS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Entrega {

    @Id
    private UUID idEntrega;

    @Column(name = "id_pedido", nullable = false)
    private UUID idPedido;

    @Embedded
    private Endereco enderecoEntrega;

    private LocalDateTime dataCriacao;
}