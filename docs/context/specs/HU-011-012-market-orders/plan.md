# Plan — HU-11+12: Market Orders de compra y venta

---

## Orden general de implementación

```
0. markets-service  →  prerequisito (endpoint de status)
A. trading-service infraestructura  (pom, yaml, security, Alpaca client)
B. trading-service dominio           (entities, repos, DTOs, exceptions)
C. trading-service lógica            (clients, service, controllers)
D. trading-service tests
E. frontend
```

---

## 0. markets-service — extensión (prerequisito)

### Archivos a crear

| Archivo | Descripción |
|---|---|
| `model/MarketSchedule.java` | Entidad JPA → `markets.market_schedules` |
| `repository/MarketScheduleRepository.java` | `findByMarketCodeAndActiveTrue(String)` |
| `dto/response/MarketStatusResponse.java` | `{ symbol, market, open, opensAt, closesAt }` |

### Archivos a modificar

| Archivo | Cambio |
|---|---|
| `service/StockService.java` | Agregar `getMarketStatus(String symbol)` |
| `service/impl/StockServiceImpl.java` | Implementar: stock → marketCode → schedule → comparar `LocalTime.now(UTC)` + día de semana lun-vie |
| `controller/StockController.java` | Agregar `GET /markets/status/{symbol}` |
| `test/.../controller/StockControllerTest.java` | Agregar: mercado abierto, cerrado, símbolo no encontrado |

### Tests nuevos en markets-service

| Test | Qué valida |
|---|---|
| `getMarketStatus_open` | Hora dentro del rango y día hábil → `open: true` |
| `getMarketStatus_closed_time` | Hora fuera de rango → `open: false` |
| `getMarketStatus_closed_weekend` | Día no hábil → `open: false` |
| `getMarketStatus_symbolNotFound` | Símbolo inexistente → 404 |

---

## A. trading-service — infraestructura base

### A1. pom.xml — agregar dependencias

Sobre el pom existente (web, jpa, validation, postgresql, lombok, test):

| Dependencia | Scope |
|---|---|
| `spring-boot-starter-security` | compile |
| `jjwt-api 0.12.x` | compile |
| `jjwt-impl 0.12.x` | runtime |
| `jjwt-jackson 0.12.x` | runtime |
| `spring-retry` | compile |
| `spring-boot-starter-aop` | compile |
| `org.testcontainers:postgresql` | test |

### A2. application.yaml

```yaml
server.port: 8082
spring:
  jpa:
    hibernate.ddl-auto: update
    properties.hibernate.default_schema: trading
alpaca.broker:
  base-url: ${ALPACA_BROKER_BASE_URL}
  api-key:  ${ALPACA_BROKER_API_KEY}
  secret-key: ${ALPACA_BROKER_SECRET_KEY}
  auth-url: https://authx.sandbox.alpaca.markets
markets.service.url: http://localhost:8083
notification.service.url: http://localhost:8084
audit.service.url: http://localhost:8085
commission.percentage: ${COMMISSION_PERCENTAGE:0.02}
jwt.secret: ${JWT_SECRET}
```

### A3. Seguridad JWT (mismo patrón que markets-service)

| Archivo | Descripción |
|---|---|
| `util/JwtUtil.java` | `extractUserId`, `extractAlpacaAccountId`, `validateToken` — bytes UTF-8 |
| `security/JwtFilter.java` | Lee Bearer, valida, pone userId como principal y alpacaAccountId en atributo del request |
| `config/SecurityConfig.java` | Protege `/api/v1/**`, CORS habilitado, 401 custom, sin sesión HTTP |
| `config/CorsConfig.java` | Permite `http://localhost:5173` |

### A4. Cliente Alpaca Broker

| Archivo | Descripción |
|---|---|
| `service/AlpacaTokenService.java` | OAuth2 Client Credentials, caché 15 min — misma lógica que identity-service |
| `config/AlpacaBrokerConfig.java` | `RestClient` bean con baseUrl + interceptor Bearer token |

### A5. TradingServiceApplication.java — anotaciones

Agregar `@EnableAsync` y `@EnableRetry`.

---

## B. trading-service — dominio

### B1. Entidades JPA (mapean tablas ya existentes en init.sql)

