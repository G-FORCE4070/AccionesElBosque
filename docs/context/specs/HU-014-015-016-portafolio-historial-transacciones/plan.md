# Plan — HU-014 + HU-015 + HU-016

## Estado de partida

| Componente | Estado |
|-----------|--------|
| `PortfolioController` + `PortfolioServiceImpl` | Existe — bug a corregir |
| `GET /orders` | No existe |
| `GET /transactions` | No existe |
| `getPortfolio()` en `tradingService.ts` | Existe |
| UI de las 3 páginas | Completa con mock data |

---

## Archivos a crear o modificar

### Backend (`trading-service`)

| # | Archivo | Acción |
|---|---------|--------|
| B1 | `service/impl/PortfolioServiceImpl.java` | Modificar — bug fix: retornar holdings con null cuando prices fallan |
| B2 | `repository/OrderRepository.java` | Modificar — agregar queries con Specification |
| B3 | `dto/response/OrderHistoryItem.java` | Crear |
| B4 | `dto/response/OrderHistoryResponse.java` | Crear |
| B5 | `dto/response/TransactionItem.java` | Crear |
| B6 | `dto/response/TransactionHistoryResponse.java` | Crear |
| B7 | `service/OrderHistoryService.java` | Crear (interface) |
| B8 | `service/impl/OrderHistoryServiceImpl.java` | Crear |
| B9 | `service/TransactionService.java` | Crear (interface) |
| B10 | `service/impl/TransactionServiceImpl.java` | Crear |
| B11 | `controller/OrderHistoryController.java` | Crear — `GET /api/v1/orders` |
| B12 | `controller/TransactionController.java` | Crear — `GET /api/v1/transactions` |
| B13 | `test/service/impl/PortfolioServiceImplTest.java` | Modificar — añadir caso prices-unavailable con holdings |
| B14 | `test/service/impl/OrderHistoryServiceImplTest.java` | Crear |
| B15 | `test/service/impl/TransactionServiceImplTest.java` | Crear |
| B16 | `test/controller/OrderHistoryControllerTest.java` | Crear |
| B17 | `test/controller/TransactionControllerTest.java` | Crear |

### Frontend (`frontend/src`)

| # | Archivo | Acción |
|---|---------|--------|
| F1 | `features/dashboard/services/tradingService.ts` | Modificar — agregar `getOrders()`, `getTransactions()`, interfaces `OrderHistoryItem`, `OrderHistoryResponse`, `TransactionItem`, `TransactionHistoryResponse` |
| F2 | `features/portfolio/data/mockHoldings.ts` | Modificar — renombrar `gainAbs` → `gainDollars` en interface `Holding`; actualizar mock data |
| F3 | `features/portfolio/components/PortfolioRow.tsx` | Modificar — usar `gainDollars` |
| F4 | `features/portfolio/components/PortfolioSummary.tsx` | Modificar — usar `gainDollars` |
| F5 | `features/portfolio/components/PortfolioDetailView.tsx` | Modificar — usar `gainDollars` |
| F6 | `features/portfolio/components/PortfolioBarChart.tsx` | Modificar — usar `gainDollars` (si aplica) |
| F7 | `features/portfolio/pages/PortfolioPage.tsx` | Modificar — conectar a `getPortfolio()`, balance real, pricesAvailable warning, prefs de profileStore |
| F8 | `features/orders/data/mockOrders.ts` | Modificar — reemplazar `OrderStatus` por los 5 estados del backend |
| F9 | `features/orders/components/OrderStatusBadge.tsx` | Modificar — mapear 5 estados del backend |
| F10 | `features/orders/components/OrdersFilters.tsx` | Modificar — 5 estados en dropdown |
| F11 | `features/orders/components/OrderRow.tsx` | Modificar — eliminar botón Cancelar; adaptar campos del API |
| F12 | `features/orders/components/OrdersTable.tsx` | Modificar — recibir `OrderHistoryItem[]` |
| F13 | `features/orders/pages/OrdersPage.tsx` | Modificar — eliminar mock, proposals tab, `CancelOrderDialog`; conectar API; paginación |
| F14 | `features/orders/hooks/useOrderFilters.ts` | Eliminar (filtrado pasa a ser server-side) |
| F15 | `features/transactions/data/mockTransactions.ts` | Modificar — simplificar interface `Transaction` a campos del API |
| F16 | `features/transactions/components/TransactionRow.tsx` | Modificar — adaptar campos; pasar `brokerName: null` a `CommissionBreakdown` |
| F17 | `features/transactions/pages/TransactionsPage.tsx` | Modificar — eliminar mock; conectar API; paginación; totalCommission |

**Componentes que se eliminan** (están en `OrdersPage` como imports, se borran sus archivos):
- `features/orders/components/BrokerProposalsList.tsx`
- `features/orders/components/ProposalCard.tsx`
- `features/orders/components/CancelOrderDialog.tsx`

---

## Orden de ejecución

### Bloque A — Bug fix portfolio (backend)
1. **B1** — Corregir `PortfolioServiceImpl.buildHoldingItem()`: cuando el precio falla, retornar `HoldingItem` con campos de precio en `null` en lugar de retornar `null` y descartar el holding.
2. **B13** — Actualizar `PortfolioServiceImplTest`: añadir caso que verifica que con markets-service caído, los holdings aparecen con `currentPrice=null`, `pricesAvailable=false`.

