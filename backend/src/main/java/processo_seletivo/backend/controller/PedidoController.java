package processo_seletivo.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import processo_seletivo.backend.dto.request.PedidoRequest;
import processo_seletivo.backend.dto.response.PedidoResponse;
import processo_seletivo.backend.service.BuscaPedidoService;
import processo_seletivo.backend.service.PedidoService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final BuscaPedidoService buscaPedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        PedidoResponse response = pedidoService.criarPedido(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar() {
        return ResponseEntity.ok(pedidoService.findAll());
    }

    @GetMapping("/{idPedido}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable UUID idPedido) {
        return ResponseEntity.ok(pedidoService.findById(idPedido));
    }

    // consultas no elasticsearch

    @GetMapping("/busca")
    public ResponseEntity<List<PedidoResponse>> buscarTodosNoElasticsearch() {
        return ResponseEntity.ok(buscaPedidoService.buscarTodos());
    }

    @GetMapping("/busca/{idPedido}")
    public ResponseEntity<PedidoResponse> buscarPorIdNoElasticsearch(@PathVariable UUID idPedido) {
        return ResponseEntity.ok(buscaPedidoService.buscarPorId(idPedido));
    }
}
