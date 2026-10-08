# data-model.md — Modelo de Datos del MVP

Este documento define las entidades, atributos, relaciones y restricciones de la base de datos PostgreSQL y Elasticsearch para el MVP de Acciones ElBosque.

Sigue las decisiones definidas en `architecture.md` (schemas separados) y los contratos OpenAPI.

> **Nota sobre la creación de tablas**
>
> El script `db/init.sql` crea los schemas y las tablas necesarias (`identity`, `trading`, `markets`) junto con los datos iniciales de `markets`.
>
> De esta forma, PostgreSQL ya tiene la estructura lista cuando Spring Boot inicia, y Hibernate (`ddl-auto=update`) solo valida o agrega columnas nuevas en futuras versiones.

---

# Convenciones generales

- **PK:** `UUID` generado por la aplicación.
- **Timestamps:** `created_at` y `updated_at` con `DEFAULT NOW()`.
- **Enums:** almacenados como `VARCHAR` con valores controlados por la aplicación.
- **Dinero:** `DECIMAL(15,2)`.
- **Precios unitarios:** `DECIMAL(15,4)`.
- **Relaciones:** se usan claves foráneas por `UUID`.
- La lógica de borrado se maneja desde los servicios; no se usan `ON DELETE CASCADE`.

---

# Esquema `identity`

Responsabilidad:
identidad, autenticación, perfil, preferencias, watchlist y suscripciones.

---

## Tabla `users`

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Identificador único |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Credencial de acceso |
| `password_hash` | VARCHAR(255) | NOT NULL | Contraseña cifrada con BCrypt |
| `first_name` | VARCHAR(100) | NOT NULL | Nombre |
| `last_name` | VARCHAR(100) | NOT NULL | Apellido |
| `phone` | VARCHAR(20) | NOT NULL | Teléfono |
| `birth_date` | DATE | NOT NULL | Fecha de nacimiento |
| `doc_type` | VARCHAR(10) | NOT NULL | `cc`, `ce`, `passport` |
| `doc_number` | VARCHAR(50) | NOT NULL | Número de documento |
| `nationality` | VARCHAR(100) | NOT NULL | Nacionalidad |
| `country` | VARCHAR(100) | NOT NULL | País |
| `address` | TEXT | NOT NULL | Dirección |
| `city` | VARCHAR(100) | NOT NULL | Ciudad |
| `postal_code` | VARCHAR(20) | NOT NULL | Código postal |
| `income_source` | VARCHAR(50) | NOT NULL | `salary`, `business`, `investments`, `pension`, `other` |
| `annual_income` | VARCHAR(20) | NOT NULL | `<30k`, `30k-100k`, `100k-500k`, `>500k` |
| `net_worth` | VARCHAR(20) | NOT NULL | `<50k`, `50k-250k`, `>250k` |
| `risk_tolerance` | VARCHAR(20) | NOT NULL | `conservative`, `moderate`, `aggressive` |
| `trading_experience` | VARCHAR(20) | NOT NULL | `beginner`, `experienced` |
| `investment_goal` | VARCHAR(20) | NOT NULL | `preservation`, `income`, `growth`, `speculation` |
| `is_pep` | BOOLEAN | NOT NULL | Persona políticamente expuesta |
| `is_finra_affiliated` | BOOLEAN | NOT NULL | Afiliado a FINRA |
| `accepts_terms` | BOOLEAN | NOT NULL | Aceptación de términos |
| `accepts_privacy` | BOOLEAN | NOT NULL | Aceptación de privacidad |
| `authorize_data_use` | BOOLEAN | NOT NULL | Autorización de uso de datos |
| `role` | VARCHAR(20) | NOT NULL DEFAULT 'INVESTOR' | Rol del usuario |
| `plan` | VARCHAR(10) | NOT NULL DEFAULT 'BASIC' | `BASIC`, `PREMIUM` |
| `alpaca_account_id` | VARCHAR(100) | NULL | ID retornado por Alpaca |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() | Fecha de creación |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() | Fecha de actualización |

### Índices

- `idx_users_email`
- `idx_users_alpaca_account`

---

## Tabla `user_preferences`

Relación 1:1 con `users`.

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `user_id` | UUID | PRIMARY KEY, FK → `users(id)` | Usuario |
| `market_open` | BOOLEAN | DEFAULT false | No usado en MVP |
| `price_alerts` | BOOLEAN | DEFAULT false | No usado |
| `daily_summary` | BOOLEAN | DEFAULT false | No usado |
| `order_confirm` | BOOLEAN | DEFAULT true | Confirmación de órdenes |
| `notif_email` | BOOLEAN | DEFAULT true | Canal email |
| `notif_sms` | BOOLEAN | DEFAULT false | No implementado |
| `notif_whatsapp` | BOOLEAN | DEFAULT false | No implementado |
| `monthly_report` | BOOLEAN | DEFAULT false | No usado |
| `view_type` | VARCHAR(20) | DEFAULT 'list' | `list`, `bars`, `detail` |
| `default_order` | VARCHAR(20) | DEFAULT 'totalValue' | Ordenamiento del portafolio |
| `show_low_weight` | BOOLEAN | DEFAULT true | Mostrar activos pequeños |
| `default_order_type` | VARCHAR(20) | DEFAULT 'market' | Solo `market` en MVP |
| `daily_limit` | INTEGER | DEFAULT 0 | `0 = sin límite` |
| `require_double_auth` | BOOLEAN | DEFAULT false | No usado |
| `markets` | VARCHAR(255) | NULL | Ej: `"nyse,nasdaq"` |
| `sectors` | VARCHAR(255) | NULL | Ej: `"tech,health"` |

