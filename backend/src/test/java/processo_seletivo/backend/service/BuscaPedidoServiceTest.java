package processo_seletivo.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.response.PedidoResponse;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.model.DocumentoPedido;
import processo_seletivo.backend.model.Status;
import processo_seletivo.backend.repository.BuscaPedidoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuscaPedidoService")
class BuscaPedidoServiceTest {

    @Mock
    private BuscaPedidoRepository buscaPedidoRepository;

    @InjectMocks
    private BuscaPedidoService buscaPedidoService;

    private DocumentoPedido documentoExemplo(UUID idPedido) {
        return DocumentoPedido.builder()
                .idPedido(idPedido)
                .idCliente(UUID.randomUUID())
                .itens(List.of())
                .valorTotal(new BigDecimal("500.00"))
                .status(Status.RECEBIDO)
                .enderecoEntrega(EnderecoDTO.builder()
                        .logradouro("Rua Teste")
                        .numero(10)
                        .bairro("Bairro")
                        .cep("00000-000")
                        .cidade("Cidade")
                        .estado("UF")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("buscarTodos")
    class BuscarTodos {

        @Test
        @DisplayName("deve retornar lista vazia quando o índice não tiver documentos")
        void deveRetornarListaVaziaQuandoIndiceVazio() {
            when(buscaPedidoRepository.findAll()).thenReturn(List.of());

            List<PedidoResponse> resultado = buscaPedidoService.buscarTodos();

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("deve mapear todos os documentos encontrados para PedidoResponse")
        void deveMapearDocumentosParaPedidoResponse() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            when(buscaPedidoRepository.findAll())
                    .thenReturn(List.of(documentoExemplo(id1), documentoExemplo(id2)));

            List<PedidoResponse> resultado = buscaPedidoService.buscarTodos();

            assertThat(resultado).hasSize(2);
            assertThat(resultado).extracting(PedidoResponse::getIdPedido)
                    .containsExactlyInAnyOrder(id1, id2);
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar o PedidoResponse correspondente quando o documento existir")
        void deveRetornarPedidoQuandoDocumentoExiste() {
            UUID idPedido = UUID.randomUUID();
            when(buscaPedidoRepository.findById(idPedido))
                    .thenReturn(Optional.of(documentoExemplo(idPedido)));

            PedidoResponse resultado = buscaPedidoService.buscarPorId(idPedido);

            assertThat(resultado.getIdPedido()).isEqualTo(idPedido);
            assertThat(resultado.getValorTotal()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("deve lançar PedidoNaoEncontradoException quando não houver documento indexado com esse id")
        void deveLancarExcecaoQuandoDocumentoNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(buscaPedidoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> buscaPedidoService.buscarPorId(idInexistente))
                    .isInstanceOf(PedidoNaoEncontradoException.class)
                    .hasMessageContaining(idInexistente.toString());
        }
    }
}