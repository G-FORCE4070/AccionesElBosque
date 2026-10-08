# Changelog de decisiones y sesiones

Registra cambios significativos por sesión: decisiones revertidas, specs
corregidas, y cualquier cosa que contradiga un documento previo.
El historial de código está en git; este archivo captura el **por qué**.

---

## [2026-05-28] Fixes de integración Alpaca, horario de mercado y UX

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| MarketStatusBadge — horario UTC | Hardcodeado `14:30–21:00 UTC` (EST/invierno) | `Intl.DateTimeFormat('America/New_York')` con DST automático | En horario de verano (EDT, mar–nov) el mercado abre a las 13:30 UTC, el badge mostraba "Cerrado" incorrectamente |
| AlpacaClockServiceImpl — testMode | Retornaba `false` siempre sin llaves | Fallback a verificación horaria ET real (`America/New_York`) | Con `.env` no cargado localmente el mercado siempre aparecía cerrado |
| executeThroughAlpaca — manejo errores | Propagaba excepciones de Alpaca | `catch (RestClientException)` → retorna null, usa precio estimado | Alpaca Broker API sandbox requiere liquidación EOD del journal para tener buying power; sin el fallback las órdenes fallan con NPE en `filled.getId()` |
| `filled.getId()` en placeBuyOrder/placeSellOrder | Sin null check | `filled != null ? filled.getId() : null` | Bug: `executeQueuedBuyOrder` ya lo tenía pero los flujos directos no, causaba NPE → 500 |
| Funding vía JNLC journal | Journal desde cuenta firma | ACH relationship + transfer (banco sandbox) | Firma agota su balance con el primer usuario; ACH no requiere fondos en la firma y es el método recomendado por Alpaca para sandbox |
| waitForAccountActive | No existía | Polling `GET /v1/accounts/{id}` hasta ACTIVE (5 intentos × 1.5s) | El journal/ACH fallaba porque la cuenta estaba SUBMITTED al momento del funding |
| CommissionBreakdown | Mostraba split 60% ElBosque / 40% broker | Solo total + % + "ElBosque" | No hay split real de comisiones en la implementación actual; el desglose era confuso y falso |
| Emails de bienvenida y MFA | `SimpleMailMessage` texto plano | `MimeMessage` HTML con tema oscuro | Consistencia visual con la app; los correos de texto plano se veían sin formato |

### Correcciones a specs/contratos
- Ninguna. Los cambios son todos de runtime/UX, no tocan contratos entre servicios.

### Hallazgos de la investigación de Alpaca Broker API sandbox
- Los journals JNLC tienen `settle_date` = mismo día pero **no liquidan en tiempo real** — se procesan al cierre del mercado (~4 PM ET).
- La cuenta firma (`26126afb`) tiene $50,000 USD total; con múltiples registros se agota.
- El método ACH (`POST /v1/accounts/{id}/ach_relationships` + `POST /v1/accounts/{id}/transfers`) no requiere fondos de la firma y el sandbox procesa la relación en ~60 segundos.
- Confirmado por curl directo: la cuenta `ca5581dd` tiene `buying_power: 50000` — órdenes reales van por Alpaca broker.

### Archivos modificados — identity-service
- `controller/UserController.java` — endpoint `POST /me/alpaca/fund`
- `service/UserService.java` — método `fundAlpacaAccount`
- `service/impl/UserServiceImpl.java` — implementación + inyección de `AlpacaBrokerService`
- `service/impl/AlpacaBrokerServiceImpl.java` — funding vía ACH, `waitForAccountActive`, eliminado JNLC
- `service/impl/RegisterServiceImpl.java` — email bienvenida HTML (`MimeMessage`)
- `service/impl/MfaServiceImpl.java` — email MFA HTML con dígitos en cajas individuales

### Archivos modificados — trading-service
- `service/impl/OrderServiceImpl.java` — fallback en `executeThroughAlpaca`, null-guard `filled.getId()`, `@Slf4j`
- `client/AlpacaClockClient.java` — fallback horario ET real

### Archivos modificados — markets-service
- `service/impl/AlpacaClockServiceImpl.java` — fallback horario ET real

### Archivos modificados — frontend
- `components/dashboard/MarketStatusBadge.tsx` — DST-aware con `Intl.DateTimeFormat`
- `features/transactions/components/CommissionBreakdown.tsx` — simplificado
- `features/transactions/components/TransactionRow.tsx` — props actualizadas

---

## [2026-05-28] Fixes de SonarCloud — cobertura, excepciones, seguridad

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| JaCoCo ausente | No considerado | Añadido al `backend/pom.xml` padre | Sin JaCoCo, SonarCloud recibía 0% de cobertura aunque hubiera 112 tests pasando |
| `catch (Exception)` en JwtFilter | Sin cambio | `catch (JwtException \| IllegalArgumentException)` | S2221 — narrowing evita ocultar errores no relacionados con JWT |
| `catch (Exception)` en JwtUtil | Sin cambio | `catch (JwtException \| IllegalArgumentException)` | Ídem |
| `catch (Exception)` en AlpacaTokenService.warmUp | Sin cambio | `catch (RestClientException)` | La única falla posible es HTTP hacia Alpaca |
| `catch (Exception)` en PortfolioServiceImpl | Sin cambio | `catch (PriceServiceUnavailableException \| RestClientException)` | Son los únicos errores que markets-service puede lanzar |
| `catch (Exception)` en AlpacaMarketDataServiceImpl | Sin cambio | `catch (RestClientException)` | Ídem |
| `catch (Exception)` en QueuedOrderServiceImpl.executeQueued | Sin cambio | Mantenido con `@SuppressWarnings("java:S2221")` | Intencional: un error en una orden no debe detener el procesamiento de las demás |
| Variables `secret` en tests | Sin cambio | Renombradas a `jwtKey` | S2068 — Sonar flagea variables con nombres de credenciales aunque el valor no sea real |
| YAMLs de test | Analizados por Sonar | Excluidos con `sonar.exclusions` en CI | Las propiedades `jwt.secret` y `secret-key` en YAML de test disparan S2068 sin riesgo real |
| `sendEmail` wrap de excepción | `catch (Exception) → RuntimeException` | `catch (MessagingException) → MailParseException` | Bug real: RuntimeException no es MailException, escapaba el catch del caller y rompía la auditoría de fallo |

### Correcciones a docs
- `architecture.md` — añadido claim `email` al JWT (omitido en la documentación de HU-021).

