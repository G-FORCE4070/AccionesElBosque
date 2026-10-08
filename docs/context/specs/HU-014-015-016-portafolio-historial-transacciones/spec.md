# HU-014 + HU-015 + HU-016
# Portafolio · Historial de Órdenes · Historial de Transacciones

---

## Justificación de agrupación

Estas tres HUs se implementan en un solo ciclo porque:

1. **Mismo servicio de backend**: las tres dependen exclusivamente de `trading-service`
   y sus entidades `Order`, `Holding`, `Account` en el schema `trading`.
2. **Misma migración frontend**: las páginas `/portfolio`, `/orders` y `/transactions`
   ya están construidas con UI completa pero con datos mock. La tarea común es
   desconectar el mock y conectar la API real, tocando los mismos archivos.
3. **Estado parcial del backend**: `GET /portfolio` ya está implementado
   (`PortfolioController` + `PortfolioServiceImpl`) pero tiene un bug. Los endpoints
   de historial de órdenes y transacciones faltan completamente. Tiene sentido cerrar
   los tres en una sola HU para no dejar el portafolio conectado con las otras páginas
   aún en mock.
4. **`getPortfolio()`** ya existe en `tradingService.ts` del frontend.

---

## Prioridad y estimación

| Campo      | Valor                                         |
|------------|-----------------------------------------------|
| Prioridad  | Must                                          |
| Estimación | 8 puntos (3 + 3 + 2)                          |
| Épica      | Trading                                       |
| Feature    | Portafolio e historial                        |
| Servicio   | trading-service (backend), frontend (3 páginas) |

---

## Referencias

- `docs/context/contracts/trading-service.yaml` — `GET /portfolio`, `GET /orders`, `GET /transactions`
- `docs/context/architecture.md` — ADR-06, ADR-10

---

## Estado actual (antes de esta HU)

| Capa | Componente | Estado |
|------|-----------|--------|
| Backend | `GET /portfolio` | Implementado — con bug: si precios fallan, descarta holdings |
| Backend | `GET /orders` | Falta |
| Backend | `GET /transactions` | Falta |
| Frontend | `PortfolioPage` | UI lista, usa `mockHoldings` y balance hardcodeado `$100,000.00` |
| Frontend | `OrdersPage` | UI lista, usa `mockOrders` con 10 estados inventados |
| Frontend | `TransactionsPage` | UI lista, usa `mockTransactions` con split de comisión ficticio |
| Frontend | `getPortfolio()` | Ya existe en `tradingService.ts` |
| Frontend | `getOrders()` | Falta |
| Frontend | `getTransactions()` | Falta |

---

## Historia 1 — HU-014: Visualización de portafolio y saldo

**Como** inversionista,
**quiero** ver mi saldo disponible y mis posiciones actuales con su valor de mercado,
**para** conocer el estado de mis inversiones.

### Criterios de aceptación

#### CA-01: Balance real desde API

**Dado que** el inversionista abre `/portfolio`
**Cuando** el frontend llama a `GET /api/v1/portfolio`
**Entonces** el balance de la pill superior derecha muestra `response.balance` (no el hardcodeado `$100,000.00`).

---

#### CA-02: Holdings con precios actuales

**Dado que** el inversionista tiene posiciones en `trading.holdings`
**Cuando** llama a `GET /api/v1/portfolio`
**Entonces** la respuesta incluye por cada holding:
`symbol`, `quantity`, `avgCost`, `currentPrice`, `totalValue`, `gainPct`, `gainDollars`

**Y** la tabla muestra `currentPrice`, `totalValue`, `gainDollars` (G/P $), `gainPct` (G/P %).

> Adaptación de interfaz: el frontend mock usa `gainAbs`; se renombra a `gainDollars`
> en la interfaz `Holding` y todos los componentes que la usan
> (`PortfolioTable`, `PortfolioDetailView`, `PortfolioRow`, `PortfolioSummary`, `PortfolioBarChart`).

---

#### CA-03: Precios no disponibles — bug fix

**Dado que** markets-service no responde
**Cuando** `buildHoldingItem()` falla para uno o más símbolos
**Entonces** el backend retorna `pricesAvailable: false`
**Y** los holdings SÍ están en la respuesta con `currentPrice`, `totalValue`, `gainPct`, `gainDollars` en `null`

