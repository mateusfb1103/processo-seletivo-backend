package processo_seletivo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnderecoDTO {

    @NotBlank(message = "Logradouro é obrigatório")
    private String logradouro;

    private int numero;

    @NotBlank(message = "Bairro é obrigatório")
    private String bairro;

    @NotBlank(message = "CEP é obrigatório")
    private String cep;

    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;

    @NotBlank(message = "Estado é obrigatório")
    private String estado;

    private String complemento;
}