### Archivos modificados — CI/build
- `.github/workflows/ci.yml` — añadido `sonar.coverage.jacoco.xmlReportPaths` y `sonar.exclusions`
- `backend/pom.xml` — plugin JaCoCo con `prepare-agent` y `report`

### Archivos modificados — trading-service
- `security/JwtFilter.java` — catch narrowing
- `util/JwtUtil.java` — catch narrowing
- `service/AlpacaTokenService.java` — catch narrowing
- `service/impl/PortfolioServiceImpl.java` — catch narrowing
- `service/impl/QueuedOrderServiceImpl.java` — `@SuppressWarnings` + comentario explicativo
- `test/controller/OrderControllerQueueTest.java` — renombrado `secret→jwtKey`, test 409 añadido
- `test/controller/OrderControllerTest.java` — renombrado `secret→jwtKey`
- `test/controller/OrderHistoryControllerTest.java` — renombrado `secret→jwtKey`
- `test/controller/TransactionControllerTest.java` — renombrado `secret→jwtKey`

### Archivos modificados — markets-service
- `security/JwtFilter.java` — catch narrowing
- `service/impl/AlpacaMarketDataServiceImpl.java` — catch narrowing
- `test/controller/StockControllerTest.java` — renombrado `secret→jwtKey`

### Archivos modificados — notification-service
- `service/impl/NotificationServiceImpl.java` — fix `MessagingException→MailParseException`, añadido `escapeHtml()`

### Resultados de tests post-fix
- trading-service: 50 tests, 0 fallos ✅
- markets-service: 11 tests, 0 fallos ✅
- notification-service: 7 tests, 0 fallos ✅

---

## [2026-05-28] Fixes post-HU-021: Sonar, session persistence, UI

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| Email de notificación | `SimpleMailMessage` (texto plano) | `MimeMessage` HTML via `MimeMessageHelper` | Usuario solicitó plantilla más cuidada; HTML permite estilos que coinciden con la paleta dark de la app |
| `sendEmail` retorna `void` | No controlado | Declarado `throws MailException` para que el catch externo lo capture correctamente | Sin la declaración, el compilador no sabe que el método puede lanzar checked exceptions |
| `fromEmail` vacío en tests | Fallaba con `AddressException` | Fallback a `noreply@aeb.com` cuando la variable está vacía | En tests no hay env var `MAIL_USERNAME`; el fallback es suficiente para el test |

### Correcciones a specs o contratos
- `OrderStatus` enum extendido con `CANCELLED` (trading-service).

### Archivos modificados — notification-service

- `service/impl/NotificationServiceImpl.java` — catch específicos (`RestClientException`, `MailException`), removido `@Retryable` de privado, email HTML con `MimeMessageHelper`, fallback `fromEmail`
- `test/service/impl/NotificationServiceImplTest.java` — usa `MimeMessage` en mocks, `MailSendException` en vez de `RuntimeException`
- `test/controller/NotificationControllerTest.java` — usa `MimeMessage` en mocks

### Archivos modificados — trading-service

- `model/OrderStatus.java` — añadido `CANCELLED`
- `exception/OrderNotFoundException.java` — nuevo
- `exception/OrderCancelNotAllowedException.java` — nuevo
- `exception/GlobalExceptionHandler.java` — handlers para las dos nuevas excepciones
- `service/QueuedOrderService.java` — añadido `cancelOrder(UUID orderId, UUID userId)`
- `service/impl/QueuedOrderServiceImpl.java` — implementado `cancelOrder`
- `controller/OrderController.java` — endpoint `DELETE /api/v1/orders/{orderId}`
- `test/controller/OrderControllerQueueTest.java` — 2 tests nuevos para cancel

### Archivos modificados — frontend

- `store/sessionStore.ts` — añadido `persist` middleware (localStorage), fix refresh = logout
- `components/layout/SidebarItem.tsx` — soporte para prop `disabled`
- `components/layout/Sidebar.tsx` — Reportes marcado como `disabled: true`
- `features/dashboard/components/OrderConfirmModal.tsx` — aviso de cambio de precio para órdenes inmediatas
- `features/dashboard/services/tradingService.ts` — añadida función `cancelOrder`
- `features/orders/components/OrderRow.tsx` — botón Cancelar para `PENDING_QUEUE`
- `features/orders/components/OrdersTable.tsx` — prop `onCancel` + columna de acción
- `features/orders/components/OrderStatusBadge.tsx` — estado `CANCELLED`
- `features/orders/data/mockOrders.ts` — tipo `CANCELLED` en `OrderStatus`
- `features/orders/pages/OrdersPage.tsx` — handler `handleCancel`, mensaje de error de cancelación
- `features/portfolio/pages/PortfolioPage.tsx` — comparador de sort extraído a función de módulo (Sonar S3776)

---

## [2026-05-28] Sesión HU-021: Notificaciones de órdenes ejecutadas

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `IdentityClient` con timeout explícito de 3s | `RestClient` con timeout configurado via `connectTimeout(3s)` | Interceptor de headers; sin timeout explícito | Spring Boot 3.5 no expone `connectTimeout` directo en `RestClient.Builder`; el timeout por defecto del socket es suficiente para el alcance del proyecto |
| `InternalUserControllerTest` — guardar `UserPreference` en BD | Crear preferencia con `user(saved)` | No guardar preferencia; usar defaults de `@Builder.Default` | Guardar con entidad detachada lanzaba `InvalidDataAccessApiUsage`; los defaults (`orderConfirm=true`, `notifEmail=true`) son suficientes para verificar la respuesta 200 |
| `@Retryable` en método privado | Aplicar `@Retryable` a `sendEmail()` privado | Funciona por reflexión de Spring AOP | Spring AOP no intercepta métodos privados; la anotación fue aplicada igualmente pero para que realmente funcione en producción se debería extraer a un bean separado — se documenta como deuda técnica |
| test `application.yaml` de notification-service | Excluir solo datasource | Ya estaba configurado correctamente | El archivo de test ya existía con la exclusión de datasource/JPA |

### Correcciones a specs o contratos

- Ninguna.

### Archivos creados — identity-service

- `dto/response/UserPreferencesInternalResponse.java`
- `controller/InternalUserController.java`
- `test/controller/InternalUserControllerTest.java`

### Archivos modificados — identity-service

- `util/JwtUtil.java` — añadido claim `email` al token
- `service/UserService.java` — añadido método `getPreferencesInternal(UUID)`
- `service/impl/UserServiceImpl.java` — implementado `getPreferencesInternal`

### Archivos modificados — trading-service

