package co.edu.unbosque.acciones_elbosque.notification_service.service;

import co.edu.unbosque.acciones_elbosque.notification_service.dto.NotificationRequest;

public interface NotificationService {

    void processNotification(NotificationRequest request);
}
