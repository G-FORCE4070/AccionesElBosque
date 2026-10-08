# Tasks — HU-13: Encolar órdenes cuando el mercado está cerrado

---

## Bloque A — Contrato y dominio

- [x] **T-A-1** Modificar `docs/context/contracts/trading-service.yaml` — agregar campo `queue` en `PlaceOrderRequest`, nueva respuesta 202 con `QueuedOrderResponse`, agregar `PENDING_QUEUE` al enum de status en `OrderHistoryItem`, nuevo schema `QueuedOrderResponse`
- [x] **T-A-2** Modificar `model/OrderStatus.java` — agregar `PENDING_QUEUE` al enum
- [x] **T-A-3** Modificar `model/Order.java` — agregar campo `@Column(name = "alpaca_account_id") private String alpacaAccountId`
- [x] **T-A-4** Modificar `dto/request/PlaceOrderRequest.java` — agregar `private boolean queue;` (default false, sin validación)
- [x] **T-A-5** Modificar `repository/OrderRepository.java` — agregar `findAllByStatusOrderByCreatedAtAsc(OrderStatus status)`
- [x] **T-A-6** Crear `dto/response/QueuedOrderResponse.java` — record con `orderId`, `symbol`, `side`, `quantity`, `estimatedPrice`, `status`, `queuedAt`, `message`

---

## Bloque B — Infraestructura del scheduler

- [x] **T-B-1** Crear `dto/AlpacaClockDto.java` en trading-service — record `{ @JsonProperty("is_open") boolean open }`
- [x] **T-B-2** Crear `client/AlpacaClockClient.java` — llama `GET https://paper-api.alpaca.markets/v2/clock` con headers `APCA-API-KEY-ID` y `APCA-API-SECRET-KEY`; `testMode=true` si api-key es `"test-key"` (retorna false sin llamar Alpaca); fail-closed con log en `RestClientException`
- [x] **T-B-3** Modificar `src/main/resources/application.yaml` — agregar bloque `alpaca.market-data.api-key` y `alpaca.market-data.secret-key` leyendo de env vars
- [x] **T-B-4** Modificar `src/test/resources/application.yaml` — agregar `scheduling.enabled: false`

---

## Bloque C — Lógica de encolamiento

- [x] **T-C-1** Crear `service/QueuedOrderService.java` — interfaz con `enqueueOrder(PlaceOrderRequest, UUID userId, String alpacaAccountId, String side, String token)` y `processQueue()`
- [x] **T-C-2** Modificar `service/OrderService.java` — agregar firmas `executeQueuedBuyOrder(Order, String alpacaAccountId)` y `executeQueuedSellOrder(Order, String alpacaAccountId)`
- [x] **T-C-3** Modificar `service/impl/OrderServiceImpl.java`:
  - Refactorizar `executeThroughAlpaca` para aceptar `(String accountId, String symbol, int qty, String side, Order order)` en lugar de `PlaceOrderRequest`; actualizar los dos callers existentes (`placeBuyOrder`, `placeSellOrder`)
  - Implementar `executeQueuedBuyOrder`: re-valida saldo con `pendingOrder.getEstimatedPrice()`, marca SENT, ejecuta vía Alpaca, aplica ejecución, finaliza, despacha eventos async
  - Implementar `executeQueuedSellOrder`: re-valida holdings, mismo flujo
- [x] **T-C-4** Crear `service/impl/QueuedOrderServiceImpl.java`:
  - `enqueueOrder`: obtiene precio con `marketsClient.getStockPrice(symbol, token)`, valida saldo/holdings según side, guarda `Order` con `status=PENDING_QUEUE` y `alpacaAccountId`, retorna `QueuedOrderResponse`
  - `processQueue`: llama `alpacaClockClient.isMarketOpen()`; si false retorna; busca `findAllByStatusOrderByCreatedAtAsc(PENDING_QUEUE)`; por cada orden llama `orderService.executeQueued*()`; captura `InsufficientFundsException`/`InsufficientHoldingsException` → marca FAILED + guarda; captura otras excepciones → log + continúa
