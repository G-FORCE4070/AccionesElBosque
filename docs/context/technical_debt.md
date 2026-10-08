# technical_debt.md — Deuda Técnica

Decisiones tomadas en el MVP por simplicidad que deberían revisarse
en producción. **No son errores** — son compromisos conocidos y documentados.

| # | Decisión en MVP | En producción | ADR relacionado |
|---|---|---|---|
| 1 | Comunicación asíncrona con `@Async` + Spring Retry (3 intentos, 1 segundo de espera entre cada uno) | Kafka o RabbitMQ para mayor confiabilidad y garantías de entrega | ADR-05 |
| 2 | Una sola instancia PostgreSQL con schemas separados | Una instancia PostgreSQL por servicio para que cada uno pueda crecer de forma independiente | ADR-02 |
| 3 | JWT expira en 1 hora, se pide nuevo login si expira | Mecanismo de renovación automática del token para no interrumpir la sesión del usuario | ADR-04 |
| 4 | Saldo local sin verificación contra Alpaca | Verificación periódica contra la cuenta de Alpaca para corregir posibles diferencias entre ambos saldos | ADR-08 |
| 5 | Solo email para notificaciones y MFA | SMS y WhatsApp como canales adicionales | — |
| 6 | Solo NYSE y NASDAQ (Alpaca solo opera en EE.UU.) | LSE, TSE, ASX con otro proveedor de datos internacionales | — |
| 7 | Rol BROKER no implementado | Roles BROKER completos: panel, propuestas, firma de órdenes, comisión compartida | — |
| 8 | audit-service solo almacena eventos, sin consultas al frontend | Módulo de consulta de logs para el responsable legal y el administrador | ADR-09 |
| 9 | Las órdenes fuera de horario se rechazan con mensaje | Encolamiento automático para procesar la orden cuando el mercado abra | — |
| 10 | Los endpoints internos entre servicios no requieren autenticación | Mecanismo de autenticación entre servicios para entornos de producción | ADR-09 |
| 11 | Fondeo ACH en sandbox: relación ACH se aprueba en ~60s y el transfer liquida durante el día; hay una ventana donde las órdenes usan precio estimado en vez del fill real de Alpaca | En producción el fondeo sería inmediato (wire/ACH mismo día) y no existiría esta ventana | — |
| 12 | Normalización ASCII de campos de texto antes de enviar a Alpaca (tildes eliminadas: "Bogotá" → "Bogota") | Validación y feedback al usuario sobre caracteres no permitidos antes de enviar | — |
| 13 | `@Retryable` en método privado `sendEmail` de `NotificationServiceImpl` — Spring AOP no intercepta métodos privados (requiere llamada a través del proxy), por lo que el retry de 3 intentos nunca se activa en producción | Extraer `sendEmail` a un `@Component` separado (`EmailSenderComponent`) con método público anotado con `@Retryable` | ADR-05 |
| 14 | ~~`MarketStatusBadge` usaba UTC fijo~~ — **RESUELTO**: ahora usa `Intl.DateTimeFormat('America/New_York')` con DST automático | — | — |