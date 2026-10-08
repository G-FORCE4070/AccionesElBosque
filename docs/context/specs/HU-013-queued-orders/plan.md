# Plan — HU-13: Encolar órdenes cuando el mercado está cerrado

---

## Resumen de la estrategia

Cuando el usuario envía una orden con `queue: true` y el mercado está cerrado,
el sistema la almacena como `PENDING_QUEUE` en la tabla `trading.orders` ya existente.
Un `@Scheduled` en trading-service revisa cada 60 segundos si el mercado abrió
(llamando directamente al endpoint de Alpaca clock) y ejecuta las órdenes pendientes
en orden FIFO usando el mismo flujo de polling que ya existe en `OrderServiceImpl`.

**No se requieren cambios en ningún otro servicio.** Todo queda en trading-service y frontend.

---

## Decisiones clave de diseño

| Decisión | Opción elegida | Razón |
|---|---|---|
| Cómo verificar el mercado en el scheduler | `AlpacaClockClient` en trading-service (llama `paper-api.alpaca.markets/v2/clock`) | Evita inter-service auth; las credenciales market-data ya existen en `.env` |
| Precio al ejecutar la orden encolada | Usar `estimatedPrice` almacenado; Alpaca devuelve el precio real de fill | No necesita token en el scheduler; el precio final siempre viene de `filled_avg_price` |
| Cómo reutilizar lógica de `OrderServiceImpl` | Agregar `executeQueuedBuyOrder(Order, alpacaAccountId)` al interface `OrderService` | Los métodos privados de `OrderServiceImpl` son accesibles internamente; evita duplicación |
| Deshabilitar scheduler en tests | `@ConditionalOnProperty(scheduling.enabled, default true)` + `false` en test yaml | Permite testear `processQueue()` directamente sin que el scheduler interfiera |
| Reserva de saldo al encolar | Sin reserva; se re-valida con `estimatedPrice` al ejecutar | Simplicidad MVP; si fallan fondos al ejecutar → orden FAILED + notificación |

---

## Archivos a crear — Backend (trading-service)

### 1. `dto/response/QueuedOrderResponse.java`
Record Java con: `orderId (String)`, `symbol`, `side`, `quantity (int)`,
`estimatedPrice (BigDecimal)`, `status (String = "PENDING_QUEUE")`,
`queuedAt (Instant)`, `message (String)`.

### 2. `client/AlpacaClockClient.java`
`@Component`. Constructor recibe `@Value("${alpaca.market-data.api-key}")` y
`@Value("${alpaca.market-data.secret-key}")`.
Método: `boolean isMarketOpen()` — llama `GET https://paper-api.alpaca.markets/v2/clock`,
deserializa `AlpacaClockDto` (mismo DTO de markets-service, pero aquí en trading-service
se crea uno nuevo en el paquete `dto`), `testMode` si api-key es `"test-key"` → devuelve `false`.
Manejo de `RestClientException`: log + return `false` (fail-closed).

### 3. `service/QueuedOrderService.java`
Interfaz con dos métodos:
```java
QueuedOrderResponse enqueueOrder(PlaceOrderRequest request,
    UUID userId, String alpacaAccountId, String side, String token);
void processQueue();
```

### 4. `service/impl/QueuedOrderServiceImpl.java`
`@Service @RequiredArgsConstructor`.
Dependencias: `AccountRepository`, `HoldingRepository`, `OrderRepository`,
`MarketsServiceClient`, `AlpacaClockClient`, `OrderService` (para ejecutar), `@Value commission.percentage`.

**`enqueueOrder`** (`@Transactional`):
1. Llama `marketsClient.getStockPrice(symbol, token)` → `estimatedPrice`.
2. Para `side=buy`: valida `account.balance ≥ estimatedPrice × qty + commission`. Lanza `InsufficientFundsException` si no.
3. Para `side=sell`: valida que el holding exista y tenga suficientes acciones. Lanza `InsufficientHoldingsException` si no.
4. Crea `Order` con `status=PENDING_QUEUE`, `alpacaAccountId` almacenado.
5. Guarda con `orderRepository.save()`.
6. Retorna `QueuedOrderResponse`.

**`processQueue`**:
1. Llama `alpacaClockClient.isMarketOpen()`. Si false → retorna (mercado aún cerrado).
2. `orderRepository.findAllByStatusOrderByCreatedAtAsc(PENDING_QUEUE)`.
3. Para cada orden:
   - Si `side=buy` → `orderService.executeQueuedBuyOrder(order, order.getAlpacaAccountId())`.
   - Si `side=sell` → `orderService.executeQueuedSellOrder(order, order.getAlpacaAccountId())`.
   - Si lanza `InsufficientFundsException` o `InsufficientHoldingsException`:
     marca orden como `FAILED` + guarda + log (la notificación async la maneja `OrderServiceImpl`).
   - Si lanza cualquier otra excepción: log de error + continúa con la siguiente orden.

### 5. `scheduler/OrderQueueScheduler.java`
```java
@Component
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderQueueScheduler {
    @Scheduled(fixedDelay = 60_000)
    public void tick() { queuedOrderService.processQueue(); }
}
```

