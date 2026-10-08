# Tasks — HU-11+12: Market Orders de compra y venta

---

## Bloque 0 — markets-service: endpoint de status de mercado

- [x] **T-0-1** Crear `model/MarketSchedule.java` — entidad JPA mapeada a `markets.market_schedules`
- [x] **T-0-2** Crear `repository/MarketScheduleRepository.java`
- [x] **T-0-3** Crear `dto/response/MarketStatusResponse.java`
- [x] **T-0-4** Modificar `service/StockService.java` — agregar firma `getMarketStatus(String symbol)`
- [x] **T-0-5** Modificar `service/impl/StockServiceImpl.java` — implementar `getMarketStatus` usando `AlpacaClockService` (reemplazó el enfoque original basado en DB+Clock)
- [x] **T-0-6** Modificar `controller/StockController.java` — `GET /markets/status/{symbol}`
- [x] **T-0-7** Modificar `test/resources/test-init.sql` — tabla `market_schedules` sin datos (horario lo provee Alpaca)
- [x] **T-0-8** Modificar `test/.../controller/StockControllerTest.java` — 3 tests: mercado abierto, cerrado, símbolo no encontrado (via `@MockitoBean AlpacaClockService`)

---

## Bloque A — trading-service: infraestructura

- [x] **T-A-1** Modificar `pom.xml` — security, JWT, retry, AOP, testcontainers
- [x] **T-A-2** Crear `src/main/resources/application.yaml`
- [x] **T-A-3** Crear `util/JwtUtil.java`
- [x] **T-A-4** Crear `security/JwtFilter.java`
- [x] **T-A-5** Crear `config/SecurityConfig.java`
- [x] **T-A-6** Crear `config/CorsConfig.java`
- [x] **T-A-7** Crear `service/AlpacaTokenService.java`
- [x] **T-A-8** Crear `config/AlpacaBrokerConfig.java`
- [x] **T-A-9** Modificar `TradingServiceApplication.java` — `@EnableAsync @EnableRetry`

---

## Bloque B — trading-service: dominio

- [x] **T-B-1** Crear `model/OrderStatus.java`
- [x] **T-B-2** Crear `model/Account.java`
- [x] **T-B-3** Crear `model/HoldingId.java`
- [x] **T-B-4** Crear `model/Holding.java`
- [x] **T-B-5** Crear `model/Order.java`
- [x] **T-B-6** Crear `repository/AccountRepository.java`
- [x] **T-B-7** Crear `repository/HoldingRepository.java`
- [x] **T-B-8** Crear `repository/OrderRepository.java`
- [x] **T-B-9** Crear `dto/request/PlaceOrderRequest.java`
- [x] **T-B-10** Crear `dto/response/OrderResponse.java`
- [x] **T-B-11** Crear `dto/response/PortfolioResponse.java` y `HoldingItem.java`
- [x] **T-B-12** Crear `dto/AlpacaOrderRequest.java`
- [x] **T-B-13** Crear `dto/AlpacaOrderResponse.java`
- [x] **T-B-14** Crear `dto/MarketStatusDto.java` y `dto/StockPriceDto.java`
- [x] **T-B-15** Crear `dto/NotificationRequest.java` y `dto/AuditEventRequest.java`
- [x] **T-B-16** Crear las 4 excepciones de dominio
- [x] **T-B-17** Crear `exception/GlobalExceptionHandler.java`

---

## Bloque C — trading-service: lógica

- [x] **T-C-1** Crear `client/MarketsServiceClient.java`
- [x] **T-C-2** Crear `client/AlpacaBrokerClient.java`
- [x] **T-C-3** Crear `client/AuditClient.java` — `@Async @Retryable`
- [x] **T-C-4** Crear `client/NotificationClient.java` — `@Async @Retryable`
- [x] **T-C-5** Crear `service/OrderService.java`
- [x] **T-C-6** Crear `service/impl/OrderServiceImpl.java` con métodos privados extraídos para Sonar
- [x] **T-C-7** Crear `service/PortfolioService.java` y `PortfolioServiceImpl.java`
- [x] **T-C-8** Crear `controller/OrderController.java`
- [x] **T-C-9** Crear `controller/PortfolioController.java`

---

## Bloque D — trading-service: tests

- [x] **T-D-1** Crear `test/resources/application.yaml`
- [x] **T-D-2** Crear `test/resources/test-init.sql`
- [x] **T-D-3** Modificar `TradingServiceApplicationTests.java` — Testcontainers + `@MockitoBean AlpacaTokenService`
- [x] **T-D-4** Crear `OrderServiceImplTest.java` — 8 tests unitarios
- [x] **T-D-5** Crear `OrderControllerTest.java` — 6 tests de integración

---

## Bloque E — frontend

- [x] **T-E-1** Crear `features/dashboard/services/tradingService.ts`
- [x] **T-E-2** Crear `features/dashboard/store/tradingStore.ts`
- [x] **T-E-3** Modificar `AppShell.tsx` — `loadPortfolio` al montar
- [x] **T-E-4** Modificar `BalancePill.tsx` — lee de `tradingStore`
- [x] **T-E-5** Modificar `useOrderDraft.ts` — acepta `currentPrice`, elimina `mockStocks`
- [x] **T-E-6** Modificar `OrderSummary.tsx` — sin desglose de broker
- [x] **T-E-7** Modificar `NewOrderPanel.tsx` — todos los stocks sin filtros
- [x] **T-E-8** Modificar `OrderConfirmModal.tsx` — llamada real, estado error, balance actualizado

---

## Verificación final

- [x] **T-V-1** `mvn test` markets-service — 11/11 ✓
- [x] **T-V-2** `mvn test` trading-service — 15/15 ✓
- [x] **T-V-3** UI conectada — balance real, stocks reales, precio real, modal con API real
- [x] **T-V-4** `changelog.md` y `STATUS.md` actualizados
