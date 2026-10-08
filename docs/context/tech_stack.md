# tech_stack.md — Stack Tecnológico

## Backend

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 17 |
| Framework | Spring Boot | 3.5.14 |
| Patrón | MVC | — |
| Build | Maven | — |
| Base de datos | PostgreSQL | 15 |
| Caché | Caffeine (spring-boot-starter-cache) | — |
| Seguridad | Spring Security + JJWT | jjwt 0.12.x |
| Email | Spring Mail + Gmail SMTP | — |
| Logs de auditoría | Elasticsearch | 8.11.0+ |
| Broker API | Alpaca Broker API (sandbox) | — |
| Datos de mercado | Alpaca Market Data API | — |
| Pagos | Stripe Java SDK | — |

## Frontend

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | TypeScript | 6 |
| Framework | React | 19.2.6 |
| Build tool | Vite | 8 |
| Routing | React Router DOM | 7.15.1 |
| Estado global | Zustand | 5 |
| Estilos | Tailwind CSS | 3 |
| Formularios | React Hook Form + Zod | — |
| Gráficos | Recharts | — |
| Iconos | lucide-react | — |
| PDF/CSV | jsPDF + Blob API | — |

## Infraestructura

| Herramienta | Propósito |
|---|---|
| Docker + Docker Compose | Contenerización local |
| GitHub Actions | CI/CD |
| SonarCloud | Análisis estático de código |
| Stripe CLI | Pruebas locales de webhooks de Stripe |

---

## Dependencias Maven por servicio

Todas incluyen: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
`postgresql`, `lombok`, `spring-boot-devtools`, `spring-boot-starter-actuator`,
`spring-boot-starter-validation`.

| Servicio | Dependencias adicionales |
|---|---|
| identity-service | `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `spring-boot-starter-cache`, `caffeine`, `spring-boot-starter-mail`, `stripe-java` |
| trading-service | `spring-boot-starter-security` (solo filtro JWT) |
| markets-service | `spring-boot-starter-security` (solo filtro JWT), `spring-boot-starter-cache`, `caffeine` |
| notification-service | `spring-boot-starter-mail` |
| audit-service | `spring-boot-starter-data-elasticsearch` |

## Configuración Spring Initializr (igual para todos los servicios)

- Project: Maven | Language: Java | Spring Boot: 3.5.14
- Packaging: Jar | Java: 17 | Config: YAML

---

## Estructura Maven — Monorepo con POM padre

Para que SonarCloud analice todos los servicios en un solo paso,
`backend/` tiene un POM padre que agrega los 5 módulos.

**`backend/pom.xml`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>co.edu.unbosque</groupId>
  <artifactId>acciones-elbosque</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>Acciones ElBosque — Parent</name>

  <modules>
    <module>identity-service</module>
    <module>trading-service</module>
    <module>markets-service</module>
    <module>notification-service</module>
    <module>audit-service</module>
  </modules>
</project>
```

Cada servicio tiene su propio `pom.xml` normal de Spring Boot.
El CI corre `cd backend && mvn verify sonar:sonar` desde la raíz del monorepo.

---

## CI/CD — GitHub Actions + SonarCloud

El CI analiza el backend completo con SonarCloud en cada push a `main`
y en cada pull request. El frontend no hace parte del CI.

**Configuración SonarCloud:**
- Organización: `ncuellaro`
- Project key: `ncuellaro_aeb-trading`
- El `SONAR_TOKEN` se configura como secret en GitHub.

---

## Docker Compose

`docker-compose.yml` en la raíz del repo levanta:

| Contenedor | Imagen | Puerto |
|---|---|---|
| postgres | postgres:15 | 5432 |
| elasticsearch | elasticsearch:8.11.0 | 9200 |
| identity-service | build local | 8081 |
| trading-service | build local | 8082 |
| markets-service | build local | 8083 |
| notification-service | build local | 8084 |
| audit-service | build local | 8085 |

El frontend no está en Docker Compose — se corre por separado con
`npm run dev` para aprovechar la recarga automática en desarrollo.

Al levantar, PostgreSQL ejecuta automáticamente `db/init.sql`
que crea los schemas y los datos iniciales (acciones y horarios de mercado).

---

## Caché Caffeine

Usado en identity-service (MFA) y markets-service (precios).

| Servicio | Clave de caché | TTL | Contenido |
|---|---|---|---|
| identity-service | `mfa:{email}` | 5 min | Código MFA + intentos |
| identity-service | `session:{sessionId}` | 5 min | Email del usuario durante login |
| markets-service | `stock:{symbol}` | Configurable vía `CACHE_TTL_SECONDS` (default 2 s) | Precio actual del símbolo |

**Nota:** Si identity-service se reinicia, los códigos MFA en caché se pierden.
El frontend interpreta un 404 en la verificación como código expirado
y ofrece reenvío automático.