# HU-11+12 — Market Orders de compra y venta

## Justificación de la combinación

trading-service está vacío — toda la infraestructura del servicio (seguridad JWT,
cliente OAuth2 para Alpaca Broker, entidades JPA, repositorios, clientes async
para audit y notification) se construye una sola vez para las dos operaciones.
Separarlas implicaría duplicar ese setup sin ningún beneficio. Además, comparten
las tres tablas del schema: `trading.accounts`, `trading.holdings`, `trading.orders`,
y el panel del frontend (`NewOrderPanel`) maneja compra y venta en la misma pantalla.

---

## Historias de usuario

**Como** inversionista,
**quiero** comprar acciones al precio actual del mercado,
**para** adquirir participaciones en empresas de mi interés.

**Como** inversionista,
**quiero** vender acciones que poseo al precio actual del mercado,
**para** obtener liquidez o realizar ganancias.

---

## Prioridad y estimación

| Campo      | Valor                             |
|------------|-----------------------------------|
| Prioridad  | Must                              |
| Estimación | 10 puntos (HU-11: 5 + HU-12: 5)  |
| Épica      | Trading                           |
| Feature    | Market Orders                     |
| Servicios  | trading-service (nuevo), markets-service (extensión), frontend |

---

## Referencias

- `docs/context/architecture.md` — ADR-08 (saldo local), ADR-12 (esperar confirmación Alpaca), ADR-05 (@Async)
- `docs/context/domain.md` — estados de orden, comisión 2%, avgCost
- `docs/context/decisions.md` — ADR-05, ADR-08, ADR-12
- `docs/context/contracts/trading-service.yaml` — PlaceOrderRequest, OrderResponse, PortfolioResponse
- `docs/context/contracts/markets-service.yaml` — StockResponse, MarketStatusResponse
- `docs/context/contracts/notification-service.yaml` — NotificationRequest
- `docs/context/contracts/audit-service.yaml` — AuditEventRequest

---

## Prerequisito — markets-service

El endpoint `GET /markets/status/{symbol}` está en el contrato pero no existe
en el controlador actual. La tabla `markets.market_schedules` ya tiene los datos.
Se agrega como primera tarea de esta HU.

---

## Criterios de aceptación — Compra (HU-11)

### CA-11-01: Compra exitosa

**Dado que** el inversionista tiene saldo suficiente, el mercado está abierto y el símbolo es válido
**Cuando** llama a `POST /api/v1/orders/buy`
**Entonces**:
1. trading-service verifica mercado abierto vía `GET /markets/status/{symbol}`
2. Obtiene precio estimado vía `GET /markets/stocks/{symbol}`
3. Calcula costo estimado: `estimatedPrice * quantity * 1.02`
4. Verifica saldo local suficiente contra `trading.accounts`
5. Envía la orden a Alpaca Broker API y espera confirmación (ADR-12, polling 5 × 1 s)
6. Calcula comisión: `executedPrice * quantity * 0.02`
7. Descuenta `executedPrice * quantity + commission` del saldo
8. Upsert en `trading.holdings` con precio promedio ponderado
9. Guarda la orden en `trading.orders` con status EXECUTED
**Y** responde HTTP 201 — ver `trading-service.yaml → OrderResponse`
**Y** registra ORDER_PLACED y ORDER_EXECUTED vía `@Async`
**Y** notifica a notification-service vía `@Async`

### CA-11-02: Fondos insuficientes

**Dado que** `estimatedPrice * quantity * 1.02 > balance`
**Cuando** llama a `POST /api/v1/orders/buy`
**Entonces** responde HTTP 400 — ver `trading-service.yaml → ErrorResponse (insufficientFunds)`
**Y** no se envía ninguna orden a Alpaca ni se modifica el saldo

### CA-11-03: Mercado cerrado

**Dado que** el mercado para el símbolo no está abierto
**Cuando** llama a `POST /api/v1/orders/buy`
**Entonces** responde HTTP 422 — ver `trading-service.yaml → ErrorResponse (marketClosed)`

### CA-11-04: Orden rechazada o fallida por Alpaca

**Dado que** Alpaca devuelve `rejected`/`canceled` o se agotan los 5 intentos de polling sin `filled`
**Cuando** el sistema procesa la respuesta
**Entonces** guarda la orden con status REJECTED o FAILED respectivamente
**Y** no modifica saldo ni holdings
**Y** responde HTTP 201 — ver `trading-service.yaml → OrderResponse`
**Y** registra ORDER_REJECTED vía `@Async`

### CA-11-05: Notificación de compra

**Dado que** la orden fue ejecutada (CA-11-01)
**Cuando** notification-service recibe la llamada asíncrona
**Entonces** envía email (si `orderConfirm` habilitado) con símbolo, tipo COMPRA,
cantidad, precio ejecutado, comisión, nuevo saldo
— ver `notification-service.yaml → NotificationRequest`

