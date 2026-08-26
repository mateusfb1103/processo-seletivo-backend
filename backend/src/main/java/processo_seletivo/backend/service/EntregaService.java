package processo_seletivo.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import processo_seletivo.backend.model.Entrega;
import processo_seletivo.backend.repository.EntregaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaService {

    private final EntregaRepository entregaRepository;

    public List<Entrega> findAll() {
        return entregaRepository.findAll();
    }
}
