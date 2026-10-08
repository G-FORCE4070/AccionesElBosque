# HU-13 — Encolar órdenes cuando el mercado está cerrado

## Historia de usuario

**Como** inversionista,
**quiero** poder encolar una orden de compra o venta cuando el mercado está cerrado,
**para** que se ejecute automáticamente en cuanto el mercado abra, sin que yo tenga que estar pendiente.

---

## Prioridad y estimación

| Campo      | Valor                                    |
|------------|------------------------------------------|
| Prioridad  | Should                                   |
| Estimación | 5 puntos                                 |
| Épica      | Trading                                  |
| Feature    | Market Orders                            |
| Servicio   | trading-service (backend), frontend      |

---

## Referencias

- `docs/context/specs/HU-011-012-market-orders/spec.md` — flujo base de órdenes (compra/venta)
- `docs/context/contracts/trading-service.yaml` — contrato actual (se extiende en esta HU)
- `docs/context/decisions.md` — ADR-02 (schema `trading`), ADR-04 (JWT claims)
- `backend/trading-service/src/main/java/.../service/impl/OrderServiceImpl.java` — flujo a reutilizar en el scheduler
- `backend/trading-service/src/main/java/.../service/AlpacaTokenService.java` — OAuth2 Alpaca
- `backend/markets-service/src/main/java/.../service/AlpacaClockService.java` — fuente de verdad del estado del mercado

---

## Contexto técnico — Alpaca Broker API

El scheduler envía la orden a Alpaca cuando detecta que el mercado está abierto.
Usa exactamente el mismo payload que una orden normal:

```json
{
  "symbol": "AAPL",
  "qty": "5",
  "side": "buy",
  "type": "market",
  "time_in_force": "day"
}
```

`time_in_force: "day"` es correcto: la orden se somete durante la sesión regular.
No se usa `gtc` porque cada orden encolada representa la intención del usuario
para la próxima apertura de mercado, no una orden abierta indefinidamente.

El precio de ejecución real lo devuelve Alpaca (`filled_avg_price`) tras el polling —
el mismo mecanismo de HU-011-012.

---

## Flujo completo

```
[Usuario]                 [Frontend]              [trading-service]       [Alpaca]
   |                          |                          |                    |
   |-- click "Comprar" -----> |                          |                    |
   |                          |-- POST /orders/buy ----> |                    |
   |                          |                          |-- isMarketOpen? -->|
   |                          |                          |<-- false ----------|
   |                          |<-- 422 MarketClosed ---- |                    |
   |                          |                          |                    |
   |<- modal: "¿Encolar?" --- |                          |                    |
   |-- click "Sí, encolar" -> |                          |                    |
   |                          |-- POST /orders/buy ----> |                    |
   |                          |   ?queue=true            |-- validateFunds    |
   |                          |                          |-- fetchLastPrice   |
   |                          |                          |-- save PENDING_Q   |
   |                          |<-- 202 Aceptado -------- |                    |
   |<- "Orden encolada ✓" --- |                          |                    |
   |                          |                          |                    |
   |              [-- mercado abre --]                   |                    |
   |                          |                          |-- @Scheduled ----  |
   |                          |                          |-- revalidate       |
   |                          |                          |-- POST order ----> |
   |                          |                          |<-- accepted        |
   |                          |                          |-- poll filled --->  |
   |                          |                          |<-- EXECUTED        |
   |                          |                          |-- update balance   |
   |                          |                          |-- notify async --> |
```

---

## Criterios de aceptación

### CA-01: Encolar orden de compra — mercado cerrado

**Dado que** el mercado está cerrado
**Y** el usuario envía `POST /api/v1/orders/buy` con `queue: true` en el body
**Y** el usuario tiene fondos suficientes para el precio estimado (último precio conocido)
**Entonces** el sistema almacena la orden con status `PENDING_QUEUE`
**Y** retorna HTTP 202 con:
- `orderId` de la orden creada
- `status: "PENDING_QUEUE"`
- `estimatedPrice` (último precio disponible de markets-service)
- `message: "Tu orden quedó encolada y se ejecutará cuando el mercado abra."`