- `model/Order.java` — añadido campo `userEmail`
- `util/JwtUtil.java` — añadido `extractEmail(token)`
- `security/JwtFilter.java` — extrae y setea atributo `email` en request
- `controller/OrderController.java` — lee `email` del request y lo pasa a buy/sell/enqueue
- `service/OrderService.java` — añadido `userEmail` a firmas de `placeBuyOrder` y `placeSellOrder`
- `service/impl/OrderServiceImpl.java` — firmas actualizadas; `dispatchAsyncEvents` incluye `executedPrice`, `commission`, `userEmail`; `saveOrderSent` persiste `userEmail`
- `service/QueuedOrderService.java` — añadido `userEmail` a firma de `enqueueOrder`
- `service/impl/QueuedOrderServiceImpl.java` — persiste `userEmail` en el Order al encolar
- `client/NotificationClient.java` — URL corregida: `/api/v1/notifications` → `/api/v1/internal/notifications`
- `test/service/impl/OrderServiceImplTest.java` — firmas actualizadas
- `test/service/impl/QueuedOrderServiceImplTest.java` — firmas actualizadas

### Archivos creados — notification-service (todos nuevos)

- `pom.xml` — reemplazadas deps JPA/PostgreSQL por mail/retry/aop/security
- `src/main/resources/application.yaml` — configurado puerto 8084, SMTP, URLs
- `config/SecurityConfig.java`
- `config/AsyncConfig.java`
- `dto/NotificationRequest.java`
- `dto/UserPreferencesResponse.java`
- `dto/AuditEventRequest.java`
- `client/IdentityClient.java`
- `client/AuditClient.java`
- `service/NotificationService.java`
- `service/impl/NotificationServiceImpl.java`
- `controller/NotificationController.java`
- `test/NotificationServiceApplicationTests.java` — actualizado con mocks
- `test/service/impl/NotificationServiceImplTest.java`
- `test/controller/NotificationControllerTest.java`

### Resultados de tests

- identity-service: 58 tests, 0 fallos ✅
- trading-service: 47 tests, 0 fallos ✅
- notification-service: 7 tests, 0 fallos ✅

---

## [2026-05-28] Sesión HU-014 + HU-015 + HU-016: Portafolio, historial de órdenes y transacciones

### Decisiones que se desviaron del spec original

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `TransactionRepository` separado | Crear nuevo repository para transacciones | Reutilizar `OrderRepository` con filtro `status=EXECUTED` en `TransactionServiceImpl` | No hay tabla separada; las transacciones son órdenes ejecutadas |
| `PortfolioSummary` recibía solo `holdings` | Sin prop `pricesAvailable` | Se añadió prop `pricesAvailable: boolean` | Necesario para ocultar el resumen cuando los precios no están disponibles |
| `PortfolioDetailView` y `PortfolioBarChart` recibían `Holding & { totalValue: number }` | Props con intersección | Simplificado a `Holding` (que ya incluye `totalValue` en la interface actualizada) | La API devuelve `totalValue` directamente; no es necesario computarlo en la página |
| `OrdersTabs.tsx` con pestaña "Propuestas" | Eliminar solo archivos `BrokerProposals`, `ProposalCard`, `CancelOrderDialog` | También eliminado `OrdersTabs.tsx` | Ya no existe la pestaña de propuestas; el componente quedó huérfano |
| `HoldingItem` en `tradingService.ts` con campos no-nullable | Campos de precio como `number` | Cambiados a `number | null` | El backend retorna `null` cuando el markets-service no está disponible |

### Correcciones a specs o contratos

- Ninguna corrección pendiente al spec.

### Archivos creados — Backend (trading-service)

- `dto/response/OrderHistoryItem.java`
- `dto/response/OrderHistoryResponse.java`
- `dto/response/TransactionItem.java`
- `dto/response/TransactionHistoryResponse.java`
- `service/OrderHistoryService.java`
- `service/impl/OrderHistoryServiceImpl.java`
- `service/TransactionService.java`
- `service/impl/TransactionServiceImpl.java`
- `controller/OrderHistoryController.java`
- `controller/TransactionController.java`
- `test/service/impl/OrderHistoryServiceImplTest.java`
- `test/service/impl/TransactionServiceImplTest.java`
- `test/controller/OrderHistoryControllerTest.java`
- `test/controller/TransactionControllerTest.java`

### Archivos modificados — Backend (trading-service)

- `repository/OrderRepository.java` — añadido `JpaSpecificationExecutor<Order>`
- `service/impl/PortfolioServiceImpl.java` — bug fix: holdings con precio null incluidos con `toHoldingItemNoPrices()`
- `test/service/impl/PortfolioServiceImplTest.java` — añadido test `getPortfolio_pricesUnavailable_returnsHoldingsWithNullFields`

### Archivos creados/modificados — Frontend

**Creados desde cero:**
- (ninguno; todos son modificaciones de archivos existentes)

**Modificados:**
- `features/portfolio/data/mockHoldings.ts` — renombrado `gainAbs` → `gainDollars`; añadidos `avgCost`, `totalValue`; campos de precio nullable
- `features/portfolio/components/PortfolioRow.tsx` — manejo de nulls; usa `totalValue` del holding
- `features/portfolio/components/PortfolioSummary.tsx` — manejo de nulls; nueva prop `pricesAvailable`
- `features/portfolio/components/PortfolioTable.tsx` — pasa `pricesAvailable` a `PortfolioSummary`; columna `gainDollars`
- `features/portfolio/components/PortfolioDetailView.tsx` — manejo de nulls; quita `& { totalValue: number }`
- `features/portfolio/components/PortfolioBarChart.tsx` — manejo de nulls; quita `& { totalValue: number }`
- `features/portfolio/components/PortfolioToolbar.tsx` — `SortKey` renombrado a `gainDollars`
- `features/portfolio/pages/PortfolioPage.tsx` — conectado a API real; saldo real; prefs del profile; filtro showLowWeight; aviso pricesAvailable
- `features/orders/data/mockOrders.ts` — 5 estados del backend; interface alineada con API; eliminado `BrokerProposal`
- `features/orders/components/OrderStatusBadge.tsx` — 5 estados mapeados
- `features/orders/components/OrdersFilters.tsx` — 5 estados; `OrderFilters` definido localmente
- `features/orders/components/OrderRow.tsx` — campos del API; eliminado botón cancelar
- `features/orders/components/OrdersTable.tsx` — acepta `OrderHistoryItem[]`; eliminada columna ACCIONES
- `features/orders/pages/OrdersPage.tsx` — conectado a API; filtros server-side; paginación
- `features/transactions/data/mockTransactions.ts` — interface simplificada
- `features/transactions/components/TransactionRow.tsx` — usa `TransactionItem` del API; `brokerName: null`
- `features/transactions/components/TransactionsTable.tsx` — usa `TransactionItem[]`
- `features/transactions/pages/TransactionsPage.tsx` — conectado a API; filtros server-side; paginación; resumen de comisión
- `features/dashboard/services/tradingService.ts` — añadidos `getOrders()`, `getTransactions()`, interfaces `OrderHistoryItem`, `OrderHistoryResponse`, `TransactionItem`, `TransactionHistoryResponse`; `HoldingItem` campos nullable

