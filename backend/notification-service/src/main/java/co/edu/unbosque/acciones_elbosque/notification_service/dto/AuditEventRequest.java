package co.edu.unbosque.acciones_elbosque.notification_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class AuditEventRequest {
    private Instant timestamp;
    private String service;
    private String eventType;
    private String userId;
    private String correlationId;
    private Map<String, Object> details;
}
