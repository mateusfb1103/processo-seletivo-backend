package processo_seletivo.backend.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import processo_seletivo.backend.model.DocumentoPedido;

import java.util.UUID;

public interface BuscaPedidoRepository extends ElasticsearchRepository<DocumentoPedido, UUID> {
}