- [x] **T-C-5** Crear `scheduler/OrderQueueScheduler.java` — `@Component @ConditionalOnProperty(name="scheduling.enabled", havingValue="true", matchIfMissing=true)`; `@Scheduled(fixedDelay=60_000)` llama `queuedOrderService.processQueue()`
- [x] **T-C-6** Modificar `TradingServiceApplication.java` — agregar `@EnableScheduling`

---

## Bloque D — Controller

- [x] **T-D-1** Modificar `controller/OrderController.java` — en `buy()` y `sell()`: envolver la llamada a `orderService.place*()` en try-catch de `MarketClosedException`; si `request.isQueue()` → catch + llamar `queuedOrderService.enqueueOrder(...)` + `return ResponseEntity.accepted().body(result)`; si `!request.isQueue()` → re-throw para que lo capture `GlobalExceptionHandler`

---

## Bloque E — Tests backend

- [x] **T-E-1** Crear `service/impl/QueuedOrderServiceImplTest.java` — 6 tests unitarios con todos los deps como `@Mock`:
  - `enqueue_buy_success` — orden guardada como PENDING_QUEUE, response correcto
  - `enqueue_sell_success` — mismo para sell
  - `enqueue_buy_insufficientFunds` — lanza excepción, no guarda
  - `enqueue_sell_insufficientHoldings` — lanza excepción, no guarda
  - `processQueue_skipsWhenMarketClosed` — `alpacaClockClient` retorna false, `orderService` no invocado
  - `processQueue_executesQueuedOrders` — `alpacaClockClient` retorna true, `orderService.executeQueuedBuyOrder` invocado
- [x] **T-E-2** Crear `controller/OrderControllerQueueTest.java` — 3 tests de integración (Testcontainers + MockMvc):
  - `buy_queueTrue_marketClosed_returns202` — `@MockitoBean AlpacaClockClient` retorna false; POST buy con `queue:true` → 202 + status PENDING_QUEUE
  - `buy_noQueue_marketClosed_returns422` — mercado cerrado, sin queue flag → 422 (regresión)
  - `buy_queueTrue_insufficientFunds_returns400` — mercado cerrado, queue true, saldo insuficiente → 400

---

## Bloque F — Frontend

- [x] **T-F-1** Modificar `features/dashboard/hooks/useOrderDraft.ts` — eliminar tipo `OrderType`, campo `type` de `OrderDraft`, campo `limitPrice` de `OrderDraft`, sus `useState`, la lógica del dispatcher para esos campos, y simplificar `isValid` a solo `quantity > 0`
- [x] **T-F-2** Modificar `features/dashboard/components/NewOrderPanel.tsx` — eliminar `ORDER_TYPES`, el `<Select label="Tipo de orden">`, la variable `showLimitPrice` y el bloque condicional del `<Input label="Precio límite">`
- [x] **T-F-3** Modificar `features/dashboard/services/tradingService.ts` — agregar parámetro opcional `queue?: boolean` a `placeOrder`; incluirlo en el body si `true`; detectar respuesta 202 (encolada) vs 201 (ejecutada) y retornar el tipo correspondiente (`OrderResult` | `QueuedOrderResult`)
- [x] **T-F-4** Modificar `features/dashboard/components/OrderConfirmModal.tsx` — agregar estados `"queued-confirm"` y `"queued-success"` al union de `ModalState`; cuando la primera llamada retorna 422 pasar a `"queued-confirm"` mostrando el mensaje de confirmación con botones "Cancelar" y "Sí, encolar"; al confirmar llamar `placeOrder(..., queue=true)` y si 202 pasar a `"queued-success"` mostrando orderId y mensaje

---

## Verificación final

- [x] **T-V-1** `mvn test` trading-service — todos los tests existentes + 9 nuevos deben pasar
- [x] **T-V-2** Regresión: `OrderServiceImplTest` (8 tests) y `OrderControllerTest` (6 tests) siguen verdes tras refactorización de `executeThroughAlpaca`
- [x] **T-V-3** TypeScript sin errores (`tsc --noEmit`)
- [ ] **T-V-4** UI: panel de nueva orden sin selector de tipo; modal muestra flujo de encolamiento cuando mercado cerrado
- [x] **T-V-5** Actualizar `docs/context/changelog.md` y `STATUS.md`