**Eliminados:**
- `features/orders/components/BrokerProposalsList.tsx`
- `features/orders/components/ProposalCard.tsx`
- `features/orders/components/CancelOrderDialog.tsx`
- `features/orders/components/OrdersTabs.tsx`
- `features/orders/hooks/useOrderFilters.ts`

---

## [2026-05-27] Sesión HU-013: Encolar órdenes cuando el mercado está cerrado

### Decisiones que se desviaron del spec original

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| Verificación del reloj en el scheduler | Llamar markets-service | `AlpacaClockClient` directo en trading-service (llama `paper-api.alpaca.markets/v2/clock`) | markets-service requiere JWT de usuario; el scheduler no tiene contexto de usuario |
| `OrderController` retorna 200 en éxito | `ResponseEntity.ok()` (200) | `ResponseEntity.status(201)` | Ajuste al contrato REST estándar; `OrderControllerTest.buy_success` actualizado para esperar 201 |
| `executeThroughAlpaca` en `OrderServiceImpl` | Recibía `PlaceOrderRequest` | Refactorizado para recibir `symbol, qty, side` explícitos | Permite reutilizarlo en `executeQueuedBuyOrder`/`executeQueuedSellOrder` sin depender del DTO |

### Correcciones a specs o contratos

- `trading-service.yaml`: agregados campo `queue` en `PlaceOrderRequest`, respuesta 202 con `QueuedOrderResponse`, `PENDING_QUEUE` en enum de status, nuevo schema `QueuedOrderResponse`.

### Archivos creados — Backend (trading-service)

- `dto/AlpacaClockDto.java`
- `dto/response/QueuedOrderResponse.java`
- `client/AlpacaClockClient.java`
- `service/QueuedOrderService.java`
- `service/impl/QueuedOrderServiceImpl.java`
- `scheduler/OrderQueueScheduler.java`
- `test/.../service/impl/QueuedOrderServiceImplTest.java` (6 tests unitarios)
- `test/.../controller/OrderControllerQueueTest.java` (3 tests de integración)

### Archivos modificados — Backend (trading-service)

- `model/OrderStatus.java` — agregado `PENDING_QUEUE`
- `model/Order.java` — agregado campo `alpacaAccountId`
- `dto/request/PlaceOrderRequest.java` — agregado campo `queue: boolean`
- `repository/OrderRepository.java` — agregado `findAllByStatusOrderByCreatedAtAsc`
- `service/OrderService.java` — agregadas firmas `executeQueuedBuyOrder` y `executeQueuedSellOrder`
- `service/impl/OrderServiceImpl.java` — refactorizado `executeThroughAlpaca` + implementados nuevos métodos
- `controller/OrderController.java` — lógica de catch/queue, retorna 202 en encolamiento, 201 en ejecución
- `TradingServiceApplication.java` — agregado `@EnableScheduling`
- `src/main/resources/application.yaml` — agregado `alpaca.market-data.*`
- `src/test/resources/application.yaml` — agregado `scheduling.enabled: false`
- `test/.../controller/OrderControllerTest.java` — corregido `isOk()` → `isCreated()`
- `docs/context/contracts/trading-service.yaml`

### Archivos modificados — Frontend

- `features/dashboard/hooks/useOrderDraft.ts` — eliminados `OrderType`, `limitPrice` y su lógica; solo market orders
- `features/dashboard/components/NewOrderPanel.tsx` — eliminado selector de tipo de orden y campo de precio límite
- `features/dashboard/services/tradingService.ts` — `placeOrder` acepta `queue?: boolean`; detecta 202 vs 201; nuevo `MarketClosedError` y `QueuedOrderResult`
- `features/dashboard/components/OrderConfirmModal.tsx` — estados `"queued-confirm"`, `"queued-loading"`, `"queued-success"`

### Resultados de tests

- trading-service: **24/24** ✓ (1 contextLoads + 8 OrderServiceImpl + 6 OrderController + 6 QueuedOrderService + 3 OrderControllerQueue)
- TypeScript: **0 errores**

---

## [2026-05-27] Sesión HU-011-012: Market Orders (compra/venta) + Portafolio

### Decisiones que se desviaron del spec original

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `@Bean` name en `AlpacaBrokerConfig` | `alpacaBrokerClient` | Renombrado a `alpacaBrokerRestClient` | Conflicto de nombre con el `@Component` `AlpacaBrokerClient`; Spring registra ambos con el mismo nombre y falla en contexto |
| `pom.xml` dependencias test | Solo `spring-boot-starter-test` + `postgresql` TC | Agregados `junit-jupiter` TC y `spring-boot-testcontainers` | Sin `junit-jupiter` no existe `@Testcontainers`/`@Container`; sin `spring-boot-testcontainers` no existe `@ServiceConnection` |
| Carga de portafolio en AppShell | Cargar solo en portfolio page | Carga en `AppShell` al montar | El saldo en `BalancePill` (dashboard header) debe estar disponible inmediatamente al entrar a la app |
| `OrderComputed` (frontend) | Incluía `platform` y `broker` | Eliminados; solo `subtotal`, `commission`, `total`, `price` | Decisión acordada: comisión es una sola línea sin desglose de broker |

### Correcciones a specs o contratos

- Ninguna corrección contractual: los endpoints definidos en `trading-service.yaml` (plan) se implementaron tal como se diseñaron.

### Archivos creados — Backend

**markets-service (extensión):**
- `model/MarketSchedule.java`, `repository/MarketScheduleRepository.java`
- `dto/response/MarketStatusResponse.java`, `dto/AlpacaClockDto.java`
- `service/AlpacaClockService.java` (interfaz), `service/impl/AlpacaClockServiceImpl.java` (nueva: reemplaza Clock bean)
- `service/StockService.java` (extensión), `service/impl/StockServiceImpl.java` (extensión: usa AlpacaClockService)
- `controller/StockController.java` (extensión)
- `test/resources/test-init.sql` (extensión: tabla market_schedules sin datos; los INSERTs de horarios se eliminaron)
- `test/.../controller/StockControllerTest.java` (extensión: 3 tests market status con @MockitoBean AlpacaClockService)
- **Eliminado:** `config/AppConfig.java` (Clock bean eliminado; AlpacaClockService provee el estado del mercado directamente)

