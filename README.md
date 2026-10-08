# README.md — Acciones ElBosque

Plataforma de Day Trading universitaria — Universidad El Bosque, Colombia.

Permite a inversionistas comprar y vender acciones en mercados internacionales
(NYSE, NASDAQ) a través de una interfaz web React, integrada con Alpaca Broker API
(sandbox) para ejecución de órdenes con dinero virtual.

---

## Requisitos

- Java 17
- Maven 3.9+
- Node.js 20+
- Docker y Docker Compose
- Credenciales de Alpaca Broker API y Market Data API (sandbox)
- Cuenta en [Stripe](https://stripe.com/) (modo test)
- Gmail con [contraseña de aplicación](https://myaccount.google.com/apppasswords) habilitada

> Elasticsearch corre automáticamente vía Docker — no requiere cuenta ni instalación adicional.

---

## Configuración inicial

1. Clona el repositorio:
```bash
   git clone https://github.com/ncuellaro/aeb-trading.git
   cd aeb-trading
```

2. Crea el archivo de variables de entorno:
```bash
   cp .env.example .env
```
   Completa los valores en `.env` con tus credenciales reales.

3. Levanta la infraestructura y los servicios:
```bash
   docker-compose up --build
```

   Esto levanta: PostgreSQL, Elasticsearch y los 5 servicios backend.
   Los schemas de la base de datos se crean automáticamente desde `db/init.sql`.

4. Inicia el frontend:
```bash
   cd frontend
   npm install
   npm run dev
```

---

## Servicios

| Servicio | Puerto | Descripción |
|---|---|---|
| identity-service | 8081 | Registro, login, MFA, perfil, suscripción |
| trading-service | 8082 | Órdenes, portafolio, historial |
| markets-service | 8083 | Precios en tiempo real, horarios de mercado |
| notification-service | 8084 | Notificaciones por email |
| audit-service | 8085 | Trazabilidad en Elasticsearch |
| frontend | 5173 | Interfaz React (corre por separado con `npm run dev`) |

---

## Estado del proyecto

Ver [STATUS.md](STATUS.md) para el estado actual de las historias de usuario.

---

## Documentación técnica

Toda la documentación está en `docs/context/`:

- `architecture.md` — Arquitectura Service-Based y comunicación entre servicios
- `domain.md` — Glosario y reglas de negocio
- `decisions.md` — Decisiones arquitectónicas (ADR)
- `tech_stack.md` — Stack tecnológico y configuración
- `coding_style.md` — Convenciones de código
- `technical_debt.md` — Deuda técnica documentada
- `data-model.md` — Entidades
- `contracts/` — Contratos OpenAPI de cada servicio

---

## Arquitectura

Service-Based Architecture con 5 servicios y base de datos PostgreSQL
compartida con schemas separados. Sin API Gateway — el frontend accede
directamente a cada servicio.

Ver `docs/context/architecture.md` para el detalle completo.