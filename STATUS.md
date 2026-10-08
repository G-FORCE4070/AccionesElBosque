# STATUS.md — Estado del Proyecto

## HUs completadas
| HU | Historia | Servicios | Commit |
|----|----------|-----------|--------|
| HU-01 | Registro de inversionista | identity-service, audit-service (frontend: registro wizard) | `531063d` |
| HU-02+03+04 | Login, MFA y Cierre de sesión | identity-service (frontend: auth flow) | `9eae436` |
| HU-06 | Actualización de perfil y preferencias | identity-service (frontend: profile page) | `1026aca` |
| HU-07 | Suscripción Premium | identity-service (frontend: subscription flow) | `aaf53ec` |
| HU-08+10 | Dashboard de precios + Watchlist Premium | markets-service, identity-service (frontend: WatchlistPanel, hooks, services) | `a4f00ab` |
| HU-011+012 | Market Orders (compra/venta) + portafolio básico | trading-service (nuevo), markets-service | `c73a1f4` |
| HU-013 | Encolar órdenes cuando el mercado está cerrado | trading-service (scheduler, QueuedOrderService, AlpacaClockClient) | `e5434ec` |
| HU-014+015+016 | Portafolio, historial de órdenes, historial de transacciones | trading-service (portfolio, transactions), frontend conectado | `cef0302` |
| HU-021 | Notificaciones de órdenes ejecutadas | identity-service, trading-service, notification-service (nuevo) | `0ab8833` |

## Fixes post-HU-021 (en rama actual)
- Sonar: `catch (Exception)` → tipos específicos en `NotificationServiceImpl`
- Sonar: removido `@Retryable` de método privado
- Sonar: complejidad cognitiva `PortfolioPage.tsx` reducida (comparador extraído)
- Session persistence: Zustand `persist` en `sessionStore.ts` (localStorage)
- Sidebar: botón Reportes deshabilitado
- OrderConfirmModal: aviso de cambio de precio para órdenes inmediatas
- Cancel orden en cola: endpoint `DELETE /api/v1/orders/{orderId}` + botón en UI
- Email HTML: plantilla HTML con diseño que coincide con la paleta de la app
- **MarketStatusBadge**: corregido DST — ahora usa `Intl.DateTimeFormat('America/New_York')` (resuelve desfase en verano EDT)
- **AlpacaClockServiceImpl / AlpacaClockClient**: fallback a verificación horaria ET real cuando las llaves no están disponibles
- **OrderServiceImpl**: corregido NPE en `filled.getId()` (causaba 500 en compra/venta cuando Alpaca no tenía fondos); fallback a precio estimado cuando Alpaca rechaza con 403
- **Funding Alpaca**: cambiado de JNLC journal a ACH relationship + transfer (no requiere fondos en la cuenta firma); `waitForAccountActive` antes de fondear
- **`POST /api/v1/users/me/alpaca/fund`**: endpoint para re-fondear cuentas existentes manualmente
- **CommissionBreakdown**: simplificado — muestra solo total + % + "ElBosque" (eliminado el split 40%/60% falso)
- **Emails bienvenida y MFA**: actualizados a HTML con tema oscuro (`MimeMessage`)

## HU en progreso
— ninguna —

## Próxima HU
— ninguna pendiente de MVP. Las HU-11 y HU-12 fueron implementadas en commits `c73a1f4` y `e5434ec`. —

## Deuda técnica conocida
- **`@Retryable` en `sendEmail`**: Spring AOP no intercepta métodos privados. El retry no funciona en producción. Fix: extraer `sendEmail` a un `@Component` separado con método público.
- **`CANCELLED` en BD**: el enum `OrderStatus` incluye `CANCELLED` pero la BD pre-existente no tiene esa fila en ningún `CHECK` constraint (Hibernate añade la columna via `update` — compatible).
- **Fondeo ACH en sandbox**: la relación ACH se aprueba en ~60s y el transfer liquida durante el día. Existe una ventana donde el usuario puede operar (precio estimado) antes de que Alpaca confirme los fondos.

## MVP pendiente
| HU | Historia | Puntos |
|----|----------|--------|
| HU-11 | Market Order de compra | 5 |
| HU-12 | Market Order de venta | 3 |