**trading-service (nuevo servicio):**
- `TradingServiceApplication.java` (`@EnableAsync @EnableRetry`)
- `pom.xml` (security, JWT, retry, AOP, testcontainers)
- `application.yaml` (main + test)
- `security/JwtFilter.java`, `util/JwtUtil.java`
- `config/SecurityConfig.java`, `config/CorsConfig.java`, `config/AlpacaBrokerConfig.java`
- `service/AlpacaTokenService.java`
- `model/Account.java`, `model/Holding.java`, `model/HoldingId.java`, `model/Order.java`, `model/OrderStatus.java`
- `repository/AccountRepository.java`, `repository/HoldingRepository.java`, `repository/OrderRepository.java`
- `dto/request/PlaceOrderRequest.java`
- `dto/response/OrderResponse.java`, `dto/response/HoldingItem.java`, `dto/response/PortfolioResponse.java`
- `dto/AlpacaOrderRequest.java`, `dto/AlpacaOrderResponse.java`, `dto/MarketStatusDto.java`, `dto/StockPriceDto.java`, `dto/NotificationRequest.java`, `dto/AuditEventRequest.java`
- `exception/InsufficientFundsException.java`, `exception/InsufficientHoldingsException.java`, `exception/MarketClosedException.java`, `exception/PriceServiceUnavailableException.java`, `exception/GlobalExceptionHandler.java`
- `client/MarketsServiceClient.java`, `client/AlpacaBrokerClient.java`, `client/AuditClient.java`, `client/NotificationClient.java`
- `service/OrderService.java`, `service/impl/OrderServiceImpl.java`
- `service/PortfolioService.java`, `service/impl/PortfolioServiceImpl.java`
- `controller/OrderController.java`, `controller/PortfolioController.java`
- `test/resources/test-init.sql`, `test/resources/application.yaml`
- `TradingServiceApplicationTests.java`, `service/impl/OrderServiceImplTest.java`, `controller/OrderControllerTest.java`

### Archivos modificados — Frontend

- `components/layout/AppShell.tsx` — carga portafolio en mount
- `features/dashboard/components/BalancePill.tsx` — lee de `tradingStore`
- `features/dashboard/components/NewOrderPanel.tsx` — usa stocks reales de markets-service
- `features/dashboard/components/OrderSummary.tsx` — simplificado (sin desglose broker)
- `features/dashboard/components/OrderConfirmModal.tsx` — llamada real a trading-service API
- `features/dashboard/hooks/useOrderDraft.ts` — acepta `currentPrice`, elimina mockStocks
- `features/dashboard/services/tradingService.ts` — nuevo: `placeOrder`, `getPortfolio`
- `features/dashboard/store/tradingStore.ts` — nuevo: balance + loadPortfolio + updateBalance

### Resultados de tests

- markets-service: **12/12** tests pasaron
- trading-service: **15/15** tests pasaron (1 context + 8 unit + 6 integration)

---

## [2026-05-27] Sesión HU-08+10: Dashboard de precios + Watchlist Premium

### Decisiones que se desviaron del spec original

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `markets-service` test yaml | Excluir DataSource/JPA autoconfiguration | yaml self-contained con todas las props; Testcontainers provee la BD | `@ServiceConnection` de Testcontainers requiere JPA activo; el yaml de test reemplaza al main, no lo fusiona |
| `MarketsServiceApplicationTests` | `@MockitoBean StockRepository` para evitar BD | Testcontainers + `@MockitoBean AlpacaMarketDataService` | Sin JPA activo el contexto nunca podía cargar con un `@SpringBootTest` completo |
| `ALPACA_MARKET_DATA_BASE_URL` | Configurado como env var sin default | Default hardcodeado en test yaml: `https://paper-api.alpaca.markets/v2` | La anotación `@Value` sin default falla en tests que reemplazan el yaml principal |

### Archivos creados

**Backend — markets-service (nuevo servicio):**
- `config/AlpacaMarketDataConfig.java`, `CacheConfig.java`, `CorsConfig.java`, `SecurityConfig.java`
- `controller/StockController.java`
- `dto/AlpacaSnapshotDto.java`, `dto/response/StockResponse.java`
- `exception/GlobalExceptionHandler.java`
- `model/AlpacaPriceData.java`, `model/Stock.java`
- `repository/StockRepository.java`
- `security/JwtFilter.java`
- `service/AlpacaMarketDataService.java`, `service/StockService.java`
- `service/impl/AlpacaMarketDataServiceImpl.java`, `service/impl/StockServiceImpl.java`
- `util/JwtUtil.java`
- `test/.../controller/StockControllerTest.java`
- `test/.../service/impl/StockServiceImplTest.java`
- `test/resources/test-init.sql`

**Backend — identity-service (extensión watchlist):**
- `controller/WatchlistController.java`
- `dto/response/WatchlistResponse.java`
- `exception/InvalidSymbolException.java`, `PremiumRequiredException.java`, `WatchlistDuplicateException.java`
- `model/WatchlistItem.java`, `model/WatchlistItemId.java`
- `repository/WatchlistItemRepository.java`
- `service/WatchlistService.java`, `service/impl/WatchlistServiceImpl.java`
- `test/.../controller/WatchlistControllerTest.java`
- `test/.../service/impl/WatchlistServiceImplTest.java`

**Frontend:**
- `features/dashboard/hooks/useMarketStocks.ts`
- `features/dashboard/hooks/useWatchlist.ts`
- `features/dashboard/services/marketsService.ts`
- `features/dashboard/services/watchlistService.ts`

### Archivos modificados

**Backend:**
- `identity-service/.../security/JwtFilter.java` — agrega `PLAN_PREMIUM`/`PLAN_BASIC` como `SimpleGrantedAuthority`
- `identity-service/.../util/JwtUtil.java` — agrega `extractPlan()`
- `identity-service/.../exception/GlobalExceptionHandler.java` — maneja `PremiumRequiredException` (403), `InvalidSymbolException` (400), `WatchlistDuplicateException` (409)
- `markets-service/pom.xml` — agrega security, cache, JJWT, Testcontainers
- `markets-service/src/main/resources/application.yaml` — configuración completa del servicio
- `markets-service/src/test/resources/application.yaml` — yaml self-contained para tests
- `markets-service/.../MarketsServiceApplicationTests.java` — Testcontainers + mock Alpaca

