package processo_seletivo.backend.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Embeddable
public class Endereco {
    private String logradouro;
    private int numero;
    private String bairro;
    private String cep;
    private String cidade;
    private String estado;
    private String complemento;
}
