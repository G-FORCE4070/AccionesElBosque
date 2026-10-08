# Tasks — HU-014 + HU-015 + HU-016

## Bloque A — Bug fix portfolio (backend)

- [x] **T-A-1** Corregir `PortfolioServiceImpl.buildHoldingItem()`: cuando el precio falla, retornar `HoldingItem` con campos de precio en `null` (no retornar `null` ni descartar el holding). Actualizar `getPortfolio()` para incluir todos los holdings aunque alguno falle.
- [x] **T-A-2** Actualizar `PortfolioServiceImplTest`: añadir test `getPortfolio_pricesUnavailable_returnsHoldingsWithNullFields` que verifica que con markets-service caído los holdings aparecen con `currentPrice=null` y `pricesAvailable=false`.

## Bloque B — Historial de órdenes (backend)

- [x] **T-B-1** Crear `dto/response/OrderHistoryItem.java` (record): `orderId`, `symbol`, `side`, `orderType`, `quantity`, `executedPrice` (nullable), `commission` (nullable), `status`, `createdAt`, `executedAt` (nullable).
- [x] **T-B-2** Crear `dto/response/OrderHistoryResponse.java` (record): `content`, `totalElements`, `totalPages`, `page`, `size`.
- [x] **T-B-3** Extender `OrderRepository` con `JpaSpecificationExecutor<Order>` y eliminar queries que ya no se necesitan si las cubre Specification.
- [x] **T-B-4** Crear `service/OrderHistoryService.java` (interface): método `getOrders(UUID userId, String symbol, String status, LocalDate startDate, LocalDate endDate, int page, int size)`.
- [x] **T-B-5** Crear `service/impl/OrderHistoryServiceImpl.java`: construye `Specification<Order>` con predicados opcionales (userId siempre requerido, resto opcionales), ejecuta `orderRepository.findAll(spec, pageable)` ordenado por `createdAt DESC`, mapea a `OrderHistoryItem`.
- [x] **T-B-6** Crear `controller/OrderHistoryController.java` con `GET /api/v1/orders` (JWT requerido, params: `page`, `size`, `symbol`, `status`, `startDate`, `endDate`). Retorna `OrderHistoryResponse`.
- [x] **T-B-7** Crear `test/service/impl/OrderHistoryServiceImplTest.java` con tests: lista paginada sin filtros, filtro por symbol, filtro por status, filtro por rango de fechas, lista vacía.
- [x] **T-B-8** Crear `test/controller/OrderHistoryControllerTest.java` (integración con MockMvc + JWT): happy path retorna 200 con lista paginada, sin JWT retorna 401.

## Bloque C — Historial de transacciones (backend)

- [x] **T-C-1** Crear `dto/response/TransactionItem.java` (record): `transactionId`, `date`, `type`, `symbol`, `quantity`, `unitPrice`, `subtotal`, `commission`, `total`.
- [x] **T-C-2** Crear `dto/response/TransactionHistoryResponse.java` (record): `content`, `totalElements`, `totalPages`, `page`, `size`, `totalCommission`.
- [x] **T-C-3** Crear `service/TransactionService.java` (interface): método `getTransactions(UUID userId, String type, String symbol, LocalDate startDate, LocalDate endDate, int page, int size)`.
- [x] **T-C-4** Crear `service/impl/TransactionServiceImpl.java`: filtra solo `status = EXECUTED`; Specification con userId + type + symbol + startDate/endDate por `executedAt`; calcula `subtotal = executedPrice × quantity`, `total` (BUY negativo, SELL positivo), `totalCommission` = suma de commission del resultado paginado.
- [x] **T-C-5** Crear `controller/TransactionController.java` con `GET /api/v1/transactions` (JWT requerido, params: `page`, `size`, `type`, `symbol`, `startDate`, `endDate`). Retorna `TransactionHistoryResponse`.
- [x] **T-C-6** Crear `test/service/impl/TransactionServiceImplTest.java` con tests: solo retorna EXECUTED, filtro por type, cálculo total BUY es negativo, cálculo total SELL es positivo, totalCommission correcto, lista vacía.
- [x] **T-C-7** Crear `test/controller/TransactionControllerTest.java` (integración con MockMvc + JWT): happy path retorna 200, sin JWT retorna 401.

## Bloque D — Frontend portfolio

