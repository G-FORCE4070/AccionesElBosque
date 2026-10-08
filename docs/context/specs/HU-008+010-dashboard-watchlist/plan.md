# HU-08+10 — Plan de implementación

## Decisión de sesión

HU-08 (Dashboard de precios) y HU-10 (Watchlist Premium) se implementan juntas porque:
- El `WatchlistPanel` es el punto de integración de ambas HUs
- `markets-service` ya debe construirse para HU-08; agregar `/stocks/{symbol}` para HU-10 es trivial
- `identity.watchlist_items` ya existe en `db/init.sql`

---

## Backend — markets-service (construir desde cero)

### Archivos a modificar
| Archivo | Cambio |
|---|---|
| `pom.xml` | Agregar: `spring-boot-starter-security`, `spring-boot-starter-cache`, `caffeine`, `jjwt-api/impl/jackson` 0.12.6 |
| `src/main/resources/application.yaml` | Config completa: datasource (schema markets), JPA, cache, Alpaca Market Data, JWT, server port 8083 |

### Archivos a crear
| Archivo | Contenido |
|---|---|
| `model/Stock.java` | Entidad JPA para `markets.stocks`: symbol (PK), companyName, marketCode, sector, active |
| `repository/StockRepository.java` | `findByMarketCodeInAndSectorIn`, `findByMarketCodeIn`, `findBySectorIn` |
| `dto/response/StockResponse.java` | symbol, companyName, market, sector, price (Double nullable), change24h (Double nullable) |
| `dto/AlpacaSnapshotDto.java` | DTO interno: `LatestTrade.p` y `PrevDailyBar.c` para precio y variación |
| `service/AlpacaMarketDataService.java` | Interfaz: `AlpacaPriceData getPrice(String symbol)` |
| `model/AlpacaPriceData.java` | Record: price (Double), change24h (Double) |
| `service/impl/AlpacaMarketDataServiceImpl.java` | Llama `GET /v2/stocks/{symbol}/snapshot`; `@Cacheable("stock", key="#symbol")`; null si Alpaca falla |
| `service/StockService.java` | `getStocks(String markets, String sectors)` + `getStock(String symbol)` |
| `service/impl/StockServiceImpl.java` | Parsea CSV, convierte a uppercase, filtra en DB, enriquece con Alpaca |
| `controller/StockController.java` | `GET /api/v1/markets/stocks` + `GET /api/v1/markets/stocks/{symbol}` |
| `config/SecurityConfig.java` | JWT filter; `anyRequest().authenticated()` |
| `config/CorsConfig.java` | `http://localhost:5173`, igual a identity-service |
| `config/CacheConfig.java` | Cache "stock", TTL desde `${cache.ttl-seconds:2}` |
| `security/JwtFilter.java` | Copia de identity-service |
| `util/JwtUtil.java` | Solo `extractUserId(token)` con JJWT 0.12.6 |
| `exception/GlobalExceptionHandler.java` | Handler estándar |
| `src/test/resources/test-init.sql` | `CREATE SCHEMA IF NOT EXISTS markets;` + datos de prueba |
| `test/.../service/StockServiceImplTest.java` | 4 tests: sin filtros, por market, por sector, combinado |
| `test/.../controller/StockControllerTest.java` | 3 tests: sin JWT→401; con JWT→200; con filtros→subset correcto |

---

## Backend — identity-service (extensión para watchlist)

### Archivos a modificar
| Archivo | Cambio |
|---|---|
| `util/JwtUtil.java` | Agregar `extractPlan(token): String` |
| `security/JwtFilter.java` | Agregar plan como authority `PLAN_PREMIUM` / `PLAN_BASIC` en el authentication |
| `config/SecurityConfig.java` | Agregar `@EnableMethodSecurity`; exponer `/api/v1/watchlist/**` como authenticated |

### Archivos a crear
| Archivo | Contenido |
|---|---|
| `model/WatchlistItem.java` | Entidad JPA para `identity.watchlist_items`: `@EmbeddedId` de userId + symbol, addedAt |
| `model/WatchlistItemId.java` | PK compuesta embebida |
| `repository/WatchlistItemRepository.java` | `findByIdUserId`, `existsByIdUserIdAndIdSymbol`, `deleteByIdUserIdAndIdSymbol` |
| `dto/response/WatchlistResponse.java` | `{ symbols: List<String> }` |
| `exception/PremiumRequiredException.java` | 403 |
| `exception/InvalidSymbolException.java` | 400 |
| `exception/WatchlistDuplicateException.java` | 409 |
| `service/WatchlistService.java` | `getWatchlist`, `addSymbol`, `removeSymbol` — todos reciben userId y plan |
| `service/impl/WatchlistServiceImpl.java` | Valida plan (`PREMIUM` o 403), valida símbolo (10 del MVP o 400), maneja duplicado (409) |
| `controller/WatchlistController.java` | `GET /api/v1/watchlist`, `POST /api/v1/watchlist/{symbol}`, `DELETE /api/v1/watchlist/{symbol}` |
| `test/.../service/WatchlistServiceImplTest.java` | 5 tests: add OK; add duplicate→409; remove OK; get list; BASIC→403 |
| `test/.../controller/WatchlistControllerTest.java` | 4 tests: get sin JWT→401; add BASIC→403; add PREMIUM OK→201; delete→204 |

---

## Frontend

### Archivos a crear
| Archivo | Contenido |
|---|---|
| `features/dashboard/services/marketsService.ts` | `getStocks(markets, sectors, token)` + `getStock(symbol, token)` |
| `features/dashboard/services/watchlistService.ts` | `getWatchlist(token)`, `addSymbol(symbol, token)`, `removeSymbol(symbol, token)` |
| `features/dashboard/hooks/useMarketStocks.ts` | Estado loading/error/data, auto-refresh 10s, re-fetch cuando cambian prefs |
| `features/dashboard/hooks/useWatchlist.ts` | Estado de watchlist, add/remove con optimistic update |

### Archivos a modificar
| Archivo | Cambio |
|---|---|
| `WatchlistRow.tsx` | Acepta `price: number \| null`, `changePct: number \| null`; botón `+`/`−` para PREMIUM |
| `WatchlistGroup.tsx` | Tipo genérico, pasa `onAdd`/`onRemove` a cada row |
| `WatchlistPanel.tsx` | Sección "Mi Watchlist": real data + sugerencias vacías. Sección mercado: real data + auto-refresh + error. Label dinámico con market/sector del usuario |

---

## Orden de implementación
1. markets-service: pom → yaml → model/repo/dto → AlpacaService → StockService → Controller → config/security → tests
2. identity-service: JwtUtil/JwtFilter → WatchlistItem/Repo → WatchlistService → Controller → SecurityConfig → tests
3. Frontend: marketsService → watchlistService → useMarketStocks → useWatchlist → WatchlistRow → WatchlistGroup → WatchlistPanel
4. Docs