> Bug actual: `PortfolioServiceImpl` descarta el holding cuando el precio falla
> (retorna `null` y no lo agrega a `items`). Este HU lo corrige.

**Y** el frontend muestra el aviso:
```
"Precios en tiempo real no disponibles. Mostrando datos de posición sin valorización actual."
```
**Y** los campos de precio muestran `—` cuando son `null`.

---

#### CA-04: Portafolio vacío

**Dado que** el inversionista no tiene ninguna posición
**Cuando** llama a `GET /api/v1/portfolio`
**Entonces** `holdings` es un array vacío
**Y** el frontend muestra:
```
"Tu portafolio está vacío — Comienza a operar desde la Terminal de Trading."
```

---

#### CA-05: Preferencias de visualización aplicadas

**Dado que** el usuario tiene preferencias en `profileStore.portfolio`
**Cuando** `PortfolioPage` se monta
**Entonces** los estados `view` y `sortBy` se inicializan desde `profileStore`
(no hardcodeados como `"list"` y `"totalValue"`):
- `viewType` → vista inicial (list / bars / detail)
- `defaultOrder` → ordenamiento inicial
- `showLowWeight: false` → oculta holdings con `totalValue < $10`

---

## Historia 2 — HU-015: Historial de órdenes

**Como** inversionista,
**quiero** consultar todas mis órdenes pasadas con su estado,
**para** hacer seguimiento a mi actividad de trading.

### Criterios de aceptación

#### CA-06: Endpoint `GET /orders` paginado

**Dado que** el inversionista tiene órdenes registradas
**Cuando** llama a `GET /api/v1/orders?page=0&size=20`
**Entonces** el sistema responde con HTTP 200
**Y** el cuerpo es un `OrderHistoryResponse`:
```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```
**Y** cada `OrderHistoryItem` incluye:
`orderId`, `symbol`, `side` (buy/sell), `orderType`, `quantity`,
`executedPrice` (null si no ejecutada), `commission` (null si no ejecutada),
`status` (PENDING_QUEUE | SENT | EXECUTED | REJECTED | FAILED),
`createdAt`, `executedAt` (null si no ejecutada)

**Y** los resultados se ordenan por `createdAt` DESC.

---

#### CA-07: Filtros de órdenes

**Dado que** el inversionista quiere filtrar el historial
**Cuando** llama a `GET /api/v1/orders` con parámetros opcionales:
- `?symbol=AAPL`
- `?status=EXECUTED` (valores: PENDING_QUEUE | SENT | EXECUTED | REJECTED | FAILED)
- `?startDate=2026-05-01&endDate=2026-05-27` (filtran por `createdAt`)
- Combinaciones de los anteriores

**Entonces** el sistema responde con HTTP 200 con solo las órdenes que cumplen los criterios.

---

#### CA-08: Sin órdenes

**Dado que** el inversionista no ha realizado ninguna operación
**Cuando** llama a `GET /api/v1/orders`
**Entonces** responde HTTP 200 con `content: []`, `totalElements: 0`
**Y** el frontend muestra:
```
"No se encontraron órdenes con los filtros aplicados."
```

---

#### CA-09: Frontend `OrdersPage` conectada a API real

**Dado que** la página `/orders` se carga
**Cuando** el frontend llama a `GET /api/v1/orders` con los filtros actuales
**Entonces**:
- Se elimina el uso de `mockOrders`
- Se elimina la pestaña "Propuestas del comisionista" y el componente `BrokerProposalsList`
  (fuera de alcance — sin sistema de comisionistas en MVP)
- Se elimina el botón "Cancelar" en `OrderRow` (sin endpoint de cancelación)
- `OrderStatus` type se actualiza a los 5 estados del backend
- `OrderStatusBadge` se actualiza con el mapeo:
  - `PENDING_QUEUE` → "En Cola" (gold)
  - `SENT` → "Enviada" (sent)
  - `EXECUTED` → "Ejecutada" (success)
  - `REJECTED` → "Rechazada" (danger)
  - `FAILED` → "Fallida" (danger)
