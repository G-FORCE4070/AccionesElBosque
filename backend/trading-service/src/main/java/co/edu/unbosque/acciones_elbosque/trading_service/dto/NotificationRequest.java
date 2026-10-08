package co.edu.unbosque.acciones_elbosque.trading_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NotificationRequest {

    private String userId;
    private String eventType;
    private Map<String, Object> data;
}
