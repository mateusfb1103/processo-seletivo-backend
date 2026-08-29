package processo_seletivo.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.PedidoMensagem;
import processo_seletivo.backend.dto.request.ItemPedidoRequest;
import processo_seletivo.backend.dto.request.PedidoRequest;
import processo_seletivo.backend.dto.response.PedidoResponse;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.exception.ProdutoNaoEncontradoException;
import processo_seletivo.backend.messaging.PedidoPublisher;
import processo_seletivo.backend.model.DocumentoPedido;
import processo_seletivo.backend.model.Pedido;
import processo_seletivo.backend.model.Produto;
import processo_seletivo.backend.model.Status;
import processo_seletivo.backend.repository.BuscaPedidoRepository;
import processo_seletivo.backend.repository.PedidoRepository;
import processo_seletivo.backend.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PedidoPublisher pedidoPublisher;

    @Mock
    private BuscaPedidoRepository buscaPedidoRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @Captor
    private ArgumentCaptor<Pedido> pedidoCaptor;

    @Captor
    private ArgumentCaptor<PedidoMensagem> mensagemCaptor;

    @Captor
    private ArgumentCaptor<DocumentoPedido> documentoCaptor;

    private Produto notebook;
    private Produto mouse;
    private EnderecoDTO enderecoDTO;

    @BeforeEach
    void setUp() {
        notebook = new Produto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Notebook Dell Inspiron",
                new BigDecimal("3500.00")
        );

        mouse = new Produto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Mouse Logitech MX",
                new BigDecimal("250.00")
        );

        enderecoDTO = EnderecoDTO.builder()
                .logradouro("Rua das Flores")
                .numero(123)
                .bairro("Centro")
                .cep("86000-000")
                .cidade("Londrina")
                .estado("PR")
                .complemento("Apto 45")
                .build();
    }

    @Nested
    @DisplayName("criarPedido")
    class CriarPedido {

        @Test
        @DisplayName("deve calcular subtotal e valorTotal a partir do preço do Produto, ignorando qualquer preço do request")
        void deveCalcularValoresCorretamenteComUmItem() {
            PedidoRequest request = new PedidoRequest(
                    UUID.randomUUID(),
                    List.of(new ItemPedidoRequest(notebook.getIdProduto(), 2)),
                    enderecoDTO
            );

            when(produtoRepository.findById(notebook.getIdProduto())).thenReturn(Optional.of(notebook));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PedidoResponse response = pedidoService.criarPedido(request);

            assertThat(response.getValorTotal()).isEqualByComparingTo("7000.00");
            assertThat(response.getItens()).hasSize(1);
            assertThat(response.getItens().get(0).getSubtotal()).isEqualByComparingTo("7000.00");
            assertThat(response.getItens().get(0).getValorUnitario()).isEqualByComparingTo("3500.00");
            assertThat(response.getStatus()).isEqualTo(Status.RECEBIDO);
        }

        @Test
        @DisplayName("deve somar corretamente o subtotal de múltiplos itens diferentes")
        void deveSomarSubtotalDeMultiplosItens() {
            PedidoRequest request = new PedidoRequest(
                    UUID.randomUUID(),
                    List.of(
                            new ItemPedidoRequest(notebook.getIdProduto(), 1),
                            new ItemPedidoRequest(mouse.getIdProduto(), 3)
                    ),
                    enderecoDTO
            );

            when(produtoRepository.findById(notebook.getIdProduto())).thenReturn(Optional.of(notebook));
            when(produtoRepository.findById(mouse.getIdProduto())).thenReturn(Optional.of(mouse));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PedidoResponse response = pedidoService.criarPedido(request);

            assertThat(response.getValorTotal()).isEqualByComparingTo("4250.00");
            assertThat(response.getItens()).hasSize(2);
        }

        @Test
        @DisplayName("deve lançar ProdutoNaoEncontradoException quando idProduto não existir, sem persistir nada")
        void deveLancarExcecaoQuandoProdutoNaoExiste() {
            UUID idProdutoInexistente = UUID.randomUUID();
            PedidoRequest request = new PedidoRequest(
                    UUID.randomUUID(),
                    List.of(new ItemPedidoRequest(idProdutoInexistente, 1)),
                    enderecoDTO
            );

            when(produtoRepository.findById(idProdutoInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.criarPedido(request))
                    .isInstanceOf(ProdutoNaoEncontradoException.class)
                    .hasMessageContaining(idProdutoInexistente.toString());

            verify(pedidoRepository, never()).save(any());
            verify(buscaPedidoRepository, never()).save(any());
            verify(pedidoPublisher, never()).publicarPedidoCriado(any());
        }

        @Test
        @DisplayName("deve persistir no H2, indexar no Elasticsearch e publicar no RabbitMQ, nessa ordem")
        void deveExecutarOsTresEfeitosColateraisNaOrdemCorreta() {
            PedidoRequest request = new PedidoRequest(
                    UUID.randomUUID(),
                    List.of(new ItemPedidoRequest(notebook.getIdProduto(), 1)),
                    enderecoDTO
            );

            when(produtoRepository.findById(notebook.getIdProduto())).thenReturn(Optional.of(notebook));
            when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

            pedidoService.criarPedido(request);

            InOrder ordem = inOrder(pedidoRepository, buscaPedidoRepository, pedidoPublisher);
            ordem.verify(pedidoRepository).save(pedidoCaptor.capture());
            ordem.verify(buscaPedidoRepository).save(documentoCaptor.capture());
            ordem.verify(pedidoPublisher).publicarPedidoCriado(mensagemCaptor.capture());

            UUID idGerado = pedidoCaptor.getValue().getIdPedido();
            assertThat(documentoCaptor.getValue().getIdPedido()).isEqualTo(idGerado);
            assertThat(mensagemCaptor.getValue().getIdPedido()).isEqualTo(idGerado);

            assertThat(mensagemCaptor.getValue().getEnderecoEntrega().getCep()).isEqualTo("86000-000");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar o PedidoResponse quando o pedido existir")
        void deveRetornarPedidoQuandoExistir() {
            UUID idPedido = UUID.randomUUID();
            Pedido pedido = new Pedido();
            pedido.setIdPedido(idPedido);
            pedido.setIdCliente(UUID.randomUUID());
            pedido.setValorTotal(new BigDecimal("100.00"));
            pedido.setStatus(Status.RECEBIDO);
            pedido.setEndereco(new processo_seletivo.backend.model.Endereco(
                    "Rua X", 1, "Bairro", "00000-000", "Cidade", "UF", ""
            ));

            when(pedidoRepository.findById(idPedido)).thenReturn(Optional.of(pedido));

            PedidoResponse response = pedidoService.findById(idPedido);

            assertThat(response.getIdPedido()).isEqualTo(idPedido);
        }

        @Test
        @DisplayName("deve lançar PedidoNaoEncontradoException quando o pedido não existir")
        void deveLancarExcecaoQuandoPedidoNaoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(pedidoRepository.findById(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.findById(idInexistente))
                    .isInstanceOf(PedidoNaoEncontradoException.class)
                    .hasMessageContaining(idInexistente.toString());
        }
    }

    @Test
    @DisplayName("findAll deve retornar lista vazia quando não houver pedidos, sem lançar exceção")
    void findAllDeveRetornarListaVaziaQuandoNaoHouverPedidos() {
        when(pedidoRepository.findAll()).thenReturn(List.of());

        List<PedidoResponse> resultado = pedidoService.findAll();

        assertThat(resultado).isEmpty();
    }
}