- El dropdown de filtro "Estado" en `OrdersFilters` usa estos 5 estados
- La columna "TIPO" muestra siempre "Market" (único tipo en MVP)
- Hay controles de paginación: Anterior / Siguiente + indicador página X de N
- Los filtros envían parámetros a la API (no filtrado client-side)

---

## Historia 3 — HU-016: Historial de transacciones ejecutadas

**Como** inversionista,
**quiero** ver el detalle financiero de todas mis operaciones ejecutadas con el desglose
de comisiones,
**para** conocer el impacto real de mis compras y ventas en mi capital.

### Criterios de aceptación

#### CA-10: Endpoint `GET /transactions` paginado

**Dado que** el inversionista tiene órdenes ejecutadas
**Cuando** llama a `GET /api/v1/transactions?page=0&size=20`
**Entonces** el sistema responde con HTTP 200
**Y** el cuerpo es un `TransactionHistoryResponse`:
```json
{
  "content": [...],
  "totalElements": 18,
  "totalPages": 1,
  "page": 0,
  "size": 20,
  "totalCommission": 145.50
}
```
**Y** solo retorna órdenes con `status = EXECUTED`
**Y** cada `TransactionItem` incluye:
`transactionId` (= orderId), `date` (= executedAt), `type` (BUY/SELL),
`symbol`, `quantity`, `unitPrice` (= executedPrice),
`subtotal` (unitPrice × quantity), `commission`, `total`
- `total` BUY: `-(subtotal + commission)` (negativo — egreso)
- `total` SELL: `+(subtotal - commission)` (positivo — ingreso)

**Y** `totalCommission` es la suma de comisiones del resultado paginado actual.

---

#### CA-11: Filtros de transacciones

**Dado que** el inversionista quiere filtrar
**Cuando** llama a `GET /api/v1/transactions` con parámetros opcionales:
- `?type=BUY` | `?type=SELL`
- `?symbol=AAPL`
- `?startDate=2026-05-01&endDate=2026-05-27` (filtran por `executedAt`)

**Entonces** el sistema responde con HTTP 200 con solo las transacciones que cumplen los criterios.

---

#### CA-12: Sin transacciones

**Dado que** el inversionista no tiene órdenes ejecutadas
**Cuando** llama a `GET /api/v1/transactions`
**Entonces** responde HTTP 200 con `content: []`
**Y** el frontend muestra:
```
"Sin resultados — Prueba ajustando los filtros para ver más transacciones."
```

---

#### CA-13: Frontend `TransactionsPage` conectada a API real

**Dado que** la página `/transactions` se carga
**Cuando** el frontend llama a `GET /api/v1/transactions` con los filtros actuales
**Entonces**:
- Se elimina el uso de `mockTransactions`
- Interfaz `Transaction` se limpia: elimina `splitPlatform`, `splitBroker`,
  `commissionPct`, `brokerName`; campos del API: `transactionId`, `date`, `type`,
  `symbol`, `quantity`, `unitPrice`, `subtotal`, `commission`, `total`
- `CommissionBreakdown` recibe `brokerName: null` → muestra 100% ElBosque
  (lógica ya existe en el componente)
- Controles de paginación: Anterior / Siguiente + indicador página X de N
- Resumen al pie de tabla: `"Comisión total en esta página: $XX.XX"`
- Los filtros envían parámetros a la API (no filtrado client-side)

---

## Definition of Done

### Backend

#### HU-014 — Portfolio (bug fix)
- [ ] `PortfolioServiceImpl.getPortfolio()` retorna todos los holdings con campos de precio
      en `null` cuando markets-service no está disponible (no los descarta)
- [ ] Tests unitarios de `PortfolioServiceImpl` cubren: precios ok, precios no disponibles
      (holding con null), portafolio vacío

#### HU-015 — Historial de órdenes
- [ ] `OrderHistoryService` interface + `OrderHistoryServiceImpl` creados
- [ ] `OrderHistoryController` con `GET /api/v1/orders` (JWT requerido, paginación, filtros)
- [ ] Filtros: `symbol`, `status` (enum 5 valores), `startDate`/`endDate` por `createdAt`
- [ ] Ordenamiento por `createdAt` DESC
- [ ] Tests unitarios `OrderHistoryServiceImplTest` (paginada, filtros, vacío)
- [ ] Test de integración MockMvc para el endpoint

