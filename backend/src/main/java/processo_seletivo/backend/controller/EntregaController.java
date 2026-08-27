package processo_seletivo.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import processo_seletivo.backend.model.Entrega;
import processo_seletivo.backend.service.EntregaService;

import java.util.List;

@RestController
@RequestMapping("/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @GetMapping
    public ResponseEntity<List<Entrega>> listar() {
        return ResponseEntity.ok(entregaService.findAll());
    }
}