---

## Tabla `watchlist_items`

Relación 1:N con `users`.

Clave primaria compuesta:
`(user_id, symbol)`

| Campo | Tipo | Restricciones |
|---|---|---|
| `user_id` | UUID | PRIMARY KEY, FK → `users(id)` |
| `symbol` | VARCHAR(10) | PRIMARY KEY |
| `added_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

---

## Tabla `subscriptions`

Relación 1:N con `users`.

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `user_id` | UUID | NOT NULL, FK → `users(id)` |
| `stripe_subscription_id` | VARCHAR(255) | UNIQUE |
| `stripe_customer_id` | VARCHAR(255) | NOT NULL |
| `plan` | VARCHAR(10) | NOT NULL |
| `billing_cycle` | VARCHAR(10) | NOT NULL (`monthly`, `annual`) |
| `status` | VARCHAR(20) | NOT NULL (`active`, `canceled`, `past_due`) |
| `current_period_end` | TIMESTAMP | NOT NULL |
| `cancel_at_period_end` | BOOLEAN | NOT NULL DEFAULT false |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

---

# Esquema `trading`

---

## Tabla `accounts`

Relación 1:1 con `users`.

| Campo | Tipo | Restricciones |
|---|---|---|
| `user_id` | UUID | PRIMARY KEY |
| `balance` | DECIMAL(15,2) | NOT NULL DEFAULT 0 |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

---

## Tabla `holdings`

Relación 1:N con `users`.

Clave primaria compuesta:
`(user_id, symbol)`

| Campo | Tipo | Restricciones |
|---|---|---|
| `user_id` | UUID | PRIMARY KEY |
| `symbol` | VARCHAR(10) | PRIMARY KEY |
| `quantity` | INTEGER | NOT NULL |
| `avg_cost` | DECIMAL(15,4) | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |

---

## Tabla `orders`

Relación 1:N con `users`.

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | UUID | PRIMARY KEY |
| `user_id` | UUID | NOT NULL |
| `symbol` | VARCHAR(10) | NOT NULL |
| `side` | VARCHAR(4) | NOT NULL (`buy`, `sell`) |
| `order_type` | VARCHAR(10) | NOT NULL (`MARKET`) |
| `quantity` | INTEGER | NOT NULL |
| `estimated_price` | DECIMAL(15,4) | NULL |
| `executed_price` | DECIMAL(15,4) | NULL |
| `commission` | DECIMAL(15,4) | NULL |
| `status` | VARCHAR(20) | NOT NULL (`SENT`, `EXECUTED`, `REJECTED`, `FAILED`) |
| `alpaca_order_id` | VARCHAR(100) | NULL |
| `created_at` | TIMESTAMP | NOT NULL DEFAULT NOW() |
| `executed_at` | TIMESTAMP | NULL |

### Índices

- `idx_orders_user_id_created_at`
- `idx_orders_symbol`
- `idx_orders_status`

---

# Esquema `markets`

---

## Tabla `stocks`

| Campo | Tipo | Restricciones |
|---|---|---|
| `symbol` | VARCHAR(10) | PRIMARY KEY |
| `company_name` | VARCHAR(100) | NOT NULL |
| `market_code` | VARCHAR(10) | NOT NULL (`NYSE`, `NASDAQ`) |
| `sector` | VARCHAR(20) | NOT NULL (`tech`, `health`, `consumer`) |
| `active` | BOOLEAN | NOT NULL DEFAULT true |

---

## Tabla `market_schedules`

| Campo | Tipo | Restricciones |
|---|---|---|
| `market_code` | VARCHAR(10) | PRIMARY KEY |
| `market_name` | VARCHAR(100) | NOT NULL |
| `open_time_utc` | TIME | NOT NULL |
| `close_time_utc` | TIME | NOT NULL |
| `timezone` | VARCHAR(50) | NOT NULL |
| `active` | BOOLEAN | NOT NULL DEFAULT true |

---

# Esquema `notification`

Vacío en el MVP.

---

# Elasticsearch — `audit-service`

Índice:
`audit-events`

## Mapeo

```json
{
  "mappings": {
    "properties": {
      "timestamp": { "type": "date" },
      "service": { "type": "keyword" },
      "eventType": { "type": "keyword" },
      "userId": { "type": "keyword" },
      "correlationId": { "type": "keyword" },
      "details": {
        "type": "object",
        "enabled": true
      }
    }
  }
}
```

Eventos permitidos en el MVP:
ver `audit-service.yaml`.

---

# Notas para implementación con Hibernate

- Cada servicio usa:
  `spring.jpa.properties.hibernate.default_schema`

- `ddl-auto=update` valida las tablas existentes y puede agregar columnas nuevas.

- Usar:
  - `@CreationTimestamp`
  - `@UpdateTimestamp`

- Los enums se manejan con:
  `@Enumerated(EnumType.STRING)`

- Para `markets` y `sectors` en `user_preferences`, se puede usar:
  - un convertidor JPA
  - o manejar la conversión directamente en el servicio