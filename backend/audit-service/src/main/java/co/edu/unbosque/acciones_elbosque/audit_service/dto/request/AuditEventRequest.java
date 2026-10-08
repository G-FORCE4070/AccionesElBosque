package co.edu.unbosque.acciones_elbosque.audit_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class AuditEventRequest {

    @NotBlank
    private String timestamp;

    @NotBlank
    private String service;

    @NotBlank
    private String eventType;

    @NotBlank
    private String userId;

    private String correlationId;

    private Map<String, Object> details;
}
