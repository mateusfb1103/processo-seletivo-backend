package processo_seletivo.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import processo_seletivo.backend.model.Produto;

import java.util.UUID;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
}