---

## Criterios de aceptación — Venta (HU-12)

### CA-12-01: Venta exitosa

**Dado que** el inversionista posee suficientes acciones y el mercado está abierto
**Cuando** llama a `POST /api/v1/orders/sell`
**Entonces**:
1. Verifica mercado abierto y obtiene precio estimado (igual que compra)
2. Verifica `trading.holdings.quantity >= quantity` para ese símbolo y usuario
3. Envía orden a Alpaca (`side: sell`) y espera confirmación (polling 5 × 1 s)
4. Calcula comisión: `executedPrice * quantity * 0.02`
5. Suma `executedPrice * quantity - commission` al saldo
6. Reduce `Holding.quantity`; si llega a 0 elimina la fila
7. Guarda la orden con status EXECUTED
**Y** responde HTTP 201 — ver `trading-service.yaml → OrderResponse`
**Y** registra y notifica vía `@Async`

### CA-12-02: Cantidad insuficiente

**Dado que** `Holding.quantity < quantity` o el símbolo no está en holdings
**Cuando** llama a `POST /api/v1/orders/sell`
**Entonces** responde HTTP 400 — ver `trading-service.yaml → ErrorResponse (insufficientHoldings)`
**Y** no se envía ninguna orden a Alpaca

### CA-12-03: Mercado cerrado

Igual que CA-11-03

### CA-12-04: Orden rechazada o fallida por Alpaca

Igual que CA-11-04

### CA-12-05: Notificación de venta

Igual que CA-11-05 pero tipo VENTA

---

## Casos transversales

### CA-X-01: Servicio de precios no disponible

**Si** markets-service no responde al consultar estado o precio
**Entonces** responde HTTP 503 — ver `trading-service.yaml → ErrorResponse`

### CA-X-02: JWT requerido

**Si** no se envía token o es inválido
**Entonces** responde HTTP 401 — ver `trading-service.yaml → Unauthorized`

---

## Definition of Done — Backend

### markets-service
- [ ] `GET /markets/status/{symbol}` — lee `market_schedules`, verifica hora UTC y lunes–viernes
- [ ] Tests: mercado abierto, cerrado, símbolo no encontrado

### trading-service
- [ ] `POST /api/v1/orders/buy` y `POST /api/v1/orders/sell` con JWT requerido
- [ ] `PlaceOrderRequest` validado: `symbol` not blank, `quantity` min 1, `orderType` = MARKET
- [ ] Validación de mercado abierto (síncrono a markets-service)
- [ ] Precio estimado desde markets-service
- [ ] Validación de saldo (compra) y holdings (venta) contra BD local
- [ ] Envío a Alpaca Broker API con `alpacaAccountId` del JWT
- [ ] Polling 5 × 1 s → EXECUTED / REJECTED / FAILED
- [ ] `Account.balance` actualizado tras ejecución exitosa
- [ ] `Holding` creado/actualizado (compra) o reducido/eliminado (venta)
- [ ] `Order` guardada con todos los campos del contrato
- [ ] `commission = executedPrice * quantity * 0.02`
- [ ] `avgCost` con precio promedio ponderado en compras
- [ ] `GET /api/v1/portfolio` retorna balance + holdings + precios actuales
- [ ] ORDER_PLACED, ORDER_EXECUTED / ORDER_REJECTED vía `@Async @Retryable`
- [ ] notification-service notificado vía `@Async @Retryable`
- [ ] 8 tests unitarios en `OrderServiceImplTest`
- [ ] 6 tests de integración en `OrderControllerTest` (MockMvc + Testcontainers)
- [ ] `mvn test` pasa sin errores

## Definition of Done — Frontend

- [ ] `BalancePill` muestra saldo real desde trading-service
- [ ] `NewOrderPanel` usa stocks y precios reales desde markets-service
- [ ] `OrderConfirmModal` llama API real, muestra orderId real, maneja errores
- [ ] `OrderSummary` muestra solo Subtotal + Comisión (2%) + Total
- [ ] Balance actualizado en pantalla tras orden ejecutada

---

## Notas técnicas

- La validación de fondos usa `estimatedPrice` como cota superior (no `executedPrice`).
- `totalAmount` en la respuesta = `executedPrice * quantity` sin comisión.
- Polling: `rejected` o `canceled` de Alpaca → REJECTED; 5 intentos sin `filled` → FAILED.
- `@EnableRetry` y `@EnableAsync` en `TradingServiceApplication.java`.
- `alpacaAccountId` se extrae del claim JWT.
- Si no existe fila en `trading.accounts` → error claro, no se autocrea.
- Comisión 100% a ElBosque en MVP (deuda técnica #7 — split broker/plataforma).
