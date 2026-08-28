package processo_seletivo.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import processo_seletivo.backend.dto.EnderecoDTO;
import processo_seletivo.backend.dto.PedidoMensagem;
import processo_seletivo.backend.dto.request.ItemPedidoRequest;
import processo_seletivo.backend.dto.request.PedidoRequest;
import processo_seletivo.backend.dto.response.ItemPedidoResponse;
import processo_seletivo.backend.dto.response.PedidoResponse;
import processo_seletivo.backend.exception.PedidoNaoEncontradoException;
import processo_seletivo.backend.exception.ProdutoNaoEncontradoException;
import processo_seletivo.backend.messaging.PedidoPublisher;
import processo_seletivo.backend.model.*;
import processo_seletivo.backend.repository.BuscaPedidoRepository;
import processo_seletivo.backend.repository.PedidoRepository;
import processo_seletivo.backend.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoPublisher pedidoPublisher;
    private final BuscaPedidoRepository buscaPedidoRepository;

    @Transactional
    public PedidoResponse criarPedido(PedidoRequest request) {

        Pedido pedido = new Pedido();
        pedido.setIdPedido(UUID.randomUUID());
        pedido.setIdCliente(request.getIdCliente());
        pedido.setStatus(Status.RECEBIDO);
        pedido.setDataCriacao(LocalDateTime.now());
        pedido.setEndereco(toEndereco(request.getEnderecoEntrega()));

        List<ItemPedido> itens = montarItens(
                request.getItens(),
                pedido
        );

        pedido.setItens(itens);

        BigDecimal valorTotal = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setValorTotal(valorTotal);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        indexarPedido(pedidoSalvo);

        publicarParaEntrega(pedidoSalvo, request.getEnderecoEntrega());

        return toResponse(pedidoSalvo);
    }

    public List<PedidoResponse> findAll() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PedidoResponse findById(UUID idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() ->
                        new PedidoNaoEncontradoException(idPedido)
                );
        return toResponse(pedido);
    }

    private List<ItemPedido> montarItens(
            List<ItemPedidoRequest> itensRequest,
            Pedido pedido
    ) {

        return itensRequest.stream()
                .map(itemRequest -> {

                    Produto produto = produtoRepository
                            .findById(itemRequest.getIdProduto())
                            .orElseThrow(() ->
                                    new ProdutoNaoEncontradoException(itemRequest.getIdProduto())
                            );

                    BigDecimal valorUnitario = produto.getValorProduto();

                    BigDecimal subtotal = valorUnitario.multiply(
                            BigDecimal.valueOf(itemRequest.getQtdProduto())
                    );

                    ItemPedido item = new ItemPedido();

                    item.setIdItemPedido(UUID.randomUUID());
                    item.setPedido(pedido);
                    item.setProduto(produto);
                    item.setQtdProduto(itemRequest.getQtdProduto());
                    item.setValorUnitario(valorUnitario);
                    item.setSubtotal(subtotal);

                    return item;})
                .toList();
    }

    private void indexarPedido(Pedido pedido) {

        DocumentoPedido document = DocumentoPedido.builder()
                .idPedido(pedido.getIdPedido())
                .idCliente(pedido.getIdCliente())
                .itens(toItensResponse(pedido.getItens()))
                .valorTotal(pedido.getValorTotal())
                .status(pedido.getStatus())
                .dataCriacao(pedido.getDataCriacao())
                .enderecoEntrega(toEnderecoDTO(pedido.getEndereco()))
                .build();

        buscaPedidoRepository.save(document);
    }

    private void publicarParaEntrega(Pedido pedido, EnderecoDTO enderecoEntrega) {
        PedidoMensagem mensagem = PedidoMensagem.builder()
                .idPedido(pedido.getIdPedido())
                .enderecoEntrega(enderecoEntrega)
                .build();
        pedidoPublisher.publicarPedidoCriado(mensagem);
    }

    private Endereco toEndereco(EnderecoDTO dto) {
        return new Endereco(
                dto.getLogradouro(),
                dto.getNumero(),
                dto.getBairro(),
                dto.getCep(),
                dto.getCidade(),
                dto.getEstado(),
                dto.getComplemento());
    }

    private EnderecoDTO toEnderecoDTO(Endereco endereco) {
        return EnderecoDTO.builder()
                .logradouro(endereco.getLogradouro())
                .numero(endereco.getNumero())
                .bairro(endereco.getBairro())
                .cep(endereco.getCep())
                .cidade(endereco.getCidade())
                .estado(endereco.getEstado())
                .complemento(endereco.getComplemento())
                .build();
    }

    private List<ItemPedidoResponse> toItensResponse(List<ItemPedido> itens) {
        return itens.stream()
                .map(item ->
                        ItemPedidoResponse.builder()
                                .idProduto(item.getProduto().getIdProduto())
                                .nomeProduto(item.getProduto().getNomeProduto())
                                .qtdProduto(item.getQtdProduto())
                                .valorUnitario(item.getValorUnitario())
                                .subtotal(item.getSubtotal())
                                .build())
                .toList();
    }

    private PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.builder()
                .idPedido(pedido.getIdPedido())
                .idCliente(pedido.getIdCliente())
                .itens(toItensResponse(pedido.getItens()))
                .valorTotal(pedido.getValorTotal())
                .status(pedido.getStatus())
                .dataCriacao(pedido.getDataCriacao())
                .enderecoEntrega(toEnderecoDTO(pedido.getEndereco()))
                .build();
    }
}