---

## Archivos a modificar — Backend (trading-service)

### 6. `model/OrderStatus.java`
Agregar `PENDING_QUEUE` al enum: `PENDING_QUEUE, SENT, EXECUTED, REJECTED, FAILED`.

### 7. `model/Order.java`
Agregar campo `@Column(name = "alpaca_account_id") private String alpacaAccountId`.
Necesario para que el scheduler pueda ejecutar la orden sin JWT.

### 8. `dto/request/PlaceOrderRequest.java`
Agregar `private boolean queue;` (default `false`).
No lleva `@NotNull` ni `@NotBlank` — el campo es opcional y hace default a false.

### 9. `repository/OrderRepository.java`
Agregar:
```java
List<Order> findAllByStatusOrderByCreatedAtAsc(OrderStatus status);
```

### 10. `service/OrderService.java`
Agregar dos nuevos métodos al interface:
```java
OrderResponse executeQueuedBuyOrder(Order pendingOrder, String alpacaAccountId);
OrderResponse executeQueuedSellOrder(Order pendingOrder, String alpacaAccountId);
```

### 11. `service/impl/OrderServiceImpl.java`
Implementar los dos métodos nuevos. Flujo para `executeQueuedBuyOrder`:
1. `getOrCreateAccount(pending.getUserId())`.
2. Calcular `estimatedTotal = pending.getEstimatedPrice() × qty + commission`.
3. Si `account.balance < estimatedTotal` → lanza `InsufficientFundsException`.
4. Marcar `pending.setStatus(SENT)` + `orderRepository.save(pending)`.
5. `executeThroughAlpaca(alpacaAccountId, pending.getSymbol(), pending.getQuantity(), pending.getSide(), pending)`.
   _(Se refactoriza la firma privada para aceptar `symbol, qty, side` en lugar de `PlaceOrderRequest`)_
6. Resolver precio real, `applyBuyExecution(...)`, `finalizeOrder(...)`, `dispatchAsyncEvents(...)`.
7. Retornar `buildOrderResponse(...)`.

Análogamente para `executeQueuedSellOrder`.

**Refactorización menor de `executeThroughAlpaca`**: cambiar la firma de:
```java
private AlpacaOrderResponse executeThroughAlpaca(String accountId, PlaceOrderRequest req, String side, Order order)
```
a:
```java
private AlpacaOrderResponse executeThroughAlpaca(String accountId, String symbol, int qty, String side, Order order)
```
para que sea usable tanto desde `placeBuyOrder`/`placeSellOrder` como desde los nuevos métodos.
Actualizar los dos callers existentes.

### 12. `controller/OrderController.java`
En `buy()` y `sell()`:
```
si request.isQueue() y mercado cerrado → enqueueOrder → return ResponseEntity.accepted() (202)
si request.isQueue() y mercado abierto → ejecutar normalmente (ignorar el flag)
si !request.isQueue() → comportamiento actual (422 si mercado cerrado)
```

Para conocer el estado del mercado en el controller sin duplicar lógica:
- Inyectar `QueuedOrderService` en `OrderController`.
- Intentar `orderService.place*(...)` → si lanza `MarketClosedException` y `request.isQueue()` → catch + llamar `queuedOrderService.enqueueOrder(...)` + return 202.
- Si lanza `MarketClosedException` y `!request.isQueue()` → re-throw (lo captura `GlobalExceptionHandler`).

Esto evita una doble llamada al clock y mantiene el flujo simple.

### 13. `TradingServiceApplication.java`
Agregar `@EnableScheduling` junto a `@EnableAsync @EnableRetry`.

### 14. `src/main/resources/application.yaml`
Agregar bloque:
```yaml
alpaca:
  market-data:
    api-key: ${ALPACA_MARKET_DATA_API_KEY:test-key}
    secret-key: ${ALPACA_MARKET_DATA_SECRET_KEY:test-secret}
```

### 15. `src/test/resources/application.yaml`
Agregar:
```yaml
scheduling:
  enabled: false
```

---

## Archivos a modificar — Contrato

### 16. `docs/context/contracts/trading-service.yaml`
- `PlaceOrderRequest`: agregar campo `queue` (boolean, default false).
- Respuesta `202` en `/orders/buy` y `/orders/sell`: nuevo schema `QueuedOrderResponse`.
- `OrderHistoryItem.status` enum: agregar `PENDING_QUEUE`.
- Nuevo schema `QueuedOrderResponse`.

---

## Tests a escribir

### 17. `service/impl/QueuedOrderServiceImplTest.java` — 6 tests unitarios
Todos los repos y clients mockeados con `@Mock`.

| Test | Qué verifica |
|---|---|
| `enqueue_buy_success` | Orden guardada como PENDING_QUEUE, retorna 202 con estimatedPrice |
| `enqueue_sell_success` | Orden guardada como PENDING_QUEUE para side=sell |
| `enqueue_buy_insufficientFunds` | Lanza `InsufficientFundsException`, no guarda |
| `enqueue_sell_insufficientHoldings` | Lanza `InsufficientHoldingsException`, no guarda |
| `processQueue_skipsWhenMarketClosed` | `alpacaClockClient.isMarketOpen()=false` → `orderService` no es llamado |
| `processQueue_executesAndMarksExecuted` | `isMarketOpen=true` → llama `orderService.executeQueuedBuyOrder(...)` |

