package co.edu.unbosque.acciones_elbosque.notification_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationRequest {
    private String userId;
    private String eventType;
    private Map<String, Object> data;
}
