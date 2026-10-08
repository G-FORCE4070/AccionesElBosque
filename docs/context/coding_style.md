# coding_style.md — Convenciones de Código

## Principios

Todo el código sigue **DRY, KISS, YAGNI y SOLID**:

- **DRY:** No duplicar lógica. Extraer a métodos o clases reutilizables.
- **KISS:** La solución más simple que funcione. Sin complejidad innecesaria.
- **YAGNI:** Solo implementar lo que el spec pide. Nada extra.
- **SOLID:** Una responsabilidad por clase, depender de abstracciones,
  interfaces específicas, abierto a extensión pero cerrado a modificación.

---

## Idioma del código

| Contexto | Idioma |
|---|---|
| Clases, métodos, variables, constantes | Inglés |
| Endpoints REST | Inglés |
| Comentarios en código | Inglés |
| Mensajes de error al usuario final | Español |
| Eventos de auditoría | Inglés SCREAMING_SNAKE_CASE |

---

## Package base

Spring Boot convierte los guiones de los nombres de servicio a underscores
al generar los packages. El package base de cada servicio es:

```text
co.edu.unbosque.acciones_elbosque.{service}
```

| Servicio | Package base |
|---|---|
| identity-service | `co.edu.unbosque.acciones_elbosque.identity_service` |
| trading-service | `co.edu.unbosque.acciones_elbosque.trading_service` |
| markets-service | `co.edu.unbosque.acciones_elbosque.markets_service` |
| notification-service | `co.edu.unbosque.acciones_elbosque.notification_service` |
| audit-service | `co.edu.unbosque.acciones_elbosque.audit_service` |

---

## Estructura de paquetes MVC (igual para todos los servicios)

```text
co.edu.unbosque.acciones_elbosque.{service}/
├── controller/       ← @RestController, maneja HTTP, delega al service
├── service/          ← Interfaz del servicio (abstracción)
│   └── impl/         ← Implementación del servicio (@Service)
├── repository/       ← @Repository, extiende JpaRepository
├── model/            ← Entidades JPA (@Entity)
├── dto/              ← Clases de transferencia
│   ├── request/      ← DTOs de entrada (sufijo Request)
│   └── response/     ← DTOs de salida (sufijo Response)
├── config/           ← Beans de configuración (@Configuration)
│   ├── CorsConfig
│   ├── SecurityConfig
│   └── AsyncConfig   ← Solo en servicios que usan @Async
├── security/         ← Solo en servicios que validan JWT
│   ├── JwtFilter
│   └── JwtUtil
└── exception/        ← Excepciones personalizadas + handler global
    ├── GlobalExceptionHandler  ← @RestControllerAdvice
    └── (excepciones específicas del dominio)
```

---

## Convenciones de naming

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clases | PascalCase | `OrderController`, `UserService` |
| Interfaces de service | PascalCase sin sufijo | `UserService` |
| Implementaciones | PascalCase + `Impl` | `UserServiceImpl` |
| Métodos | camelCase, verbo + sustantivo | `createOrder`, `findByEmail` |
| Variables | camelCase | `totalCost`, `newBalance` |
| Constantes | SCREAMING_SNAKE_CASE | `MAX_MFA_ATTEMPTS` |
| Entidades JPA | PascalCase singular | `Order`, `UserAccount` |
| Tablas en BD | snake_case plural | `orders`, `user_accounts` |
| DTOs | PascalCase + `Request` o `Response` | `RegisterRequest`, `LoginResponse` |
| Endpoints | kebab-case plural en inglés | `/api/v1/orders`, `/api/v1/users/me` |
| Variables de entorno | SCREAMING_SNAKE_CASE | `JWT_SECRET`, `COMMISSION_PERCENTAGE` |

---

## Endpoints REST

Todos los endpoints siguen este patrón:

```text
/api/v1/{recurso}
```

El `v1` indica la versión de la API. Si en el futuro se necesita cambiar
la forma en que funciona un endpoint sin romper lo que ya existe,
se crea `/api/v2/...` y el `v1` sigue funcionando.

### Endpoints internos

Llamados entre servicios, sin JWT:

```text
/api/v1/internal/{recurso}
```

---

## Formato de errores

Siempre:

```json
{ "error": "Mensaje en español" }
```

Con el código HTTP apropiado.

### Ejemplos

```json
{ "error": "El correo electrónico ya está registrado" }
{ "error": "Fondos insuficientes" }
{ "error": "El mercado está cerrado" }
{ "error": "Código inválido" }
{ "error": "Funcionalidad exclusiva para usuarios Premium" }
{ "error": "Tu sesión ha expirado. Por favor, inicia sesión nuevamente." }
```

Implementado en:

```text
GlobalExceptionHandler
```

Con:

```java
@RestControllerAdvice
```

---

## DTOs

- Los controllers solo reciben y devuelven DTOs, nunca entidades JPA.
- Las entidades JPA nunca salen del `service/impl/` hacia afuera.
- Validaciones con anotaciones de Jakarta Validation en los `*Request`:
  - `@NotBlank`
  - `@Email`
  - `@Size`
  - `@Min`
  - `@NotNull`
- `@Valid` en los parámetros del controller para activar validación.

---

## CORS

Cada servicio tiene `CorsConfig` permitiendo:

```text
http://localhost:5173
```

### Implementación

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

## @Async

Solo en los servicios que envían eventos asíncronos
(`@EnableAsync` en `AsyncConfig`).

Todos los métodos `@Async` tienen Spring Retry con:

- 3 intentos
- 1 segundo de espera entre intentos

### Ejemplo

```java
@Async
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void sendOrderConfirmation(String userId, String eventType) {
    ...
}
```

---

## Seguridad JWT en cada servicio

- `JwtFilter` intercepta cada petición y valida el token con `JWT_SECRET`.
- Los endpoints internos (`/api/v1/internal/**`) se excluyen del filtro JWT.
- Los endpoints públicos (`login`, `register`, `MFA`, webhook de Stripe)
  se excluyen del filtro JWT.
- El filtro valida el JWT y guarda la información
  del usuario autenticado para que el controller la use.

---

## Convenciones de tests

- Tests unitarios en `src/test/java/` con JUnit 5 + Mockito.
- Un test class por clase de producción que lo necesite.
- Naming: `{Clase}Test`
  - Ejemplo: `OrderServiceTest`
- Al menos un test por cada método de servicio
  que tenga lógica de negocio.
- Tests de integración para los controllers con:
  - `@SpringBootTest`
  - `MockMvc`