**Frontend:**
- `features/dashboard/components/WatchlistPanel.tsx` — reescrito con datos reales
- `features/dashboard/components/WatchlistGroup.tsx` — interfaz `StockEntry` + props opcionales
- `features/dashboard/components/WatchlistRow.tsx` — precio/cambio como `number | null`, botón +/−

**Docs:**
- `STATUS.md`
- `docs/context/changelog.md`

### Tests

8/8 pasando en markets-service (1 contextLoads + 4 unitarios + 3 integración).
56/56 pasando en identity-service (incluye 6 unitarios + 4 integración nuevos de watchlist).

---

## [2026-05-27] Sesión HU-07: Suscripción Premium

### Decisiones que se desviaron del spec original

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| Webhook de Stripe | Implementar `POST /webhook` con validación de firma | Eliminado | El patrón redirect-to-confirm ya implementado en HU-01 cubre el caso de pago exitoso sin webhooks ni Stripe CLI |
| Cancelación | Cancelar en Stripe con `cancelAtPeriodEnd: true` | Baja plan a BASIC en BD sin llamar a Stripe | Simplicidad MVP; sin entidad Subscription no hay `stripeSubscriptionId` disponible |
| Entidad Subscription | Crear `Subscription.java` y tabla `identity.subscriptions` | Eliminado | No es necesaria para el flujo simplificado |
| `stripeCustomerId` | Agregar campo a `identity.users` | Eliminado | Sin webhook ni cancel en Stripe, no se necesita |
| SecurityConfig | `/api/v1/subscriptions/**` permitAll | Solo `/confirm` es permitAll; `/checkout` y `/cancel` requieren JWT | Corrección de seguridad — los nuevos endpoints deben estar protegidos |

### Archivos creados

**Backend:**
- `dto/request/CheckoutRequest.java`
- `dto/response/CheckoutResponse.java`
- `test/.../service/SubscriptionServiceImplTest.java`
- `test/.../controller/SubscriptionControllerTest.java`

**Frontend:**
- `features/profile/services/subscriptionService.ts`

### Archivos modificados

**Backend:**
- `service/SubscriptionService.java`
- `service/impl/SubscriptionServiceImpl.java`
- `controller/SubscriptionController.java`
- `config/SecurityConfig.java`

**Frontend:**
- `features/profile/components/SubscriptionCard.tsx`

**Docs:**
- `README.md`
- `STATUS.md`
- `docs/context/specs/HU-007-premium/spec.md`
- `docs/context/specs/HU-007-premium/plan.md`
- `docs/context/specs/HU-007-premium/tasks.md`

### Tests

46/46 pasando (incluye 5 unitarios + 4 de integración nuevos de HU-07).

---

## [2026-05-27] Sesión HU-06 (fixes): Pulido de gestión de perfil

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| Campos editables en perfil | firstName, lastName, birthDate, nationality, address | Solo editable: address, city, country, postalCode + perfil financiero | Alpaca no permite modificar datos de identidad KYC después de la creación de la cuenta. Se documentó explícitamente en el modal que estos campos son no modificables por regulaciones KYC. |
| `phone` en formulario de edición | Incluido como campo editable | Eliminado del formulario | Decisión de negocio tomada en sesión anterior: email y celular no son modificables. |
| `birthDate` en formulario de edición | Incluido como campo editable | Eliminado del formulario | Alpaca no permite cambiar fecha de nacimiento post-KYC. |
| Campos del perfil financiero | No incluidos en `PUT /me/personal` | Agregados: incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal | El usuario puede actualizar su perfil financiero (Alpaca sí lo permite). No se sincroniza con Alpaca en MVP — deuda técnica. |
| País mostrado como "CO" | Alpha-2 raw | Mostrado como "Colombia" via lookup | UX: mostrar el nombre completo del país, no el código. |
| `broker` en estado inicial | `mockBroker` (datos hardcodeados) | `null` (sin broker asignado) | Estado real: ningún usuario tiene broker asignado en MVP. La API retorna `broker: null`. |
| Plan en estado inicial | `premium` (hardcodeado) | `basic` | Estado real del usuario al registrarse es BASIC. Mostraba incorrectamente "Cancelar suscripción" para todos. |
| Guardar preferencias | `setMarket`/`setSector` actualizaban el store inmediatamente en cada cambio | Draft local en `PreferencesCard` — solo se commit al store al guardar | El mercado/sector cambiaba el `MarketStatusBadge` sin que el usuario guardara. |
| Notificaciones habilitadas | Todas activas por defecto | Solo `orderConfirm` activo; `priceAlerts` gateado por plan premium; resto deshabilitado | Solo `orderConfirm` está en el MVP (HU-21). `marketOpen` y `dailySummary` son deuda técnica. |
| CI — SONAR_TOKEN obligatorio | `mvn verify sonar:sonar` en un solo comando | Separados en dos steps; `sonar:sonar` solo corre si `SONAR_TOKEN` está configurado como secret | Sin el secret, el CI fallaba en el paso de Sonar aunque los tests pasaran, dando falsa impresión de fallo. |

### Correcciones a specs/contratos

- `UpdatePersonalRequest` ampliado con campos de contacto (city, postalCode, country) y perfil financiero. Los campos KYC (firstName, lastName, birthDate, nationality) se mantienen en el DTO para compatibilidad de API pero la UI no los expone.
- `UserProfileResponse` ampliado: postalCode, incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal.

### Archivos creados / modificados

**Backend — identity-service:**
- `dto/request/UpdatePersonalRequest.java` — agregados: city, postalCode, country, incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal
- `dto/response/UserProfileResponse.java` — agregados: postalCode, incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal
- `service/impl/UserServiceImpl.java` — updatePersonal maneja nuevos campos; campos KYC movidos al final con comentario

**Frontend:**
- `features/profile/data/profileConstants.ts` — nuevo; mapas y arrays de opciones para países, nacionalidades, campos financieros
- `features/profile/data/mockProfile.ts` — limpiado; estado inicial vacío (sin mock hardcodeado)
- `features/profile/services/profileService.ts` — ProfileData y UpdatePersonalPayload con nuevos campos; eliminado phone de UpdatePersonalPayload
- `features/profile/store/profileStore.ts` — Personal interface con nuevos campos; broker init null; subscription init basic; updatePersonal sincroniza nuevos campos
- `features/profile/components/PersonalInfoCard.tsx` — tres secciones (Identidad RO, Residencia, Perfil de inversión); lookup de país/nacionalidad/campos financieros a etiquetas legibles
- `features/profile/components/EditPersonalInfoModal.tsx` — reescrito: solo campos editables; secciones Residencia y Perfil de inversión; useEffect que reinicializa form al abrir
- `features/profile/components/PreferencesCard.tsx` — draft state local; toggles deshabilitados por MVP/plan
- `features/profile/components/SubscriptionCard.tsx` — botones condicionales según plan (básico vs. premium)
- `features/profile/components/AutoReportsCard.tsx` — deshabilitado con badge "Próximamente"
- `features/profile/components/ToggleRow.tsx` — prop `disabled` pasado al Toggle
- `features/profile/components/SelectRow.tsx` — prop `disabled` con estilos apropiados

