package processo_seletivo.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PRODUTOS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    @Id
    private UUID idProduto;

    @Column(nullable = false)
    private String nomeProduto;

    @Column(nullable = false)
    private BigDecimal valorProduto;
}
