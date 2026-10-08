package co.edu.unbosque.acciones_elbosque.audit_service.controller;

import co.edu.unbosque.acciones_elbosque.audit_service.dto.request.AuditEventRequest;
import co.edu.unbosque.acciones_elbosque.audit_service.service.AuditEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventService auditEventService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveEvent(@Valid @RequestBody AuditEventRequest request) {
        auditEventService.saveEvent(request);
    }
}