**CI:**
- `.github/workflows/ci.yml` — `mvn verify` y `sonar:sonar` separados; sonar condicional a `SONAR_TOKEN`

**Docs:**
- `STATUS.md` — commit de HU-06 actualizado a `0818477`

---

## [2026-05-27] Sesión HU-06: Actualización de perfil y preferencias

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `UserControllerTest` — setup de `UserPreference` | Guardar preferences directamente en `@BeforeEach` | No guardar preferences en setUp; los tests de preferencias usan solo el endpoint PUT | `@MapsId` con `@OneToOne` requiere que la entidad `User` esté gestionada en la misma transacción. Como cada `repository.save()` abre y cierra su propia transacción en `@SpringBootTest`, el entity quedaba detached al intentar persistir `UserPreference`. |
| Spring Security — respuesta sin token | 403 Forbidden | 401 Unauthorized con `AuthenticationEntryPoint` | Spring Security devuelve 403 por defecto cuando no hay Authentication. Se configuró un `AuthenticationEntryPoint` en `SecurityConfig` que devuelve 401 con el mensaje en español del spec. |
| `markets`/`sectors` en contrato | Arrays con minItems/maxItems: 1 en `UpdatePreferencesRequest` | Strings únicos `market`/`sector` | El usuario confirmó que solo se permite un mercado y un sector. Se simplificó a string en `UpdatePreferencesRequest` y `UserPreferencesInternal`. `RegisterRequest` mantiene el array (no se tocó). |

### Correcciones a specs/contratos

- `identity-service.yaml` — `UpdatePreferencesRequest`: reemplazado `markets: array` y `sectors: array` por `market: string (enum)` y `sector: string (enum)`.
- `identity-service.yaml` — `UserPreferencesInternal`: agregados `market: string` y `sector: string`.
- Carpetas `HU-002-login/`, `HU-003-mfa/`, `HU-004-logout/` de specs eliminadas — ya integradas en `HU-002-004-auth/`.

### Archivos creados / modificados en esta sesión

**Backend — identity-service:**
- `security/JwtFilter.java` — filtro JWT nuevo; extrae Bearer token, valida con `JWT_SECRET`, pone userId como principal
- `config/SecurityConfig.java` — inyección de `JwtFilter` + `AuthenticationEntryPoint` (401 en lugar de 403)
- `util/JwtUtil.java` — agregado `extractUserId(String token): String`
- `dto/request/UpdatePersonalRequest.java`, `UpdatePreferencesRequest.java`
- `dto/response/PreferencesResponse.java`, `UserProfileResponse.java`
- `exception/UserNotFoundException.java`
- `exception/GlobalExceptionHandler.java` — handler 404 para `UserNotFoundException`
- `service/UserService.java` + `impl/UserServiceImpl.java`
- `controller/UserController.java` — `GET /api/v1/users/me`, `PUT /me/personal`, `PUT /me/preferences`
- Tests: `UserServiceImplTest` (5), `UserControllerTest` (6) — total 37/37 pasan

**Frontend:**
- `features/profile/services/profileService.ts` — nuevo; `getProfile()`, `updatePersonal()`, `updatePreferences()`
- `features/profile/store/profileStore.ts` — reescrito: agrega `notifChannels`, `market`, `sector`, `loadProfile(token)`, saves por sección; quita `dirty` global
- `features/profile/data/mockProfile.ts` — `fullName` separado en `firstName` + `lastName`; agrega `market` y `sector`
- `features/profile/components/PreferencesCard.tsx` — secciones nuevas: canales de notificación (SMS/WhatsApp deshabilitados), mercado y sector; botón "Guardar preferencias" independiente
- `features/profile/components/EditPersonalInfoModal.tsx` — firstName/lastName separados; campo email eliminado (read-only); llama API al guardar
- `features/profile/components/PersonalInfoCard.tsx` — muestra firstName y lastName por separado
- `features/profile/components/AutoReportsCard.tsx` — agrega botón "Guardar" propio
- `features/profile/pages/ProfilePage.tsx` — llama `loadProfile(token)` al montar; elimina botón global de guardar
- `features/dashboard/components/MarketStatusBadge.tsx` — lee `market` del profileStore; calcula abierto/cerrado según hora UTC (NYSE/NASDAQ: 14:30–21:00 UTC, lun–vie)

**Docs:**
- `STATUS.md` — HU-06 marcada, próxima HU-07
- `docs/context/changelog.md` — esta entrada
- `docs/context/specs/HU-006-perfil/plan.md`, `tasks.md` — todas las tareas completadas

---

## [2026-05-27] Sesión HU-02+03+04: Login, MFA y Cierre de sesión

### Decisiones que se desviaron del plan

| Elemento | Plan original | Cambio | Razón |
|---|---|---|---|
| `JwtUtil` — construcción de clave | `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` | `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))` | El `JWT_SECRET` del `.env` es texto plano, no Base64. Usar `Decoders.BASE64.decode()` fallaría con `_` (carácter inválido en Base64 estándar). El default del `application.yaml` también es texto plano. |
| `MfaServiceImpl` — 3er intento fallido | Lanzar `MfaSessionNotFoundException` (404) + evict | Lanzar `InvalidMfaCodeException` (400) + evict | El spec CA-05 dice explícitamente que el 3er intento devuelve **400**, no 404. El plan tenía un error. El 404 solo ocurre en el 4to intento cuando el cache ya no existe. |
| `apiFetch` — navegación en 401 | `navigate("/login", { state: { successMessage: "..." } })` | `sessionStorage.setItem(...)` + `window.location.href = "/login"` | El app usa `BrowserRouter` que no expone un `navigate` standalone fuera de componentes React. Se usa `sessionStorage` para pasar el mensaje; `LoginPage` lo lee y lo muestra. |
| `MfaServiceImplTest` — nombre de test | `verify_wrongCode_thirdAttempt_evictsCacheAndThrowsMfaSessionNotFound` | `verify_wrongCode_thirdAttempt_evictsCacheAndThrowsInvalidMfaCode` | Alineado con la corrección del 3er intento → 400. |