### Bloque B — Historial de órdenes (backend)
3. **B3** `OrderHistoryItem` record
4. **B4** `OrderHistoryResponse` record
5. **B2** `OrderRepository` — agregar `findAll(Specification, Pageable)`; extender con `JpaSpecificationExecutor<Order>`
6. **B7** `OrderHistoryService` interface
7. **B8** `OrderHistoryServiceImpl` — lógica de paginación + filtros (symbol, status, startDate/endDate por `createdAt`)
8. **B11** `OrderHistoryController` — `GET /api/v1/orders`
9. **B14** `OrderHistoryServiceImplTest`
10. **B16** `OrderHistoryControllerTest`

### Bloque C — Historial de transacciones (backend)
11. **B5** `TransactionItem` record
12. **B6** `TransactionHistoryResponse` record
13. **B9** `TransactionService` interface
14. **B10** `TransactionServiceImpl` — filtra solo EXECUTED; calcula `subtotal`, `total`, `totalCommission`; filtros (type, symbol, startDate/endDate por `executedAt`)
15. **B12** `TransactionController` — `GET /api/v1/transactions`
16. **B15** `TransactionServiceImplTest`
17. **B17** `TransactionControllerTest`

### Bloque D — Frontend portfolio
18. **F2** Renombrar `gainAbs` → `gainDollars` en interface `Holding` y mock data
19. **F3–F6** Actualizar componentes que usan `gainAbs`
20. **F7** Conectar `PortfolioPage` a `getPortfolio()`: balance real, pricesAvailable warning, campos null como `—`, prefs de profileStore

### Bloque E — Frontend órdenes
21. **F8** Actualizar `mockOrders.ts`: `OrderStatus` → 5 estados del backend; adaptar campos a los del API
22. **F9** Actualizar `OrderStatusBadge` con el mapeo de 5 estados
23. **F10** Actualizar `OrdersFilters` con los 5 estados
24. **F1** Agregar `getOrders()` + interfaces en `tradingService.ts`
25. **F11–F12** Actualizar `OrderRow` y `OrdersTable` para campos del API (eliminar cancel, adaptar columnas)
26. **F14** Eliminar `useOrderFilters.ts`
27. **F13** Reescribir `OrdersPage`: eliminar mock + proposals tab + componentes broker; conectar API; paginación

### Bloque F — Frontend transacciones
28. **F15** Simplificar interface `Transaction` en `mockTransactions.ts`
29. **F16** Actualizar `TransactionRow` para los campos del API
30. **F1** (ya hecho) `getTransactions()` en `tradingService.ts`
31. **F17** Reescribir `TransactionsPage`: eliminar mock; conectar API; paginación; totalCommission

---

## Tests a escribir por bloque

### Backend
- `PortfolioServiceImplTest` (existente): añadir `getPortfolio_pricesUnavailable_returnsHoldingsWithNullFields`
- `OrderHistoryServiceImplTest`:
  - `getOrders_noFilters_returnsPaginatedList`
  - `getOrders_filterBySymbol_returnsFiltered`
  - `getOrders_filterByStatus_returnsFiltered`
  - `getOrders_filterByDateRange_returnsFiltered`
  - `getOrders_noOrders_returnsEmptyPage`
- `TransactionServiceImplTest`:
  - `getTransactions_noFilters_returnsOnlyExecuted`
  - `getTransactions_filterByType_returnsFiltered`
  - `getTransactions_calculatesTotal_buyIsNegative`
  - `getTransactions_calculatesTotal_sellIsPositive`
  - `getTransactions_calculatesTotalCommission`
  - `getTransactions_noExecutedOrders_returnsEmpty`
- `OrderHistoryControllerTest` (integración): happy path con MockMvc + JWT
- `TransactionControllerTest` (integración): happy path con MockMvc + JWT

### Frontend
No hay tests de frontend en este proyecto.

---

## Notas técnicas del plan

### Filtros dinámicos con JPA Specification
`OrderRepository` extenderá `JpaSpecificationExecutor<Order>`. Se creará una clase estática o factory de `Specification<Order>` con predicados opcionales:
- `hasSymbol(String symbol)` — `IS NULL OR LIKE`
- `hasStatus(OrderStatus status)` — `IS NULL OR =`
- `afterDate(LocalDate start)` — `createdAt >= start.atStartOfDay(UTC)`
- `beforeDate(LocalDate end)` — `createdAt < end.plusDays(1).atStartOfDay(UTC)`

`TransactionRepository` no se crea — reutiliza `OrderRepository` con filtro `status = EXECUTED`.

### Paginación en frontend
Patrón igual para OrdersPage y TransactionsPage:
```typescript
const [page, setPage] = useState(0);
const [totalPages, setTotalPages] = useState(1);
// en useEffect: fetch → setData; setTotalPages(response.totalPages)
// al cambiar filtros: setPage(0) antes del fetch
```
Controles: botón Anterior (disabled si page===0), botón Siguiente (disabled si page===totalPages-1), texto "Página X de N".

### Campos de OrderHistoryItem vs mock Order
| Mock (frontend) | API (backend) |
|-----------------|---------------|
| `id` | `orderId` |
| `date` | `createdAt` |
| `type` | `orderType` |
| `qty` | `quantity` |
| `price` | `executedPrice` (nullable) |
| `status` (10 inventados) | `status` (5 reales) |

### Campos de TransactionItem vs mock Transaction
| Mock (frontend) | API (backend) |
|-----------------|---------------|
| `id` | `transactionId` |
| `date` | `date` (executedAt) |
| `side` ("buy"/"sell") | `type` ("BUY"/"SELL") |
| `qty` | `quantity` |
| `unitPrice` | `unitPrice` |
| `subtotal` | `subtotal` |
| `commissionTotal` | `commission` |
| `total` | `total` |
| `splitPlatform`, `splitBroker`, `brokerName` | ← eliminados |
| `commissionPct` | ← eliminado (hardcoded 2%, calculable en frontend si se necesita) |
