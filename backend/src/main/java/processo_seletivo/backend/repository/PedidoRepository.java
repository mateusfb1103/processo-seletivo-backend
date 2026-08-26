package processo_seletivo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import processo_seletivo.backend.model.Pedido;

import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
}
