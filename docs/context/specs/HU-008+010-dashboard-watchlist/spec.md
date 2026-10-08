# HU-08+10 — Dashboard de precios y Watchlist Premium

## Decisión de sesión

HU-08 (Dashboard de precios) y HU-10 (Watchlist Premium) se implementan juntas porque comparten el mismo componente de entrada (`WatchlistPanel`) y porque construir `markets-service` ya incluye el endpoint individual `/stocks/{symbol}` que necesita la Watchlist. Combinarlas elimina una segunda ronda de cambios al mismo componente.

---

## Historias de usuario

### HU-08 — Dashboard de precios según preferencias

**Como** inversionista,
**quiero** ver en mi dashboard los precios actuales de las acciones de los mercados y sectores que seleccioné en mis preferencias,
**para** monitorear el mercado.

### HU-10 — Watchlist personalizada (Premium)

**Como** inversionista premium,
**quiero** crear y gestionar una lista de seguimiento con mis acciones favoritas,
**para** monitorearlas rápidamente desde mi dashboard.

---

## Prioridad y estimación

| Campo      | Valor                                                |
|------------|------------------------------------------------------|
| Prioridad  | Must                                                 |
| Estimación | 10 puntos (5 + 5)                                    |
| Épica      | Dashboard y Mercados                                 |
| Feature    | Dashboard de precios + Watchlist Premium             |
| Servicios  | markets-service + identity-service + frontend        |

---

## Referencias

- `docs/context/architecture.md` — markets-service (puerto 8083), caché Caffeine TTL configurable; Watchlist: datos en `identity.watchlist_items`, CRUD en identity-service
- `docs/context/domain.md` — acciones MVP: AAPL, MSFT, GOOGL, AMZN, META, TSLA, NVDA (NASDAQ); JNJ, JPM, XOM (NYSE); plan BASIC/PREMIUM
- `docs/context/tech_stack.md` — caché Caffeine `stock:{symbol}` TTL `CACHE_TTL_SECONDS` (default 2s)
- `docs/context/contracts/markets-service.yaml` — `GET /api/v1/markets/stocks`, `GET /api/v1/markets/stocks/{symbol}`
- `docs/context/contracts/identity-service.yaml` — `GET /api/v1/watchlist`, `POST /api/v1/watchlist/{symbol}`, `DELETE /api/v1/watchlist/{symbol}`

---

## Criterios de aceptación — Dashboard (HU-08)

### CA-01: Dashboard con preferencias configuradas

**Dado que** el inversionista tiene preferencias de `markets` y `sectors` configuradas
**Cuando** accede al dashboard
**Entonces** el frontend llama a `GET /api/v1/markets/stocks?markets={markets}&sectors={sectors}`
pasando los valores del `profileStore` (cargados en el login desde `GET /api/v1/users/me`)
**Y** el dashboard muestra una lista con las acciones filtradas
**Y** cada ítem muestra: símbolo, nombre de empresa, precio actual y variación en 24h (`change24h`)

---

### CA-02: Dashboard sin preferencias configuradas

**Dado que** el inversionista no tiene preferencias configuradas (o `markets`/`sectors` están vacíos)
**Cuando** accede al dashboard
**Entonces** el frontend llama a `GET /api/v1/markets/stocks` sin parámetros
**Y** el dashboard muestra todas las acciones disponibles (NYSE y NASDAQ, todos los sectores)

---

### CA-03: Actualización periódica de precios

**Dado que** el dashboard está visible
**Cuando** transcurren 10 segundos desde la última consulta
**Entonces** el frontend vuelve a llamar al endpoint de precios automáticamente
**Y** los precios se actualizan en pantalla sin recargar la página

---

### CA-04: Error del servicio de precios

**Dado que** markets-service no está disponible o responde con 5xx
**Cuando** el frontend intenta obtener los precios
**Entonces** el dashboard muestra el mensaje:
```
"Datos de mercado no disponibles temporalmente"
```
**Y** no muestra precios desactualizados ni datos vacíos sin contexto
**Y** el usuario puede hacer clic en "Reintentar" para volver a consultar

---

### CA-05: Actualización inmediata tras cambio de preferencias

**Dado que** el inversionista modifica sus preferencias de mercados/sectores en la pantalla de perfil (HU-06)
**Cuando** guarda los cambios
**Entonces** el dashboard aplica el nuevo filtro inmediatamente al volver a él
(el frontend re-consulta con los nuevos parámetros obtenidos del `profileStore`)

---

## Criterios de aceptación — Watchlist (HU-10)

### CA-06: Agregar símbolo a la watchlist

**Dado que** el inversionista tiene plan PREMIUM y un JWT válido
**Cuando** llama a `POST /api/v1/watchlist/{symbol}` (ej: `/watchlist/AAPL`)
**Entonces** el sistema responde con HTTP 201
**Y** el símbolo es persistido en `identity.watchlist_items` para ese usuario
**Y** el símbolo aparece en la sección watchlist del dashboard con su precio actual

**Dado que** el símbolo ya está en la watchlist
**Cuando** intenta agregarlo nuevamente
**Entonces** el sistema responde con HTTP 409 y cuerpo `{ "error": "El símbolo ya está en tu watchlist" }`

---

### CA-07: Quitar símbolo de la watchlist

**Dado que** el inversionista tiene plan PREMIUM y el símbolo está en su watchlist
**Cuando** llama a `DELETE /api/v1/watchlist/{symbol}`
**Entonces** el sistema responde con HTTP 204
**Y** el símbolo es eliminado de `identity.watchlist_items`
**Y** desaparece de la sección watchlist del dashboard

---

### CA-08: Consultar watchlist

