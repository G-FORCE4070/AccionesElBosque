package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class AuditEventRequest {

    private Instant timestamp;
    private String service;
    private String eventType;
    private String userId;
    private String correlationId;
    private Map<String, Object> details;
}
