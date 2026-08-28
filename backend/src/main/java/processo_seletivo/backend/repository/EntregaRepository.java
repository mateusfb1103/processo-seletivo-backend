package processo_seletivo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import processo_seletivo.backend.model.Entrega;

import java.util.UUID;

public interface EntregaRepository extends JpaRepository<Entrega, UUID> {
    boolean existsByIdPedido(UUID Pedido);
}
