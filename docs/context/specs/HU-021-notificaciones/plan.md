# Plan — HU-21: Notificaciones de órdenes ejecutadas

## Contexto

notification-service está vacío. El flujo ya existe parcialmente en trading-service
(`NotificationClient`, `dispatchAsyncEvents`) pero tiene bugs y campos faltantes.
identity-service necesita un endpoint interno de preferencias.

## Orden de implementación

### Bloque A — identity-service (prerequisito)

**A1** `JwtUtil.java` — agregar `.claim("email", user.getEmail())` al builder del token.
**A2** `InternalUserController.java` — nuevo controller en `/api/v1/internal/users/{userId}/preferences`
  sin JWT; llama a `UserService` para obtener preferencias del usuario y retornar
  `UserPreferencesInternalResponse` (los campos del esquema del contrato).
**A3** `UserPreferencesInternalResponse.java` — DTO de respuesta para el endpoint interno.

Test: el test de integración existente `UserControllerTest` no toca este endpoint.
Se agrega `InternalUserControllerTest` con MockMvc: happy path 200, userId no existe 404.

### Bloque B — trading-service (correcciones y extensión)

**B1** `Order.java` — agregar campo `String userEmail` (Hibernate añade la columna vía `update`).
**B2** `JwtFilter.java` — extraer claim `email` del JWT, guardarlo como atributo de request.
**B3** `OrderController.java` — leer atributo `email` del HttpServletRequest, pasarlo a
  `placeBuyOrder`, `placeSellOrder` y `enqueueOrder`.
**B4** `OrderService.java` (interface) — agregar `userEmail` a firma de `placeBuyOrder`/`placeSellOrder`.
**B5** `OrderServiceImpl.java` — actualizar firmas; completar `dispatchAsyncEvents` con
  `executedPrice`, `commission`, `userEmail`; leer `order.getUserEmail()` en `executeQueued*`.
**B6** `QueuedOrderService.java` (interface) — agregar `userEmail` a firma de `enqueueOrder`.
**B7** `QueuedOrderServiceImpl.java` — guardar `userEmail` en el `Order` al encolar.
**B8** `NotificationClient.java` — corregir URL: `/api/v1/notifications` → `/api/v1/internal/notifications`.

Tests existentes: `OrderControllerTest`, `OrderControllerQueueTest`, `OrderServiceImplTest`,
`QueuedOrderServiceImplTest` — actualizar firmas en mocks. No se añaden tests nuevos para
estas correcciones (los existentes cubren el flujo).

### Bloque C — notification-service (implementación completa)

**C1** `pom.xml` — quitar `spring-boot-starter-data-jpa` y `postgresql`; agregar
  `spring-boot-starter-mail`, `spring-retry`, `spring-boot-starter-aop`.
**C2** `application.properties` — puerto 8084, mail SMTP (vars de entorno), URLs de
  identity-service y audit-service, async config.
**C3** `SecurityConfig.java` — `permitAll` para todos los endpoints (servicio interno).
**C4** `AsyncConfig.java` — `@EnableAsync`.
**C5** `dto/NotificationRequest.java` — `userId`, `eventType`, `data` (Map).
**C6** `dto/UserPreferencesResponse.java` — `orderConfirm`, `notifEmail` (solo los necesarios).
**C7** `client/IdentityClient.java` — `GET /api/v1/internal/users/{userId}/preferences`
  con timeout de 3 segundos; lanza excepción en fallo (capturada por el service).
**C8** `client/AuditClient.java` — `POST /api/v1/internal/events` con `@Async`; swallows exceptions.
**C9** `dto/AuditEventRequest.java` — `timestamp`, `service`, `eventType`, `userId`, `correlationId`, `details`.
**C10** `service/NotificationService.java` — interface con `processNotification(NotificationRequest)`.
**C11** `service/impl/NotificationServiceImpl.java` — lógica principal:
  - Obtener preferencias de identity-service; en fallo: usar defaults (`notifEmail=true`, `orderConfirm=true`)
    y registrar `NOTIFICATION_SENT_DEFAULT_PREFS`.
  - Si `!notifEmail || !orderConfirm` → no enviar.
  - Construir y enviar email con Spring Mail (`@Retryable(3, 1s)`).
  - En éxito: `NOTIFICATION_SENT` vía AuditClient `@Async`.
  - En fallo total: `NOTIFICATION_FAILED` vía AuditClient `@Async`.
**C12** `controller/NotificationController.java` — `POST /api/v1/internal/notifications`,
  status 202, sin JWT.
**C13** `NotificationServiceApplicationTests.java` — actualizar para no fallar en startup
  (sin datasource).

### Bloque D — tests notification-service

**D1** `NotificationServiceImplTest.java` — 4 tests unitarios (`@ExtendWith(MockitoExtension)`):
  - `processNotification_orderConfirmTrue_sendsEmail`
  - `processNotification_orderConfirmFalse_skipsEmail`
  - `processNotification_identityUnavailable_usesDefaultsAndSendsEmail`
  - `processNotification_mailFails_registersNotificationFailed`

**D2** `NotificationControllerTest.java` — integración con `@SpringBootTest @AutoConfigureMockMvc`:
  - `postNotification_validBody_returns202`
  - `postNotification_noAuth_returns202` (sin JWT también funciona)

## Archivos — resumen

### identity-service
**Nuevos:** `InternalUserController.java`, `UserPreferencesInternalResponse.java`,
  `InternalUserControllerTest.java`
**Modificados:** `JwtUtil.java`

### trading-service
**Modificados:** `Order.java`, `JwtFilter.java`, `OrderController.java`, `OrderService.java`,
  `OrderServiceImpl.java`, `QueuedOrderService.java`, `QueuedOrderServiceImpl.java`,
  `NotificationClient.java`, tests existentes (firmas de mocks)

### notification-service
**Nuevos (todo):** `pom.xml` (modificado), `application.properties`, `SecurityConfig.java`,
  `AsyncConfig.java`, `NotificationRequest.java`, `UserPreferencesResponse.java`,
  `AuditEventRequest.java`, `IdentityClient.java`, `AuditClient.java`,
  `NotificationService.java`, `NotificationServiceImpl.java`, `NotificationController.java`,
  `NotificationServiceImplTest.java`, `NotificationControllerTest.java`
**Modificados:** `NotificationServiceApplicationTests.java`

## Decisiones documentadas

- `userEmail` se agrega al JWT (claim `email`) para evitar un HTTP call extra en el path crítico
  de ejecución de órdenes inmediatas.
- `userEmail` se persiste en `Order.userEmail` para que las órdenes encoladas también puedan
  notificar cuando se ejecutan por el scheduler.
- `AuditClient` en notification-service usa la URL correcta `/api/v1/internal/events`
  (el AuditClient de trading-service tiene un bug con `/api/v1/audit/events` que se deja
  como deuda técnica existente — no se corrige aquí para no alterar tests del scope anterior).
- Template de email: String formateado simple (sin Thymeleaf), conforme a la nota técnica del spec.