#### HU-016 — Historial de transacciones
- [ ] `TransactionService` interface + `TransactionServiceImpl` creados
- [ ] `TransactionController` con `GET /api/v1/transactions` (JWT requerido, paginación, filtros)
- [ ] Solo retorna órdenes `EXECUTED`; filtros: `type`, `symbol`, `startDate`/`endDate` por `executedAt`
- [ ] `total` calculado: BUY = `-(subtotal + commission)`, SELL = `+(subtotal - commission)`
- [ ] `totalCommission` = suma de comisiones del resultado paginado
- [ ] Tests unitarios `TransactionServiceImplTest` (paginada, filtros, cálculo total, totalCommission)
- [ ] Test de integración MockMvc para el endpoint

#### Todos
- [ ] `mvn test` pasa sin errores

### Frontend

#### HU-014 — Portfolio
- [ ] `PortfolioPage` usa `getPortfolio()` en lugar de `mockHoldings`
- [ ] Interfaz `Holding` renombra `gainAbs` → `gainDollars`; todos los componentes actualizados
- [ ] Balance en pill superior muestra `response.balance`
- [ ] Aviso visible cuando `pricesAvailable: false`
- [ ] Campos null se muestran como `—`
- [ ] `view` y `sortBy` inicializados desde `profileStore.portfolio`
- [ ] Filtro `showLowWeight` aplicado sobre los holdings antes de renderizar

#### HU-015 — Historial de órdenes
- [ ] `getOrders(token, filters, page, size)` creado en `tradingService.ts`
- [ ] `OrdersPage` usa datos de la API; elimina `mockOrders`
- [ ] Tab "Propuestas" y componentes `BrokerProposalsList`, `ProposalCard`, `CancelOrderDialog` eliminados
- [ ] `OrderStatus` type y `OrdersFilters` actualizados a 5 estados del backend
- [ ] `OrderStatusBadge` actualizado con el mapeo de 5 estados
- [ ] Controles de paginación funcionales (Anterior / Siguiente)

#### HU-016 — Historial de transacciones
- [ ] `getTransactions(token, filters, page, size)` creado en `tradingService.ts`
- [ ] `TransactionsPage` usa datos de la API; elimina `mockTransactions`
- [ ] Interfaz `Transaction` simplificada al contrato del API
- [ ] `CommissionBreakdown` con `brokerName: null`
- [ ] Controles de paginación funcionales
- [ ] Resumen `totalCommission` al pie de la tabla

---

## Notas técnicas

### Backend
- Para filtros dinámicos con parámetros opcionales se recomienda `JpaSpecificationExecutor`
  o queries JPQL con `IS NULL OR` logic. Evitar N queries separadas.
- `commission` ya está almacenado en `trading.orders` — no recalcular.
- `subtotal = executedPrice * quantity` se calcula en la capa de servicio al construir el DTO.
- `totalCommission` = suma de `commission` de los ítems del resultado paginado actual (no del historial total).
- `OrderHistoryController` y `TransactionController` pueden ser endpoints separados o estar
  juntos en `OrderController` — lo que sea más limpio.

### Frontend
- La paginación es **server-side**: filtros y número de página se envían como query params.
  El hook `useOrderFilters` existente (filtrado client-side) se reemplaza por un hook de fetching.
- Al cambiar cualquier filtro, resetear `page = 0` antes de hacer el fetch.
- `CommissionBreakdown` no necesita cambios de lógica; solo pasar `brokerName: null`.

### Decisiones de alcance
- **Sin cancelación**: no hay endpoint `DELETE /orders/{id}`. El botón de cancelar era solo mock.
- **Sin propuestas de comisionista**: UI especulativa para una HU futura. Se elimina.
- **Comisión sin split**: toda la comisión va a ElBosque en MVP; `brokerName = null` → 100%.
- **Tipo de orden siempre Market**: desde HU-013. La columna TIPO se mantiene por completitud.