**Dado que** el inversionista tiene plan PREMIUM y un JWT válido
**Cuando** llama a `GET /api/v1/watchlist`
**Entonces** el sistema responde con HTTP 200 y cuerpo:
```json
{ "symbols": ["AAPL", "MSFT", "NVDA"] }
```

---

### CA-09: Persistencia de la watchlist

**Dado que** el inversionista tiene símbolos en su watchlist
**Cuando** cierra sesión y vuelve a iniciarla
**Entonces** la watchlist se mantiene con los mismos símbolos (datos en BD, no en localStorage)

---

### CA-10: Sugerencias cuando la watchlist está vacía

**Dado que** el inversionista PREMIUM no tiene símbolos en su watchlist
**Cuando** accede a la sección watchlist del dashboard
**Entonces** el frontend muestra sugerencias predefinidas: `AAPL, MSFT, NVDA, GOOGL, TSLA, META, AMZN, JPM, JNJ`
**Y** el usuario puede hacer clic en cualquiera para agregarlo directamente

---

### CA-11: Acceso denegado para usuario BASIC

**Dado que** el inversionista tiene plan BASIC
**Cuando** llama a cualquier endpoint de watchlist (`GET`, `POST`, `DELETE`)
**Entonces** el sistema responde con HTTP 403 y cuerpo:
```json
{ "error": "Funcionalidad exclusiva para usuarios Premium" }
```
**Y** el frontend muestra este mensaje al usuario e invita a suscribirse

---

## Definition of Done

### markets-service
- [ ] Endpoint `GET /api/v1/markets/stocks` acepta `markets` y `sectors` como query params (comma-separated); sin parámetros devuelve todas las acciones del MVP
- [ ] Endpoint `GET /api/v1/markets/stocks/{symbol}` devuelve precio y change24h para un símbolo individual
- [ ] Precios obtenidos de Alpaca Market Data API con caché Caffeine (`stock:{symbol}`, TTL = `CACHE_TTL_SECONDS`)
- [ ] Si Alpaca falla para un símbolo, devuelve `price: null` y `change24h: null` sin fallar todo el listado
- [ ] Tests: lista completa, filtro por markets, filtro por sectors, filtro combinado; sin JWT → 401; con JWT → 200

### identity-service
- [ ] Endpoints `GET /api/v1/watchlist`, `POST /api/v1/watchlist/{symbol}`, `DELETE /api/v1/watchlist/{symbol}` implementados
- [ ] Control de acceso vía claim `plan` del JWT: BASIC → 403 en cualquier endpoint de watchlist
- [ ] `POST` valida que el símbolo pertenezca a los 10 del MVP; retorna 400 si inválido
- [ ] `POST` maneja duplicado con HTTP 409
- [ ] `JwtFilter` agrega authority `PLAN_PREMIUM` / `PLAN_BASIC` al `SecurityContext`
- [ ] Tests: agregar símbolo OK, duplicado → 409, quitar símbolo OK, obtener lista, BASIC → 403; integración: sin JWT → 401, BASIC → 403, PREMIUM add → 201, delete → 204

### Frontend
- [ ] Dashboard consume `GET /api/v1/markets/stocks` con las preferencias del `profileStore`
- [ ] Actualización automática cada 10 segundos con cleanup en `useEffect`
- [ ] Re-fetch inmediato cuando cambian preferencias de markets/sector
- [ ] Estado de error con mensaje "Datos de mercado no disponibles temporalmente" y botón "Reintentar"
- [ ] `change24h` mostrado en verde (positivo) o rojo (negativo); `null` muestra `—`
- [ ] Sección "Mi Watchlist" separada de la lista de mercado (solo visible para PREMIUM)
- [ ] Precios de la watchlist obtenidos de `GET /api/v1/markets/stocks/{symbol}` en paralelo
- [ ] Botones `+` / `−` en la lista de mercado (solo PREMIUM) con optimistic update
- [ ] Sugerencias mostradas cuando la watchlist está vacía
- [ ] El código compila, type-check pasa, `mvn test` sin errores

---

## Notas técnicas

- La verificación de plan se hace leyendo el claim `plan` del JWT en el `JwtFilter` de identity-service, sin consultar la BD. Consistente con ADR-04.
- Los precios de la watchlist los obtiene el frontend directamente de markets-service (`/stocks/{symbol}`), no a través de identity-service. Esto evita acoplar identity-service con datos de mercado.
- El caché Caffeine en markets-service tiene TTL de 2 segundos (default). Cada request de precio individual comparte el mismo caché que los listados; no hay doble llamada a Alpaca.
- Las acciones del MVP son 10 (AAPL, MSFT, GOOGL, AMZN, META, TSLA, NVDA, JNJ, JPM, XOM). La tabla `markets.stocks` las tiene precargadas en `db/init.sql`.
- `change24h` = `(latestTrade.p - prevDailyBar.c) / prevDailyBar.c * 100`. Campo `latestTrade.p` y `prevDailyBar.c` del snapshot de Alpaca.
- No hay límite de símbolos en la watchlist en el MVP (deuda técnica futura).
- El `JwtUtil` de markets-service solo necesita `extractUserId(token)`, no `extractPlan` (la autorización por plan no aplica ahí; cualquier token válido puede consultar precios).

---

## Aclaraciones de sesión

- La combinación HU-08+HU-10 fue acordada en sesión porque ambas tocan `WatchlistPanel` y porque el endpoint `/stocks/{symbol}` para la watchlist es trivial dado que markets-service ya se construye.
- El cancel de suscripción (HU-07 post-implementación) también se corrigió en esta sesión: al cancelar, se fuerza logout y redirect a `/login` con mensaje de éxito, ya que el JWT todavía contiene `plan: PREMIUM` hasta que el usuario vuelva a iniciar sesión.
