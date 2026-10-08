# architecture.md — Acciones ElBosque

## Qué es el proyecto

Plataforma de Day Trading que permite a inversionistas comprar y vender
acciones en mercados internacionales (NYSE, NASDAQ, LSE, TSE, ASX) a
través de una interfaz web React. Se integra con Alpaca Broker API
(sandbox) para ejecución de órdenes y gestión de cuentas virtuales.

Roles: INVESTOR (BASIC/PREMIUM), BROKER, ADMIN, LEGAL.

---

## Estilo arquitectónico: Service-Based

5 servicios coarse-grained organizados por dominio de negocio.
Una sola instancia PostgreSQL con schemas separados por servicio.
Cada servicio solo lee y escribe en su propio schema. Si necesita
datos de otro dominio, los pide por REST.
Sin API Gateway. El frontend accede directamente a cada servicio.

---

## Los 5 servicios

| Servicio | Puerto | Schema | Responsabilidad |
|---|---|---|---|
| identity-service | 8081 | identity | Registro, login, JWT, MFA (email directo vía Spring Mail), perfil, preferencias, watchlist (CRUD), suscripción Premium (Stripe), creación de cuenta Alpaca durante registro |
| trading-service | 8082 | trading | Market Orders compra/venta, validación de fondos contra saldo local, portafolio completo con precios (llama a markets-service internamente), historial de órdenes (PostgreSQL), cálculo de comisiones |
| markets-service | 8083 | markets | Precios en tiempo real desde Alpaca Market Data API, caché Caffeine, listado de acciones, validación de horarios de mercado |
| notification-service | 8084 | notification | Notificaciones de negocio por email (Spring Mail + Gmail SMTP). Consulta preferencias a identity-service antes de cada envío |
| audit-service | 8085 | - (Elasticsearch) | Recibe eventos asíncronos de todos los servicios y los almacena en Elasticsearch. En MVP no expone endpoints al frontend |

---

## Comunicación entre servicios

### Síncrona REST (bloquea al usuario)

| Origen | Destino | Propósito |
|---|---|---|
| trading-service | markets-service | Obtener precio estimado para previsualización de orden |
| trading-service | markets-service | Validar que el mercado esté abierto antes de enviar a Alpaca |
| trading-service | markets-service | Obtener precios actuales para construir el portafolio completo |
| trading-service | identity-service | Consultar si el usuario tiene comisionista asignado al calcular comisión |
| notification-service | identity-service | Consultar preferencias del usuario antes de cada envío |

### Asíncrona con @Async + Spring Retry (no bloquea al usuario, 3 reintentos)

| Origen | Destino | Eventos |
|---|---|---|
| trading-service | notification-service | Confirmación de orden ejecutada (userId + tipo de evento) |
| trading-service | audit-service | ORDER_PLACED, ORDER_EXECUTED, ORDER_REJECTED |
| identity-service | audit-service | USER_REGISTERED, ALPACA_ACCOUNT_CREATED, LOGIN_SUCCESS, LOGIN_FAILED, MFA_CODE_SENT, MFA_FAILED, PROFILE_UPDATED |
| notification-service | audit-service | NOTIFICATION_SENT, NOTIFICATION_FAILED, NOTIFICATION_SENT_DEFAULT_PREFS |

---

## JWT

Generado por identity-service al completar MFA exitosamente.
Validado localmente en cada servicio con JWT_SECRET — ningún servicio
necesita llamar a identity-service para validar el token.

```json
{
  "sub": "identificador-único-del-usuario",
  "role": "INVESTOR | BROKER | ADMIN | LEGAL",
  "plan": "BASIC | PREMIUM",
  "alpacaAccountId": "id-cuenta-alpaca",
  "email": "correo-del-usuario"
}
```

- `sub`: identifica al usuario en todos los servicios
- `role`: controla qué endpoints puede usar
- `plan`: permite rechazar peticiones PREMIUM sin consultar a nadie
- `alpacaAccountId`: trading-service lo usa para ejecutar órdenes en Alpaca
- `email`: trading-service lo propaga al Order para que notification-service sepa a quién enviar el correo sin consultar a identity-service

Expiración: 1 hora. Ante un 401 el frontend redirige al login con
"Tu sesión ha expirado. Por favor, inicia sesión nuevamente."

---

## Portafolio

trading-service construye y devuelve el portafolio completo:
1. Lee holdings del usuario desde trading.holdings (PostgreSQL)
2. Llama síncronamente a markets-service para obtener precios actuales
3. Calcula valor actual, ganancia/pérdida por posición
4. Devuelve todo en una sola respuesta al frontend

El frontend solo consume un endpoint — no hace cálculos.

---

## Watchlist

Datos en identity.watchlist, CRUD en identity-service.
Solo usuarios con plan PREMIUM pueden acceder — identity-service
rechaza con 403 si el claim plan del JWT es BASIC.
Los precios de la watchlist los obtiene el frontend consultando
markets-service directamente.

---

## Historial de transacciones

Se consulta desde trading.orders (PostgreSQL), no desde Elasticsearch.
Elasticsearch es exclusivo para auditoría.

---

## Fallback de notificaciones

Si notification-service no puede consultar preferencias a identity-service,
aplica defaults: email habilitado, orderConfirm activo.
Registra NOTIFICATION_SENT_DEFAULT_PREFS en auditoría.

---

## Integración con servicios externos

| Servicio externo | Usado por | Propósito |
|---|---|---|
| Alpaca Broker API (sandbox) | identity-service | Crear cuenta del usuario durante registro, fondear con $50,000 vía ACH relationship + transfer (sandbox auto-aprueba en ~60s) |
| Alpaca Broker API (sandbox) | trading-service | Ejecutar órdenes usando alpacaAccountId del JWT |
| Alpaca Market Data API | markets-service | Precios en tiempo real |
| Stripe | identity-service | Suscripción Premium ($12/mes o $120/año) |
| Gmail SMTP | identity-service | Envío de código MFA directamente, sin pasar por notification-service |
| Gmail SMTP | notification-service | Notificaciones de negocio (confirmación de órdenes, etc.) |
| Elasticsearch | audit-service | Almacenamiento de todos los eventos de auditoría |