### 18. `controller/OrderControllerQueueTest.java` — 3 tests de integración
Testcontainers + MockMvc. `@MockitoBean MarketsServiceClient` y `@MockitoBean AlpacaBrokerClient`.

| Test | Endpoint | Escenario |
|---|---|---|
| `buy_queueTrue_marketClosed_returns202` | `POST /orders/buy` body con `queue:true` + `@MockitoBean AlpacaClockClient` devuelve false | Recibe 202 + status PENDING_QUEUE |
| `buy_noQueue_marketClosed_returns422` | `POST /orders/buy` sin queue flag + mercado cerrado | Recibe 422 (regresión) |
| `buy_queueTrue_insufficientFunds_returns400` | `POST /orders/buy` con `queue:true`, mercado cerrado, saldo insuficiente | Recibe 400 (validación previa al encolamiento) |

---

## Archivos a modificar — Frontend

### 19. `features/dashboard/hooks/useOrderDraft.ts`
Eliminar todo lo relacionado a tipos de orden distintos de `market`:
- Eliminar el tipo `OrderType` (o simplificarlo a `type OrderType = "market"`).
- Eliminar el campo `type: OrderType` de `OrderDraft`.
- Eliminar el campo `limitPrice?: number` de `OrderDraft`.
- Eliminar los `useState` de `type` y `limitPrice`.
- Eliminar el `set("type", ...)` y `set("limitPrice", ...)` del dispatcher.
- Simplificar `isValid` a solo `quantity > 0` (eliminada la guarda de limitPrice).
- Eliminar `setType("market")` y `setLimitPrice(undefined)` del `useEffect` de reset (ya no hay nada que resetear excepto `quantity`).

### 20. `features/dashboard/components/NewOrderPanel.tsx`
- Eliminar la constante `ORDER_TYPES`.
- Eliminar el `<Select label="Tipo de orden" ...>` completo.
- Eliminar la variable `showLimitPrice` y el bloque `{showLimitPrice && <Input label="Precio límite" ...>}`.
- La orden siempre es de tipo market — no hace falta mostrarlo al usuario.

### 21. `features/dashboard/services/tradingService.ts`
`placeOrder(side, symbol, quantity, token, queue?: boolean)`:
si `queue=true` → agregar `queue: true` al body JSON.
El tipo de retorno cambia a `OrderResult | QueuedOrderResult`:
```typescript
export type QueuedOrderResult = {
  orderId: string;
  status: "PENDING_QUEUE";
  estimatedPrice: number;
  message: string;
};
```
Detectar 202 vs 201 en la respuesta para distinguir ejecución vs encolamiento.

### 20. `features/dashboard/components/OrderConfirmModal.tsx`
Agregar dos estados nuevos al union `ModalState`:
```typescript
type ModalState = "idle" | "loading" | "success" | "error" | "queued-confirm" | "queued-success";
```

**Flujo**:
1. `"loading"`: llama `placeOrder(..., queue=false)`.
2. Si 422 → pasa a `"queued-confirm"`: muestra mensaje "El mercado está cerrado. ¿Deseas encolar la orden?"
3. Botón "Sí, encolar" → llama `placeOrder(..., queue=true)` → si 202 → pasa a `"queued-success"`.
4. `"queued-success"`: muestra orderId + "Se ejecutará automáticamente cuando el mercado abra."

---

## Orden de implementación

```
1.  trading-service.yaml (contrato)
2.  OrderStatus.PENDING_QUEUE
3.  Order.alpacaAccountId
4.  PlaceOrderRequest.queue
5.  OrderRepository.findAllByStatusOrderByCreatedAtAsc
6.  QueuedOrderResponse DTO
7.  AlpacaClockClient (+ dto/AlpacaClockDto en trading-service)
8.  application.yaml (main: alpaca.market-data + test: scheduling.enabled=false)
9.  QueuedOrderService interface
10. OrderService: nuevas firmas executeQueuedBuy/SellOrder
11. OrderServiceImpl: refactorizar executeThroughAlpaca + implementar nuevos métodos
12. QueuedOrderServiceImpl
13. OrderQueueScheduler + @EnableScheduling
14. OrderController (lógica de catch + queue)
15. Tests backend (QueuedOrderServiceImplTest + OrderControllerQueueTest)
16. useOrderDraft.ts — eliminar type, limitPrice y toda la lógica de órdenes no-market
17. NewOrderPanel.tsx — eliminar selector de tipo de orden y campo de precio límite
18. tradingService.ts (frontend)
19. OrderConfirmModal.tsx (frontend)
```

---

## Tests de regresión a verificar

- `OrderServiceImplTest` — los 8 tests existentes no deben romperse por la refactorización de `executeThroughAlpaca`.
- `OrderControllerTest` — los 6 tests existentes de buy/sell normal deben seguir pasando.
- `mvn test` en su totalidad.
