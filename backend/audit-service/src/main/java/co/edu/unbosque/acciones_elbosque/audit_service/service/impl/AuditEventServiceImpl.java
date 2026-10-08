package co.edu.unbosque.acciones_elbosque.audit_service.service.impl;

import co.edu.unbosque.acciones_elbosque.audit_service.dto.request.AuditEventRequest;
import co.edu.unbosque.acciones_elbosque.audit_service.model.AuditEvent;
import co.edu.unbosque.acciones_elbosque.audit_service.repository.AuditEventRepository;
import co.edu.unbosque.acciones_elbosque.audit_service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventServiceImpl implements AuditEventService {

    private final AuditEventRepository auditEventRepository;

    @Override
    public void saveEvent(AuditEventRequest request) {
        AuditEvent event = AuditEvent.builder()
                .timestamp(request.getTimestamp())
                .service(request.getService())
                .eventType(request.getEventType())
                .userId(request.getUserId())
                .correlationId(request.getCorrelationId())
                .details(request.getDetails())
                .build();
        auditEventRepository.save(event);
        log.info("Audit event stored: type={}, userId={}", request.getEventType(), request.getUserId());
    }
}