| Archivo | Tabla | Notas |
|---|---|---|
| `model/Account.java` | `trading.accounts` | `userId UUID PK`, `balance BigDecimal` |
| `model/Holding.java` | `trading.holdings` | PK compuesto `(userId, symbol)` via `@EmbeddedId` |
| `model/HoldingId.java` | — | `@Embeddable` con userId + symbol |
| `model/Order.java` | `trading.orders` | Todos los campos del contrato; `status` como `@Enumerated(STRING)` |
| `model/OrderStatus.java` | — | Enum: `SENT, EXECUTED, REJECTED, FAILED` |

### B2. Repositorios

| Archivo | Métodos clave |
|---|---|
| `repository/AccountRepository.java` | `findById(UUID)` |
| `repository/HoldingRepository.java` | `findById(HoldingId)` |
| `repository/OrderRepository.java` | `findByUserIdOrderByCreatedAtDesc(UUID, Pageable)` |

### B3. DTOs

**Request / Response del contrato:**
- `dto/request/PlaceOrderRequest.java` — `@NotBlank symbol`, `@Min(1) quantity`, `@NotNull orderType`
- `dto/response/OrderResponse.java`
- `dto/response/PortfolioResponse.java` + `dto/response/HoldingItem.java`

**Comunicación con Alpaca (interno):**
- `dto/AlpacaOrderRequest.java` — `{ symbol, qty, side, type, time_in_force }`
- `dto/AlpacaOrderResponse.java` — `{ id, status, filledAvgPrice }`

**Comunicación con otros servicios (interno):**
- `dto/MarketStatusDto.java` — respuesta de `GET /markets/status/{symbol}`
- `dto/StockPriceDto.java` — respuesta de `GET /markets/stocks/{symbol}`
- `dto/NotificationRequest.java`
- `dto/AuditEventRequest.java`

### B4. Excepciones

| Clase | HTTP | Mensaje |
|---|---|---|
| `InsufficientFundsException` | 400 | "Fondos insuficientes" |
| `InsufficientHoldingsException` | 400 | "Cantidad insuficiente de acciones" |
| `MarketClosedException` | 422 | "El mercado está cerrado" |
| `PriceServiceUnavailableException` | 503 | "Servicio de precios no disponible temporalmente" |
| `GlobalExceptionHandler` | — | Maneja todas + 401 custom |

---

## C. trading-service — lógica

### C1. Clientes HTTP

| Archivo | Descripción |
|---|---|
| `client/MarketsServiceClient.java` | `getMarketStatus(symbol, token)` y `getStockPrice(symbol, token)` — síncronos; lanza `PriceServiceUnavailableException` si fallan |
| `client/AlpacaBrokerClient.java` | `placeOrder(accountId, request)` y `getOrderStatus(accountId, orderId)` — usa el `RestClient` bean de Alpaca |
| `client/NotificationClient.java` | `notifyOrderExecuted(request)` — `@Async @Retryable(3 intentos, 1 s backoff)` |
| `client/AuditClient.java` | `sendEvent(request)` — `@Async @Retryable(3 intentos, 1 s backoff)` |

### C2. OrderService — flujo compra

`placeBuyOrder(userId, alpacaAccountId, request)`:
1. `MarketsServiceClient.getMarketStatus` → si closed → `MarketClosedException`
2. `MarketsServiceClient.getStockPrice` → `estimatedPrice`
3. `estimatedCost = estimatedPrice * quantity * 1.02`
4. `accountRepo.findById(userId)` → si `balance < estimatedCost` → `InsufficientFundsException`
5. Guardar `Order{status=SENT}`
6. `AlpacaBrokerClient.placeOrder(accountId, {symbol, qty, side:"buy", type:"market", tif:"day"})`
7. Polling (5 × 1 s): `getOrderStatus` hasta `filled` / `rejected` / `canceled`
8. Si `filled`:
   - `commission = executedPrice * quantity * 0.02`
   - `account.balance -= executedPrice * quantity + commission`
   - Upsert holding: `avgCost = (oldQty*oldAvgCost + qty*executedPrice) / (oldQty+qty)`
   - `order.status = EXECUTED`
9. Si `rejected`/`canceled` o timeout: `order.status = REJECTED` o `FAILED`; no tocar saldo
10. `AuditClient.sendEvent(ORDER_PLACED)`, `AuditClient.sendEvent(ORDER_EXECUTED | ORDER_REJECTED)` — async
11. Si EXECUTED: `NotificationClient.notifyOrderExecuted(...)` — async
12. Retornar `OrderResponse`

### C2b. OrderService — flujo venta

