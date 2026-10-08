package co.edu.unbosque.acciones_elbosque.audit_service.service;

import co.edu.unbosque.acciones_elbosque.audit_service.dto.request.AuditEventRequest;

public interface AuditEventService {
    void saveEvent(AuditEventRequest request);
}
