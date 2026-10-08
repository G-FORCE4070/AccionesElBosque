# HU-21 — Notificaciones de órdenes ejecutadas

## Historia de usuario

**Como** inversionista,
**quiero** recibir una notificación inmediata cada vez que una de mis órdenes de compra o venta es ejecutada,
**para** estar informado del resultado de mis operaciones.

---

## Prioridad y estimación

| Campo      | Valor                                    |
|------------|------------------------------------------|
| Prioridad  | Must                                     |
| Estimación | 5 puntos                                 |
| Épica      | Notificaciones                           |
| Feature    | Notificaciones transaccionales           |
| Servicio   | notification-service                     |

---

## Referencias

- `docs/context/architecture.md` — notification-service (puerto 8084), fallback de preferencias, ADR-05 (@Async), ADR-07 (MFA directo — las notificaciones de negocio SÍ pasan por notification-service)
- `docs/context/domain.md` — eventos: `NOTIFICATION_SENT`, `NOTIFICATION_FAILED`, `NOTIFICATION_SENT_DEFAULT_PREFS`
- `docs/context/contracts/notification-service.yaml` — `POST /api/v1/internal/notifications`
- `docs/context/contracts/identity-service.yaml` — `GET /api/v1/internal/users/{userId}/preferences`

---

## Flujo de notificación

```
trading-service (orden EXECUTED)
  → @Async POST /api/v1/internal/notifications (userId, eventType: ORDER_EXECUTED, data)
    notification-service
      → GET /api/v1/internal/users/{userId}/preferences (identity-service)
        ↓ preferencias obtenidas         ↓ fallo (5xx / timeout)
      evalúa orderConfirm              aplica defaults (email: true, orderConfirm: true)
                                       registra NOTIFICATION_SENT_DEFAULT_PREFS
      si orderConfirm: true → envía email (Spring Mail + Gmail SMTP)
      registra NOTIFICATION_SENT o NOTIFICATION_FAILED en audit-service
```

---

## Criterios de aceptación

### CA-01: Contenido del email de notificación

**Dado que** la orden fue ejecutada y el usuario tiene `orderConfirm: true`
**Cuando** notification-service procesa la notificación
**Entonces** envía un email al usuario con los siguientes datos:
- Tipo de operación: COMPRA o VENTA
- Símbolo de la acción
- Cantidad de acciones ejecutadas
- Precio de ejecución (por acción)
- Comisión cobrada
- Nuevo saldo disponible

---

### CA-02: Envío por email respetando preferencias

**Dado que** el usuario tiene `orderConfirm: true` en sus preferencias
**Cuando** notification-service recibe la llamada
**Entonces** envía el email
**Y** registra el evento `NOTIFICATION_SENT` en audit-service

**Dado que** el usuario tiene `orderConfirm: false`
**Cuando** notification-service recibe la llamada
**Entonces** no envía el email
**Y** no registra ningún evento de auditoría (la notificación fue correctamente omitida)

---

### CA-03: Inmediatez

**Dado que** la orden fue marcada como EXECUTED
**Cuando** trading-service llama asíncronamente a notification-service
**Entonces** el email es enviado en los siguientes segundos (no hay batching ni retardo programado)

---

### CA-04: Fallback cuando identity-service no está disponible

**Dado que** notification-service no puede consultar las preferencias de identity-service
(timeout, 5xx o connection refused)
**Cuando** procesa la notificación
**Entonces** aplica los valores por defecto: `notifEmail: true`, `orderConfirm: true`
**Y** envía el email al usuario
**Y** registra el evento `NOTIFICATION_SENT_DEFAULT_PREFS` en audit-service (en lugar de `NOTIFICATION_SENT`)

---

### CA-05: Fallo al enviar el email

**Dado que** Gmail SMTP no responde o rechaza el envío
**Cuando** notification-service intenta enviar el email
**Entonces** reintenta hasta 3 veces (Spring Retry, 1 segundo entre intentos)
**Y** si todos los intentos fallan, registra el evento `NOTIFICATION_FAILED` en audit-service
**Y** no lanza una excepción que afecte a trading-service (el flujo ya fue completado)

---

## Definition of Done

- [ ] Endpoint `POST /api/v1/internal/notifications` en notification-service implementado sin JWT
- [ ] `NotificationRequest` acepta: `userId`, `eventType: ORDER_EXECUTED`, `data` (mapa con detalles de la orden)
- [ ] notification-service consulta `GET /api/v1/internal/users/{userId}/preferences` en identity-service antes de enviar
- [ ] Si la consulta de preferencias falla, aplica defaults (`notifEmail: true`, `orderConfirm: true`)
- [ ] Envío de email con Spring Mail + Gmail SMTP usando template con los datos del campo `data`
- [ ] Spring Retry en el envío de email: 3 intentos, 1 segundo de espera
- [ ] Evento `NOTIFICATION_SENT` registrado vía `@Async` cuando el email es enviado correctamente
- [ ] Evento `NOTIFICATION_FAILED` registrado vía `@Async` cuando todos los reintentos fallan
- [ ] Evento `NOTIFICATION_SENT_DEFAULT_PREFS` registrado vía `@Async` cuando se usaron defaults
- [ ] trading-service llama a notification-service con `@Async` + `@Retryable` (no bloquea al usuario)
- [ ] El `data` del `NotificationRequest` incluye: `symbol`, `side`, `quantity`, `executedPrice`, `commission`, `newBalance`, `userEmail`
- [ ] notification-service obtiene el email del usuario desde `data.userEmail` (pasado por trading-service)
- [ ] Tests unitarios para `NotificationServiceImpl` cubriendo: envío exitoso, orderConfirm deshabilitado, fallo de identity-service (fallback), fallo de email
- [ ] Tests de integración con MockMvc para el endpoint interno (sin JWT)
- [ ] El código compila y pasa `mvn test` sin errores

---

## Notas técnicas

- trading-service pasa el `userEmail` en el campo `data` del `NotificationRequest`. notification-service
  no debe consultar a identity-service solo para obtener el email.
- La consulta de preferencias a identity-service es síncrona dentro de notification-service,
  pero notification-service fue llamado asíncronamente desde trading-service. Esto es correcto.
- El timeout para la consulta de preferencias debe ser bajo (ej: 3 segundos) para que el fallback
  se active rápidamente si identity-service no responde.
- notification-service usa `@EnableAsync` en su `AsyncConfig` para el envío de eventos a audit-service.
- El template del email puede ser HTML simple con los datos de la orden. No hay requerimiento de
  template engine (Thymeleaf, etc.) en el MVP — puede ser un String formateado.
- En el MVP solo se envían notificaciones por email (`notifEmail`). SMS y WhatsApp son deuda técnica.