---

### CA-02: Encolar orden de venta — mercado cerrado

**Dado que** el mercado está cerrado
**Y** el usuario envía `POST /api/v1/orders/sell` con `queue: true`
**Y** el usuario tiene suficientes acciones del símbolo
**Entonces** el sistema almacena la orden con status `PENDING_QUEUE`
**Y** retorna HTTP 202 con los mismos campos que CA-01

---

### CA-03: Validación antes de encolar — fondos insuficientes

**Dado que** el mercado está cerrado
**Y** el usuario envía `POST /api/v1/orders/buy?queue=true`
**Y** el saldo del usuario es menor que `estimatedPrice × quantity`
**Entonces** el sistema retorna HTTP 400
**Y** no crea ninguna orden en la base de datos
**Y** el mensaje de error es `"Fondos insuficientes"`

---

### CA-04: Validación antes de encolar — acciones insuficientes

**Dado que** el mercado está cerrado
**Y** el usuario envía `POST /api/v1/orders/sell?queue=true`
**Y** el usuario no tiene la cantidad de acciones solicitada
**Entonces** el sistema retorna HTTP 400
**Y** no crea ninguna orden
**Y** el mensaje de error es `"Cantidad insuficiente de acciones"`

---

### CA-05: Sin confirmación explícita — rechaza igual

**Dado que** el mercado está cerrado
**Y** el usuario envía `POST /api/v1/orders/buy` sin `queue: true`
**Entonces** el sistema retorna HTTP 422 con `"El mercado está cerrado"`
_(comportamiento idéntico al de HU-011-012, sin cambios)_

---

### CA-06: Ejecución automática al abrir el mercado

**Dado que** hay órdenes con status `PENDING_QUEUE`
**Cuando** el scheduler detecta que el mercado abrió (cada 60 s)
**Entonces** por cada orden `PENDING_QUEUE` en orden FIFO:
  1. Re-valida saldo/holdings con el precio actual del mercado
  2. Si la validación falla: marca la orden como `FAILED`, emite evento de auditoría y notificación
  3. Si la validación pasa: ejecuta la orden a través de Alpaca (mismo flujo de polling de HU-012), actualiza saldo/holdings, emite eventos asíncronos

---

### CA-07: Re-validación falla al ejecutar

**Dado que** el usuario encoló una compra de AAPL por $1,000
**Y** antes de la apertura gastó fondos en otra orden dejando saldo insuficiente
**Cuando** el scheduler intenta ejecutar la orden encolada
**Entonces** la orden pasa a `FAILED`
**Y** el saldo del usuario no se modifica
**Y** se envía una notificación al usuario informando el fallo

---

### CA-08: Comportamiento del frontend — flujo de dos pasos

**Paso 1 — intento normal:**
- El usuario abre el modal de confirmación y hace click en "Confirmar"
- El frontend llama a `POST /orders/buy` (sin `queue: true`)
- Si recibe 422 → el modal cambia a estado `"queued-confirm"`:
  - Muestra: `"El mercado está cerrado actualmente. ¿Deseas encolar esta orden para que se ejecute cuando el mercado abra?"`
  - Botón secundario: `"Cancelar"`
  - Botón primario: `"Sí, encolar"`

**Paso 2 — confirmación explícita:**
- El usuario hace click en `"Sí, encolar"`
- El frontend llama a `POST /orders/buy` con `queue: true` en el body
- Si recibe 202 → el modal cambia a estado `"queued-success"`:
  - Muestra: `"Orden encolada ✓. Se ejecutará automáticamente cuando el mercado abra."`
  - Muestra el `orderId`
  - Botón: `"Cerrar"`
- Si recibe 4xx → muestra el error normalmente (balance insuficiente, etc.)

