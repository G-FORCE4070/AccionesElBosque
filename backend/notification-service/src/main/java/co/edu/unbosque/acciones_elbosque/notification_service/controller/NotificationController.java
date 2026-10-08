package co.edu.unbosque.acciones_elbosque.notification_service.controller;

import co.edu.unbosque.acciones_elbosque.notification_service.dto.NotificationRequest;
import co.edu.unbosque.acciones_elbosque.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> notify(@RequestBody NotificationRequest request) {
        notificationService.processNotification(request);
        return ResponseEntity.accepted().build();
    }
}
