package co.edu.unbosque.acciones_elbosque.notification_service.dto;

import lombok.Data;

@Data
public class UserPreferencesResponse {
    private boolean orderConfirm;
    private boolean notifEmail;
}
