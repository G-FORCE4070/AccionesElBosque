package co.edu.unbosque.acciones_elbosque.audit_service.repository;

import co.edu.unbosque.acciones_elbosque.audit_service.model.AuditEvent;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AuditEventRepository extends ElasticsearchRepository<AuditEvent, String> {
}
