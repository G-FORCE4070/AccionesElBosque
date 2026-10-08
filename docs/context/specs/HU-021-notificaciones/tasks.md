# Tasks — HU-21: Notificaciones de órdenes ejecutadas

## Bloque A — identity-service (prerequisito)

- [x] **T-A-1** `JwtUtil.java` — agregar `.claim("email", user.getEmail())` al builder del token.
- [x] **T-A-2** `InternalUserController.java` — nuevo controller en `/api/v1/internal/users/{userId}/preferences` sin JWT; llama a `UserService` para obtener preferencias del usuario y retornar `UserPreferencesInternalResponse`.
- [x] **T-A-3** `UserPreferencesInternalResponse.java` — DTO de respuesta para el endpoint interno (`orderConfirm`, `notifEmail`, y demás campos del contrato).
- [x] **T-A-4** `InternalUserControllerTest.java` — integración con MockMvc: happy path 200, userId no existe 404. ✅ 58 tests, 0 fallos.

## Bloque B — trading-service (correcciones y extensión)

- [x] **T-B-1** `Order.java` — agregar campo `String userEmail` (Hibernate añade la columna vía `update`).
- [x] **T-B-2** `JwtFilter.java` — extraer claim `email` del JWT y guardarlo como atributo de request (`"email"`).
- [x] **T-B-3** `OrderController.java` — leer atributo `email` del `HttpServletRequest`, pasarlo a `placeBuyOrder`, `placeSellOrder` y `enqueueOrder`.
- [x] **T-B-4** `OrderService.java` (interface) — agregar `String userEmail` a la firma de `placeBuyOrder` y `placeSellOrder`.
- [x] **T-B-5** `OrderServiceImpl.java` — actualizar firmas de `placeBuyOrder`/`placeSellOrder`; completar `dispatchAsyncEvents` con `executedPrice`, `commission`, `userEmail`; leer `order.getUserEmail()` en `executeQueuedBuyOrder` y `executeQueuedSellOrder`.
- [x] **T-B-6** `QueuedOrderService.java` (interface) — agregar `String userEmail` a la firma de `enqueueOrder`.
- [x] **T-B-7** `QueuedOrderServiceImpl.java` — guardar `userEmail` en el `Order` al encolar.
- [x] **T-B-8** `NotificationClient.java` — corregir URL: `/api/v1/notifications` → `/api/v1/internal/notifications`.
- [x] **T-B-9** Tests existentes — actualizar firmas de mocks en `OrderControllerTest`, `OrderControllerQueueTest`, `OrderServiceImplTest`, `QueuedOrderServiceImplTest`. ✅ 47 tests, 0 fallos.

## Bloque C — notification-service (implementación completa)

- [x] **T-C-1** `pom.xml` — quitar `spring-boot-starter-data-jpa` y `postgresql`; agregar `spring-boot-starter-mail`, `spring-retry`, `spring-boot-starter-aop`.
- [x] **T-C-2** `application.yaml` — puerto 8084, mail SMTP (vars de entorno), URLs de identity-service y audit-service.
- [x] **T-C-3** `SecurityConfig.java` — `permitAll` para todos los endpoints (servicio interno sin JWT).
- [x] **T-C-4** `AsyncConfig.java` — `@EnableAsync`.
- [x] **T-C-5** `dto/NotificationRequest.java` — campos: `userId`, `eventType`, `data` (Map<String, Object>).
- [x] **T-C-6** `dto/UserPreferencesResponse.java` — campos: `orderConfirm`, `notifEmail`.
- [x] **T-C-7** `dto/AuditEventRequest.java` — campos: `timestamp`, `service`, `eventType`, `userId`, `correlationId`, `details`.
- [x] **T-C-8** `client/IdentityClient.java` — `GET /api/v1/internal/users/{userId}/preferences`; lanza excepción en fallo.
- [x] **T-C-9** `client/AuditClient.java` — `POST /api/v1/internal/events` con `@Async`; swallows exceptions.
- [x] **T-C-10** `service/NotificationService.java` — interface con `processNotification(NotificationRequest)`.
- [x] **T-C-11** `service/impl/NotificationServiceImpl.java` — lógica principal: obtener prefs → defaults si falla → skip si `!notifEmail || !orderConfirm` → enviar email con `@Retryable(3, 1s)` → auditar `NOTIFICATION_SENT` o `NOTIFICATION_FAILED`.
- [x] **T-C-12** `controller/NotificationController.java` — `POST /api/v1/internal/notifications`, responde 202, sin JWT.
- [x] **T-C-13** `NotificationServiceApplicationTests.java` — actualizar para no fallar en startup (sin datasource).

## Bloque D — tests notification-service

- [x] **T-D-1** `NotificationServiceImplTest.java` — 4 tests unitarios (`@ExtendWith(MockitoExtension)`):
  - `processNotification_orderConfirmTrue_sendsEmail`
  - `processNotification_orderConfirmFalse_skipsEmail`
  - `processNotification_identityUnavailable_usesDefaultsAndSendsEmail`
  - `processNotification_mailFails_registersNotificationFailed`
- [x] **T-D-2** `NotificationControllerTest.java` — integración con `@SpringBootTest @AutoConfigureMockMvc`:
  - `postNotification_validBody_returns202`
  - `postNotification_noAuth_returns202`

## Verificación final

- [x] **T-V-1** `mvn test` en `identity-service` — 58 tests, 0 fallos. ✅
- [x] **T-V-2** `mvn test` en `trading-service` — 47 tests, 0 fallos. ✅
- [x] **T-V-3** `mvn test` en `notification-service` — 7 tests, 0 fallos. ✅