`placeSellOrder(userId, alpacaAccountId, request)`:
1-2. Igual que compra (status + precio)
3. `holdingRepo.findById({userId, symbol})` → si `holding.quantity < quantity` → `InsufficientHoldingsException`
4. `AlpacaBrokerClient.placeOrder(..., side:"sell")`
5. Polling igual
6. Si `filled`:
   - `commission = executedPrice * quantity * 0.02`
   - `account.balance += executedPrice * quantity - commission`
   - `holding.quantity -= quantity` → si 0 → `holdingRepo.delete(holding)`
   - `order.status = EXECUTED`
7. Si rejected/timeout: igual que compra
8. Async audit + notification

### C3. PortfolioService

`getPortfolio(userId, token)`:
1. `accountRepo.findById(userId)` → balance (0 si no existe)
2. `holdingRepo.findAllByUserId(userId)` → lista de holdings
3. Por cada holding: `MarketsServiceClient.getStockPrice(symbol, token)` — best-effort (null si falla)
4. Calcular `totalValue`, `gainDollars`, `gainPct` por holding
5. Retornar `PortfolioResponse{balance, pricesAvailable, holdings}`

### C4. Controladores

| Archivo | Endpoints |
|---|---|
| `controller/OrderController.java` | `POST /api/v1/orders/buy`, `POST /api/v1/orders/sell` |
| `controller/PortfolioController.java` | `GET /api/v1/portfolio` |

Ambos extraen `userId` y `alpacaAccountId` del `SecurityContext` (puestos por `JwtFilter`).

---

## D. trading-service — tests

### D1. OrderServiceImplTest (unitarios — todos los repos y clientes mockeados)

| Test |
|---|
| `buy_success` — balance actualizado, holding creado, status EXECUTED |
| `buy_insufficientFunds` — lanza `InsufficientFundsException` |
| `buy_marketClosed` — lanza `MarketClosedException` |
| `buy_alpacaRejects` — status REJECTED, saldo sin cambio |
| `sell_success` — balance aumentado, holding reducido |
| `sell_insufficientHoldings` — lanza `InsufficientHoldingsException` |
| `sell_marketClosed` — lanza `MarketClosedException` |
| `sell_allSharesSold_removesHolding` — holding eliminado cuando quantity llega a 0 |

### D2. OrderControllerTest (integración — MockMvc + Testcontainers PostgreSQL)

`MarketsServiceClient` y `AlpacaBrokerClient` mockeados con `@MockitoBean`.

| Test |
|---|
| `POST /orders/buy → 201 EXECUTED` |
| `POST /orders/buy → 400 fondos insuficientes` |
| `POST /orders/buy → 422 mercado cerrado` |
| `POST /orders/sell → 201 EXECUTED` |
| `POST /orders/sell → 400 cantidad insuficiente` |
| `POST /orders/buy → 401 sin JWT` |

### D3. TradingServiceApplicationTests

`contextLoads()` con Testcontainers + `@MockitoBean AlpacaTokenService`.

### D4. test/resources/application.yaml

Self-contained (reemplaza al main): JWT secret, Alpaca config (test-key), URLs de services, JPA trading schema, cache none. Sin datasource URL (la provee Testcontainers `@ServiceConnection`).

---

## E. frontend

### E1. Archivos nuevos

| Archivo | Descripción |
|---|---|
| `features/dashboard/services/tradingService.ts` | `placeOrder(side, symbol, qty, token)`, `getPortfolio(token)` |
| `features/dashboard/store/tradingStore.ts` | `balance: number \| null`, `loadPortfolio(token)`, `updateBalance(n)` |

### E2. Archivos modificados

| Archivo | Cambio |
|---|---|
| `components/layout/AppShell.tsx` | Llamar `tradingStore.loadPortfolio(token)` al montar (junto a `loadProfile`) |
| `features/dashboard/components/BalancePill.tsx` | Lee balance de `tradingStore`; muestra skeleton si null |
| `features/dashboard/hooks/useOrderDraft.ts` | Acepta `currentPrice: number \| null` como parámetro; elimina `mockStocks`, `platform`, `broker` de `OrderComputed` |
| `features/dashboard/components/OrderSummary.tsx` | Elimina prop `hasBroker` y sub-rows de broker; muestra Subtotal + Comisión (2%) + Total |
| `features/dashboard/components/NewOrderPanel.tsx` | Fetch de todos los stocks sin filtros al montar; usa precio real en `useOrderDraft` |
| `features/dashboard/components/OrderConfirmModal.tsx` | Llama `tradingService.placeOrder`; agrega estado `"error"`; muestra orderId real; llama `updateBalance` on success |