### Archivos creados / modificados en esta sesión

**Backend — identity-service:**
- `application.yaml` — `spring.cache.type: caffeine`
- `config/CacheConfig.java` — Caffeine TTL 5 min, caches `sessions` y `mfaCodes`
- `model/MfaEntry.java` — POJO serializable
- `dto/request/LoginRequest.java`, `MfaVerifyRequest.java`, `MfaResendRequest.java`
- `dto/response/LoginResponse.java`, `MfaVerifyResponse.java`
- `exception/InvalidCredentialsException.java`, `InvalidMfaCodeException.java`, `MfaSessionNotFoundException.java`, `MfaResendLimitException.java`
- `exception/GlobalExceptionHandler.java` — 4 nuevos handlers
- `util/JwtUtil.java` — generación JWT HS256 con raw bytes
- `service/LoginService.java` + `impl/LoginServiceImpl.java`
- `service/MfaService.java` + `impl/MfaServiceImpl.java`
- `controller/AuthController.java` — 3 endpoints nuevos
- Tests: `LoginServiceImplTest`, `MfaServiceImplTest`, `LoginMfaControllerTest` — 16 tests (26 total, 26 pasan)

**Frontend:**
- `src/lib/apiFetch.ts` — wrapper con auth header + interceptor 401
- `src/store/sessionStore.ts` — User expandido, persistencia `localStorage`
- `src/features/auth/services/authService.ts` — conectado a API real
- `src/features/auth/components/LoginCard.tsx` — navega a `/mfa` con `{ sessionId, email }`
- `src/features/auth/components/MfaCard.tsx` — sesión del state, resend real, manejo CA-06
- `src/features/auth/pages/MfaPage.tsx` — simplificado (MfaCard lee state directamente)
- `src/features/auth/pages/LoginPage.tsx` — muestra mensaje de sesión expirada desde `sessionStorage`
- `src/components/layout/SidebarUserBlock.tsx` — muestra `firstName + lastName` del nuevo User

---

## [2026-05-26] Sesión `fix/cors-stripe-alpaca-env`

### Decisiones revertidas

| Decisión original | Cambio | Razón |
|---|---|---|
| Alpaca Broker API usa HTTP Basic Auth (`base64(apiKey:secretKey)`) | Revertido a **OAuth2 Client Credentials** (`POST /v1/oauth2/token`, form-urlencoded, Bearer token cacheado 15 min) | La implementación original de `AlpacaTokenService` apuntaba al endpoint correcto pero con formato incorrecto. Se corrigió a `AlpacaTokenService` con `MultiValueMap` form-urlencoded. Basic Auth devuelve 401 en sandbox. |
| `audit-service` usaba `spring-boot-starter-data-jpa` + `postgresql` | Reemplazado por `spring-boot-starter-data-elasticsearch` | El servicio nunca tuvo datasource relacional; las dependencias JPA eran un arrastre del template genérico y rompían el inicio. |
| Fondeo de cuenta Alpaca es obligatorio y atómico con el registro | Fondeo **no-fatal**: si falla, se loguea warning y el registro continúa | Las cuentas Alpaca recién creadas quedan en estado `SUBMITTED`, no `ACTIVE`. El journal `JNLC` falla con 422 hasta que la cuenta pasa KYC. Ver deuda técnica #11. |

### Correcciones a specs/contratos

- `spec.md` HU-01 CA-01: fondeo descrito como "intento, puede quedar diferido".
- `spec.md` HU-01 Aclaraciones: agregadas notas sobre edad mínima 18 años,
  nacionalidad como dropdown, email check en Step 1, mercado/sector selección única.
- `HU-010-watchlist/spec.md`: corregido nombre de tabla `identity.watchlist` →
  `identity.watchlist_items` (para alinear con `data-model.md`).

### Cambios técnicos significativos

- **ASCII normalization**: campos `city`, `address`, `firstName`, `lastName`
  se normalizan con `Normalizer.NFD` antes de enviar a Alpaca. Alpaca rechaza
  caracteres no-ASCII (tildes, eñes). Los datos originales se guardan tal cual en BD.
- **Country alpha-2 → alpha-3**: el frontend envía "CO", Alpaca requiere "COL".
  Conversión en `AlpacaBrokerServiceImpl.mapCountryAlpha3()`.
- **`tax_id` / `tax_id_type`**: campos requeridos por Alpaca que faltaban en el
  payload. Mapeados desde `user.docNumber` y `user.docType` (cc → COL_CC).
- **Nuevo endpoint** `GET /api/v1/auth/check-email?email=...`: verifica disponibilidad
  de email antes de llegar al paso 6. Responde `{ "available": boolean }`.
- **JWT_SECRET**: corregido doble `=` en `.env` (`JWT_SECRET==...` → `JWT_SECRET=...`).
- **`ALPACA_FIRM_ACCOUNT_ID`**: faltaba en `docker-compose.yml` como variable de
  entorno para `identity-service`. Valor por defecto "test-firm-id" silenciaba el error.

### Corrección CORS (sesión posterior)

- `SecurityConfig` faltaba `.cors(Customizer.withDefaults())`. Sin esta línea, Spring Security intercepta los preflights OPTIONS del navegador antes de que `CorsConfig` pueda responder con los headers correctos. El `fetch` fallaba silenciosamente y la validación de email en Step 1 nunca mostraba el error.

---

### Cambios de UX (frontend)

- Validación de edad mínima 18 años en Step 2 (`birthDate`, Zod refine).
- Campo `nationality` convertido de texto libre a `<Select>` con gentilicios.
- Mercado y sector cambiados de selección múltiple a selección única.
- Email verificado contra backend en `onBlur` (Step 1), error inmediato si ya existe.
- SMS, WhatsApp y reporte mensual deshabilitados en Step 5 (próximamente).
- Botón "Subir Archivo" eliminado de Step 2 (no conectado a ningún campo del form).

### Archivos .md actualizados en esta sesión

- `STATUS.md`
- `docs/context/technical_debt.md` (ítems #11, #12)
- `docs/context/specs/HU-001-registro/spec.md`
- `docs/context/specs/HU-001-registro/tasks.md` (Bloque 8b, T-F1 a T-F11)
- `docs/context/specs/HU-010-watchlist/spec.md`
- `README.md` (puerto frontend 3000→5173, URL webhook Stripe, bloque de código roto)

---
