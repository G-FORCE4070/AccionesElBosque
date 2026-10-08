package co.edu.unbosque.acciones_elbosque.identity_service.service;

import java.util.Map;

public interface AuditService {
    void sendEvent(String eventType, String userId, Map<String, Object> details);
}