---

### CA-09: Visibilidad en historial

**Dado que** el usuario encoló una orden
**Cuando** consulta `GET /api/v1/orders`
**Entonces** la orden aparece con `status: "PENDING_QUEUE"`
_(no requiere cambios adicionales; el endpoint de historial ya devuelve todos los estados)_

---

## Definition of Done

- [ ] `OrderStatus.java` tiene el valor `PENDING_QUEUE`
- [ ] `PlaceOrderRequest.java` acepta campo `queue: boolean` (default `false`)
- [ ] `OrderController.java` — cuando `queue=true` y mercado cerrado → delega a `QueuedOrderService`
- [ ] `QueuedOrderService.java` + `QueuedOrderServiceImpl.java`:
  - `enqueueOrder(PlaceOrderRequest, userId, alpacaAccountId, side)` → guarda PENDING_QUEUE, devuelve `QueuedOrderResponse`
  - `processQueue()` → método llamado por el scheduler
- [ ] `scheduler/OrderQueueScheduler.java` — `@Scheduled(fixedDelay=60_000)`, llama `processQueue()`
- [ ] `TradingServiceApplication.java` — agrega `@EnableScheduling`
- [ ] `dto/response/QueuedOrderResponse.java` — `orderId`, `status`, `estimatedPrice`, `message`
- [ ] Endpoint contract actualizado en `trading-service.yaml` (nuevo campo `queue` en `PlaceOrderRequest`, nueva respuesta 202)
- [ ] Tests unitarios `QueuedOrderServiceImplTest` cubriendo:
  - `enqueue_buy_success`
  - `enqueue_sell_success`
  - `enqueue_buy_insufficientFunds`
  - `enqueue_sell_insufficientHoldings`
  - `processQueue_executesSuccessfully`
  - `processQueue_failsOnInsufficientFunds`
- [ ] Tests de integración `OrderControllerQueueTest` cubriendo:
  - `POST /orders/buy?queue=true` → 202 cuando mercado cerrado
  - `POST /orders/buy` (sin queue) → 422 cuando mercado cerrado (regresión)
  - `POST /orders/buy?queue=true` → 400 fondos insuficientes (regresión validación)
- [ ] Frontend: `OrderConfirmModal.tsx` — nuevo estado `"queued-confirm"` y `"queued-success"`
- [ ] Frontend: `tradingService.ts` — `placeOrder` acepta `queue?: boolean`
- [ ] `mvn test` pasa en trading-service (todos los tests existentes + nuevos)
- [ ] TypeScript sin errores en frontend

---

## Notas técnicas

### Precio al momento de encolar
`MarketsServiceClient.getStockPrice()` devuelve el último precio conocido aunque el mercado esté cerrado
(Alpaca Market Data API retorna la última cotización disponible siempre).
Este precio es solo orientativo: la orden se ejecuta al precio de mercado real al momento de la apertura.

### Sin reserva de balance
El sistema **no reserva** el saldo al encolar.
Si el usuario gasta fondos antes de que el mercado abra, la re-validación en el scheduler fallará
y la orden pasará a `FAILED`. El usuario recibe notificación del fallo.
Esta simplificación es intencional para el MVP.

### Scheduler y test mode
En los tests de integración, el scheduler no debe dispararse automáticamente.
Se puede deshabilitar con `@TestPropertySource` o `spring.task.scheduling.enabled=false`
en el `test/resources/application.yaml` y llamar `processQueue()` directamente en los tests.

### Idempotencia del scheduler
El scheduler filtra `findAllByStatus(PENDING_QUEUE)`.
Una vez que una orden encolada pasa a `SENT` (fue enviada a Alpaca), desaparece del filtro.
No hay riesgo de doble envío.

### Sin cancelación en esta HU
La cancelación de órdenes encoladas queda fuera del alcance de HU-013.
El usuario solo puede ver las órdenes encoladas en el historial.
