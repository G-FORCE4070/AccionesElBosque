# HU-08+10 — Tasks

## markets-service: Setup

- [ ] T-01: Modificar `pom.xml` — agregar `spring-boot-starter-security`, `spring-boot-starter-cache`, `caffeine`, `jjwt-api/impl/jackson` 0.12.6
- [ ] T-02: Modificar `src/main/resources/application.yaml` — config completa: datasource (schema markets), JPA update, cache, Alpaca Market Data credentials, JWT secret, port 8083

## markets-service: Modelo y DTOs

- [ ] T-03: Crear `model/Stock.java` — entidad JPA: symbol (PK), companyName, marketCode, sector, active
- [ ] T-04: Crear `repository/StockRepository.java` — `findByMarketCodeInAndSectorIn`, `findByMarketCodeIn`, `findBySectorIn`
- [ ] T-05: Crear `dto/response/StockResponse.java` — symbol, companyName, market, sector, price (Double nullable), change24h (Double nullable)
- [ ] T-06: Crear `dto/AlpacaSnapshotDto.java` — clases anidadas: `LatestTrade { double p }` y `PrevDailyBar { double c }`
- [ ] T-07: Crear `model/AlpacaPriceData.java` — record con price y change24h (ambos Double nullable)

## markets-service: Servicios

- [ ] T-08: Crear `service/AlpacaMarketDataService.java` + `service/impl/AlpacaMarketDataServiceImpl.java` — RestTemplate llama `GET /v2/stocks/{symbol}/snapshot`; `@Cacheable("stock", key="#symbol")`; retorna null si placeholder o falla
- [ ] T-09: Crear `service/StockService.java` + `service/impl/StockServiceImpl.java` — parsea CSV, uppercase para markets, filtra en DB, enriquece con precios; `getStock(symbol)` para el endpoint individual

## markets-service: Controller y configuración

- [ ] T-10: Crear `controller/StockController.java` — `GET /api/v1/markets/stocks` (params opcionales markets, sectors) + `GET /api/v1/markets/stocks/{symbol}`
- [ ] T-11: Crear `util/JwtUtil.java` — solo `extractUserId(token)` con JJWT 0.12.6
- [ ] T-12: Crear `security/JwtFilter.java` — copia de identity-service
- [ ] T-13: Crear `config/SecurityConfig.java` — JWT filter; `anyRequest().authenticated()`
- [ ] T-14: Crear `config/CorsConfig.java` — origin `http://localhost:5173`
- [ ] T-15: Crear `config/CacheConfig.java` — `@EnableCaching`; cache "stock" con TTL desde `${cache.ttl-seconds:2}`
- [ ] T-16: Crear `exception/GlobalExceptionHandler.java`

## markets-service: Tests

- [ ] T-17: Crear `src/test/resources/test-init.sql` — schema markets + INSERT de stocks de prueba
- [ ] T-18: Crear `test/.../service/StockServiceImplTest.java` — 4 tests: sin filtros devuelve todos; filtro market; filtro sector; filtro combinado
- [ ] T-19: Crear `test/.../controller/StockControllerTest.java` — 3 tests: sin JWT→401; con JWT→200; con market filter→subset

## identity-service: Extensión JWT para plan

- [ ] T-20: Modificar `util/JwtUtil.java` — agregar `extractPlan(token): String`
- [ ] T-21: Modificar `security/JwtFilter.java` — agregar `new SimpleGrantedAuthority("PLAN_" + plan)` al authentication

## identity-service: Watchlist

- [ ] T-22: Crear `model/WatchlistItemId.java` + `model/WatchlistItem.java` — PK compuesta (userId, symbol); campo addedAt
- [ ] T-23: Crear `repository/WatchlistItemRepository.java` — `findByIdUserId`, `existsByIdUserIdAndIdSymbol`, `deleteByIdUserIdAndIdSymbol`
- [ ] T-24: Crear `dto/response/WatchlistResponse.java` — `{ symbols: List<String> }`
- [ ] T-25: Crear `exception/PremiumRequiredException.java` (403) + `InvalidSymbolException.java` (400) + `WatchlistDuplicateException.java` (409); registrar en `GlobalExceptionHandler.java`
- [ ] T-26: Crear `service/WatchlistService.java` + `service/impl/WatchlistServiceImpl.java` — valida plan (PREMIUM o 403), valida símbolo (lista MVP o 400), maneja duplicado (409)
- [ ] T-27: Crear `controller/WatchlistController.java` — `GET /watchlist`, `POST /watchlist/{symbol}`, `DELETE /watchlist/{symbol}`; lee userId + plan del SecurityContext

## identity-service: Tests watchlist

- [ ] T-28: Crear `test/.../service/WatchlistServiceImplTest.java` — 5 tests: add OK; add duplicate→409; remove OK; get list; BASIC→403
- [ ] T-29: Crear `test/.../controller/WatchlistControllerTest.java` — 4 tests: sin JWT→401; BASIC→403; PREMIUM add→201; delete→204

## Frontend

- [ ] T-30: Crear `features/dashboard/services/marketsService.ts` — `getStocks(markets, sectors, token): Promise<StockData[]>` + `getStock(symbol, token): Promise<StockData>`
- [ ] T-31: Crear `features/dashboard/services/watchlistService.ts` — `getWatchlist(token)`, `addSymbol(symbol, token)`, `removeSymbol(symbol, token)`
- [ ] T-32: Crear `features/dashboard/hooks/useMarketStocks.ts` — estado loading/error/data; auto-refresh 10s con cleanup; re-fetch cuando cambian market/sector del profileStore
- [ ] T-33: Crear `features/dashboard/hooks/useWatchlist.ts` — estado symbols/loading; `add` y `remove` con optimistic update; solo activo si PREMIUM
- [ ] T-34: Modificar `WatchlistRow.tsx` — `price: number | null`; `changePct: number | null`; botón `+`/`−` opcional para PREMIUM; mostrar "—" cuando sin precio
- [ ] T-35: Modificar `WatchlistGroup.tsx` — tipo genérico; props `onAdd?` / `onRemove?` opcionales
- [ ] T-36: Modificar `WatchlistPanel.tsx` — sección "Mi Watchlist": datos reales, precios por símbolo, sugerencias cuando vacía; sección mercado: datos reales + auto-refresh + error + label dinámico; botones add en mercado (solo PREMIUM)

## Documentación

- [ ] T-37: Actualizar `STATUS.md`
- [ ] T-38: Actualizar `docs/context/changelog.md`
