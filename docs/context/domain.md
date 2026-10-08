# domain.md — Dominio y Glosario

## Glosario de términos de negocio

| Término (ES) | Término en código (EN) | Definición |
|---|---|---|
| Orden | Order | Instrucción de compra o venta de una acción. Tiene tipo, estado y registro completo de cambios. |
| Orden de mercado | Market Order | Compra o venta al mejor precio disponible en ese momento. La ejecución suele ser rápida, pero el precio final puede variar ligeramente. |
| Orden límite | Limit Order | Compra o venta solo si el precio alcanza un valor específico o mejor. Solo en sprints Should. |
| Orden stop loss | Stop Loss Order | Venta automática cuando el precio baja hasta un límite definido para reducir pérdidas. Solo en sprints Should. |
| Orden take profit | Take Profit Order | Venta automática cuando el precio alcanza una meta de ganancia. Solo en sprints Should. |
| Cotización | Quote | Información actual de precio de una acción. Incluye precio de compra (bid) y venta (ask). |
| Holding | Holding | Cantidad de acciones que un inversionista posee dentro de su portafolio. |
| Portafolio | Portfolio | Conjunto de activos financieros que posee un inversionista y su valor actual. |
| Posición | Position | Información detallada de una inversión: símbolo, cantidad, precio promedio, precio actual, valor total y ganancia/pérdida. |
| Saldo | Balance | Dinero disponible para operar dentro de la plataforma. Se mantiene localmente en `trading.accounts`. |
| Comisión | Commission | Cobro realizado por ElBosque en cada transacción. Corresponde al 2% del valor total de la operación. |
| Spread | Spread | Diferencia entre el precio de compra (bid) y venta (ask) de una acción. |
| Ticker / Símbolo | Symbol | Código único que identifica una acción en el mercado (ej: AAPL, MSFT). |
| Inversionista | Investor | Usuario con rol INVESTOR que puede comprar y vender acciones. Puede ser BASIC o PREMIUM. |
| Comisionista | Broker | Usuario con rol BROKER encargado de asesorar inversionistas y gestionar órdenes. No implementado en el MVP. |
| Plan Basic | BASIC | Plan gratuito con acceso a trading, dashboard y portafolio. |
| Plan Premium | PREMIUM | Plan de pago ($12/mes o $120/año) que incluye watchlist y alertas de precio. |
| Watchlist | Watchlist | Lista personalizada de acciones favoritas para seguimiento. Disponible solo para usuarios PREMIUM. |
| MFA | MFA | Verificación adicional mediante un código de 6 dígitos enviado por email. Obligatorio para todos los usuarios. |
| Mercado abierto | Market open | Estado en el que la bolsa permite ejecutar órdenes de compra y venta. |
| Capitalización de mercado | Market cap | Valor total de todas las acciones de una empresa en circulación. |
| Volumen | Volume | Cantidad de acciones negociadas en un período determinado. |
| Trazabilidad | Traceability | Registro de todos los cambios de estado de una orden con fecha, hora y responsable. |

---


## Acciones disponibles en el MVP

### NASDAQ (9:30–16:00 ET / 14:30–21:00 UTC)

| Símbolo | Empresa | Sector |
|---|---|---|
| AAPL | Apple Inc. | tech |
| MSFT | Microsoft Corp. | tech |
| GOOGL | Alphabet Inc. | tech |
| AMZN | Amazon.com Inc. | consumer |
| META | Meta Platforms Inc. | tech |
| TSLA | Tesla Inc. | tech |
| NVDA | NVIDIA Corp. | tech |

### NYSE (9:30–16:00 ET / 14:30–21:00 UTC)

| Símbolo | Empresa | Sector |
|---|---|---|
| JNJ | Johnson & Johnson | health |
| JPM | JPMorgan Chase & Co. | consumer |
| XOM | Exxon Mobil Corp. | consumer |

---

## Mercados no disponibles en el MVP

| Mercado | Horario local | Horario UTC |
|---|---|---|
| LSE (Londres) | 8:00–16:30 GMT | 8:00–16:30 UTC |
| TSE (Tokio) | 9:00–15:00 JST | 0:00–6:00 UTC |
| ASX (Sídney) | 10:00–16:00 AEDT | 23:00–5:00 UTC (día anterior) |

Los horarios se almacenan en UTC porque el servidor trabaja usando esa
zona horaria. Esto permite validar fácilmente si un mercado está abierto
sin hacer conversiones adicionales de horario.

---

## Estados de una orden

| Estado | EN (código) | Descripción | MVP |
|---|---|---|---|
| Pendiente | PENDING | Orden creada pero aún no enviada al broker | Should |
| Enviada | SENT | Orden enviada a Alpaca | Sí |
| Firmada por Comisionista | BROKER_SIGNED | Revisada y aprobada por el comisionista | Should |
| En Ejecución | EXECUTING | Alpaca está procesando la orden | Should |
| Ejecutada | EXECUTED | Alpaca confirmó la ejecución y se actualizó el saldo | Sí |
| Cancelada | CANCELLED | Cancelada antes de ejecutarse | Should |
| Rechazada | REJECTED | Alpaca rechazó la orden | Sí |
| Expirada | EXPIRED | La orden no se ejecutó dentro del tiempo esperado | Should |
| En Revisión | UNDER_REVIEW | Pausada para revisión manual | Should |
| Fallida | FAILED | Error técnico durante el procesamiento | Sí |
| Detenida | STOPPED | Suspendida administrativamente | Should |

### Flujo principal de estados en el MVP

```text
SENT → EXECUTING → EXECUTED
   │          │
   │          └──→ REJECTED
   │
   └──→ FAILED
```

Cada cambio de estado queda registrado en auditoría con fecha, hora y responsable.

---

## Roles del sistema

| Rol | EN (código) | Descripción | MVP |
|---|---|---|---|
| Inversionista | INVESTOR | Compra y vende acciones | Sí |
| Comisionista | BROKER | Asesora clientes y firma órdenes | No |
| Administrador | ADMIN | Gestiona configuración general del sistema | No |
| Responsable Legal | LEGAL | Supervisa auditorías y cumplimiento | No |

---

## Eventos de auditoría

Todos los eventos se almacenan en Elasticsearch usando `audit-service`.

| Evento | Servicio origen |
|---|---|
| USER_REGISTERED | identity-service |
| ALPACA_ACCOUNT_CREATED | identity-service |
| LOGIN_SUCCESS | identity-service |
| LOGIN_FAILED | identity-service |
| MFA_CODE_SENT | identity-service |
| MFA_FAILED | identity-service |
| PROFILE_UPDATED | identity-service |
| SUBSCRIPTION_CREATED | identity-service |
| SUBSCRIPTION_CANCELED | identity-service |
| PLAN_CHANGED | identity-service |
| ORDER_PLACED | trading-service |
| ORDER_EXECUTED | trading-service |
| ORDER_REJECTED | trading-service |
| NOTIFICATION_SENT | notification-service |
| NOTIFICATION_FAILED | notification-service |
| NOTIFICATION_SENT_DEFAULT_PREFS | notification-service |

---

## Sectores disponibles

| Código | Nombre |
|---|---|
| tech | Tecnología |
| health | Salud |
| consumer | Consumo |

---

## Tipos de documentos de identidad

| Código | Descripción |
|---|---|
| cc | Cédula de ciudadanía |
| ce | Cédula de extranjería |
| passport | Pasaporte |