- [x] **T-D-1** En `features/portfolio/data/mockHoldings.ts`: renombrar campo `gainAbs` → `gainDollars` en la interface `Holding` y en el array `mockHoldings`.
- [x] **T-D-2** Actualizar todos los componentes que usan `gainAbs`: `PortfolioRow.tsx`, `PortfolioSummary.tsx`, `PortfolioDetailView.tsx`, `PortfolioBarChart.tsx` (si aplica).
- [x] **T-D-3** Reescribir `features/portfolio/pages/PortfolioPage.tsx`: reemplazar `mockHoldings` con llamada a `getPortfolio()` de `tradingService.ts`; mostrar balance real desde `response.balance`; inicializar `view` y `sortBy` desde `profileStore.portfolio` (`viewType`, `defaultOrder`); aplicar filtro `showLowWeight` (ocultar holdings con `totalValue < 10`); mostrar aviso cuando `pricesAvailable=false`; mostrar `—` en campos null.

## Bloque E — Frontend órdenes

- [x] **T-E-1** En `features/orders/data/mockOrders.ts`: reemplazar `OrderStatus` type (10 valores inventados) por los 5 del backend (`PENDING_QUEUE | SENT | EXECUTED | REJECTED | FAILED`); adaptar la interface `Order` a los campos del API (`orderId`, `createdAt`, `orderType`, `quantity`, `executedPrice` nullable); eliminar `BrokerProposal` interface y `mockProposals`.
- [x] **T-E-2** Actualizar `features/orders/components/OrderStatusBadge.tsx`: mapear los 5 estados (`PENDING_QUEUE`→"En Cola" gold, `SENT`→"Enviada", `EXECUTED`→"Ejecutada" success, `REJECTED`→"Rechazada" danger, `FAILED`→"Fallida" danger).
- [x] **T-E-3** Actualizar `features/orders/components/OrdersFilters.tsx`: reemplazar `STATUS_OPTIONS` por los 5 estados del backend; actualizar type `OrderFilters.status`.
- [x] **T-E-4** Añadir `getOrders(token, params, page, size)` e interfaces `OrderHistoryItem`, `OrderHistoryResponse` a `features/dashboard/services/tradingService.ts`.
- [x] **T-E-5** Actualizar `features/orders/components/OrderRow.tsx`: adaptar campos del API (`orderId`, `createdAt`, `quantity`, `executedPrice`); eliminar botón "Cancelar" y su lógica.
- [x] **T-E-6** Actualizar `features/orders/components/OrdersTable.tsx`: recibir `OrderHistoryItem[]` en lugar de `Order[]`; actualizar columnas (eliminar "ACCIONES").
- [x] **T-E-7** Eliminar archivos: `features/orders/components/BrokerProposalsList.tsx`, `features/orders/components/ProposalCard.tsx`, `features/orders/components/CancelOrderDialog.tsx`, `features/orders/hooks/useOrderFilters.ts`.
- [x] **T-E-8** Reescribir `features/orders/pages/OrdersPage.tsx`: eliminar mock, proposals tab y componentes broker; conectar a `getOrders()`; filtros server-side; controles de paginación (Anterior / Siguiente + indicador página X de N).

## Bloque F — Frontend transacciones

- [x] **T-F-1** En `features/transactions/data/mockTransactions.ts`: simplificar interface `Transaction` a los campos del API (`transactionId`, `date`, `type`, `symbol`, `quantity`, `unitPrice`, `subtotal`, `commission`, `total`); eliminar `splitPlatform`, `splitBroker`, `brokerName`, `commissionPct`.
- [x] **T-F-2** Actualizar `features/transactions/components/TransactionRow.tsx`: adaptar a los nuevos campos; pasar `brokerName: null` a `CommissionBreakdown` (sin cambios al componente `CommissionBreakdown`).
- [x] **T-F-3** Añadir `getTransactions(token, params, page, size)` e interfaces `TransactionItem`, `TransactionHistoryResponse` a `features/dashboard/services/tradingService.ts`.
- [x] **T-F-4** Reescribir `features/transactions/pages/TransactionsPage.tsx`: eliminar mock; conectar a `getTransactions()`; filtros server-side; controles de paginación; mostrar resumen `"Comisión total en esta página: $XX.XX"` con `totalCommission`.

## Verificación final

- [x] **T-V-1** Ejecutar `mvn test` en `trading-service` y confirmar que todos los tests pasan. ✅ 47 tests, 0 fallos.
- [ ] **T-V-2** Levantar el frontend y verificar visualmente las 3 páginas: portfolio con datos reales, órdenes con paginación y filtros, transacciones con paginación y resumen de comisión.
