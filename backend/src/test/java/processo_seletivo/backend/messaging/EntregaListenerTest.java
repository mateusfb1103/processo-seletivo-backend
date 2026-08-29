package processo_seletivo.backend.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.PedidoMensagem;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.model.Entrega;
import processo_seletivo.backend.repository.EntregaRepository;
import processo_seletivo.backend.repository.PedidoRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntregaListener")
class EntregaListenerTest {

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private EntregaListener entregaListener;

    @Captor
    private ArgumentCaptor<Entrega> entregaCaptor;

    @Test
    @DisplayName("deve criar a Entrega quando o pedido existir e ainda não houver entrega para ele")
    void deveCriarEntregaQuandoPedidoExisteESemEntregaAnterior() {
        UUID idPedido = UUID.randomUUID();
        EnderecoDTO enderecoDTO = EnderecoDTO.builder()
                .logradouro("Rua das Flores")
                .numero(123)
                .bairro("Centro")
                .cep("86000-000")
                .cidade("Londrina")
                .estado("PR")
                .complemento("Apto 45")
                .build();

        PedidoMensagem mensagem = PedidoMensagem.builder()
                .idPedido(idPedido)
                .enderecoEntrega(enderecoDTO)
                .build();

        when(pedidoRepository.existsById(idPedido)).thenReturn(true);
        when(entregaRepository.existsByIdPedido(idPedido)).thenReturn(false);

        entregaListener.receberPedido(mensagem);

        verify(entregaRepository).save(entregaCaptor.capture());
        Entrega entregaSalva = entregaCaptor.getValue();

        assertThat(entregaSalva.getIdEntrega()).isNotNull();
        assertThat(entregaSalva.getIdPedido()).isEqualTo(idPedido);
        assertThat(entregaSalva.getDataCriacao()).isNotNull();
        assertThat(entregaSalva.getEnderecoEntrega().getLogradouro()).isEqualTo("Rua das Flores");
        assertThat(entregaSalva.getEnderecoEntrega().getCep()).isEqualTo("86000-000");
        assertThat(entregaSalva.getEnderecoEntrega().getComplemento()).isEqualTo("Apto 45");
    }

    @Test
    @DisplayName("deve lançar PedidoNaoEncontradoException e não salvar nada quando o pedido não existir")
    void deveLancarExcecaoQuandoPedidoNaoExiste() {
        UUID idPedidoInexistente = UUID.randomUUID();
        PedidoMensagem mensagem = PedidoMensagem.builder()
                .idPedido(idPedidoInexistente)
                .enderecoEntrega(EnderecoDTO.builder().build())
                .build();

        when(pedidoRepository.existsById(idPedidoInexistente)).thenReturn(false);

        assertThatThrownBy(() -> entregaListener.receberPedido(mensagem))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining(idPedidoInexistente.toString());

        verify(entregaRepository, never()).save(any());
        verify(entregaRepository, never()).existsByIdPedido(any());
    }

    @Test
    @DisplayName("deve ignorar a mensagem (sem salvar de novo) quando já existir Entrega para o pedido")
    void deveIgnorarMensagemDuplicadaQuandoEntregaJaExiste() {
        UUID idPedido = UUID.randomUUID();
        PedidoMensagem mensagem = PedidoMensagem.builder()
                .idPedido(idPedido)
                .enderecoEntrega(EnderecoDTO.builder().build())
                .build();

        when(pedidoRepository.existsById(idPedido)).thenReturn(true);
        when(entregaRepository.existsByIdPedido(idPedido)).thenReturn(true);

        entregaListener.receberPedido(mensagem);

        verify(entregaRepository, never()).save(any());
    }
}