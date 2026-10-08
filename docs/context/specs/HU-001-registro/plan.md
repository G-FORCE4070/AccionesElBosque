# Plan — HU-01: Registro de inversionista

## Orden de ejecución

Los archivos se crean/modifican en el orden indicado para que cada capa
dependa solo de lo que ya existe.

---

## 1. Contrato OpenAPI

### `docs/context/contracts/identity-service.yaml`
**Modificar** — agregar `billingCycle` como campo requerido condicionalmente
en `RegisterRequest` (requerido cuando `plan = premium`).

Tests: ninguno (es documentación).

---

## 2. Backend — identity-service

### 2.1 `backend/identity-service/pom.xml`
**Modificar** — agregar dependencias faltantes documentadas en `tech_stack.md`:
- `spring-boot-starter-security`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x)
- `spring-boot-starter-cache` + `caffeine`
- `spring-boot-starter-mail`
- `stripe-java`
- `spring-retry` + `spring-aspects` (para `@Retryable`)
- `spring-boot-starter-test` ya existe; agregar `testcontainers` (postgresql)

Tests: ninguno.

---

### 2.2 `backend/identity-service/src/main/resources/application.yaml`
**Modificar** — configuración completa:
- datasource → PostgreSQL (schema `identity`)
- jpa: `ddl-auto=update`, `default_schema=identity`
- security: `JWT_SECRET`, `JWT_EXPIRATION`
- mail: Gmail SMTP con variables de entorno
- stripe: `STRIPE_SECRET_KEY`, `STRIPE_PRICE_ID_MONTHLY`, `STRIPE_PRICE_ID_ANNUAL`
- alpaca: `ALPACA_BROKER_API_KEY`, `ALPACA_BROKER_SECRET_KEY`, `ALPACA_BROKER_BASE_URL`
- cache: Caffeine spec
- async: habilitado

Tests: ninguno.

---

### 2.3 Modelo JPA — `model/`

#### `model/User.java`
**Crear** — entidad JPA mapeada a `identity.users`.
Campos según `data-model.md`. Usa `@CreationTimestamp`, `@UpdateTimestamp`,
`@Enumerated(EnumType.STRING)` para `role` y `plan`.

#### `model/UserPreference.java`
**Crear** — entidad JPA mapeada a `identity.user_preferences`.
Relación `@OneToOne` con `User`. PK = `userId`.
Campos `markets` y `sectors` como `String` (CSV); conversión en servicio.

Tests: ninguno (entidades puras sin lógica).

---

### 2.4 Repositorios — `repository/`

#### `repository/UserRepository.java`
**Crear** — extiende `JpaRepository<User, UUID>`.
Métodos: `existsByEmail(String)`, `findByEmail(String)`.

#### `repository/UserPreferenceRepository.java`
**Crear** — extiende `JpaRepository<UserPreference, UUID>`.

Tests: ninguno (interfaces JPA).

---

### 2.5 DTOs — `dto/`

#### `dto/request/RegisterRequest.java`
**Crear** — todos los campos del contrato con anotaciones Jakarta Validation:
- `@NotBlank`, `@Email` para email
- `@Size(min=8)` + `@Pattern` para password (mayúscula, número, símbolo)
- `@NotBlank`, `@Size(min=2)` para firstName/lastName
- `@NotBlank`, `@Size(min=7)` para phone
- `@NotNull` para fechas, booleanos
- `@NotNull` + validación de enum para incomeSource, annualIncome, netWorth,
  riskTolerance, tradingExperience, investmentGoal, docType, plan
- `@NotEmpty` para markets, sectors
- `billingCycle`: opcional, requerido solo cuando `plan = premium`
  (validado en el servicio, no con anotación)

#### `dto/response/RegisterResponse.java`
**Crear** — `message: String`, `checkoutUrl: String` (nullable).

Tests: ninguno.

---

### 2.6 Excepciones — `exception/`

#### `exception/EmailAlreadyRegisteredException.java`
**Crear** — extiende `RuntimeException`.

#### `exception/AlpacaAccountCreationException.java`
**Crear** — extiende `RuntimeException`.

#### `exception/GlobalExceptionHandler.java`
**Crear** — `@RestControllerAdvice`:
- `EmailAlreadyRegisteredException` → 409 `{ "error": "El correo electrónico ya está registrado" }`
- `AlpacaAccountCreationException` → 500 `{ "error": "No se pudo completar el registro. Intente nuevamente." }`
- `MethodArgumentNotValidException` → 400 con primer mensaje de validación

Tests: ninguno (testeado indirectamente en tests de integración).

---

### 2.7 Clientes HTTP externos — `config/` + `service/`

#### `config/AlpacaClientConfig.java`
**Crear** — `@Configuration` que produce un `RestClient` preconfigurado para Alpaca Broker API.
Usa OAuth2 Client Credentials: inyecta `AlpacaTokenService` como interceptor para agregar
`Authorization: Bearer {token}` en cada request. El token se obtiene de
`https://authx.sandbox.alpaca.markets/v1/oauth2/token` con `grant_type=client_credentials`.

#### `service/AlpacaTokenService.java`
**Crear** — `@Service` que obtiene y cachea el Bearer token de Alpaca (TTL 15 min).
Hace warm-up en `@PostConstruct`. Se refresca automáticamente 60 segundos antes de expirar.

#### `config/StripeConfig.java`
**Crear** — `@Configuration` que inicializa `Stripe.apiKey` desde
`STRIPE_SECRET_KEY` en `@PostConstruct`.

---

### 2.8 Servicio de auditoría asíncrono — `service/`

#### `service/AuditService.java` (interfaz)
**Crear** — método `sendEvent(String eventType, String userId, Map<String,Object> details)`.

#### `service/impl/AuditServiceImpl.java`
**Crear** — `@Async` + `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000))`.
Llama a `audit-service` en `POST /api/v1/internal/events`.
Si falla, loguea error pero no propaga la excepción.

Tests: ninguno en esta HU (se prueba en HU de auditoría).

---

### 2.9 Servicio de registro — `service/`

#### `service/RegisterService.java` (interfaz)
**Crear** — método `register(RegisterRequest request): RegisterResponse`.

#### `service/impl/RegisterServiceImpl.java`
**Crear** — `@Service`, `@Transactional`. Flujo:

1. Verificar `existsByEmail` → lanzar `EmailAlreadyRegisteredException` si existe.
2. Hashear contraseña con `BCryptPasswordEncoder`.
3. Construir y guardar entidad `User` (plan = BASIC siempre).
4. Construir y guardar entidad `UserPreference` con defaults + markets/sectors del request.
5. Llamar a Alpaca Broker API: `POST /v1/accounts` → obtener `alpacaAccountId`.
   Si falla → lanzar `AlpacaAccountCreationException` (rollback automático).
   Nota: campos de texto normalizados a ASCII con `Normalizer.NFD`; country convertido
   de alpha-2 a alpha-3; `tax_id`/`tax_id_type` (COL_CC) incluidos.
6. Llamar a Alpaca Broker API: `POST /v1/journals` con `entry_type: JNLC`,
   `amount: 50000`, de cuenta sweep del broker a `alpacaAccountId`.
   **No-fatal**: envuelto en try/catch — si falla (cuenta en estado SUBMITTED/PENDING),
   se loguea warning y el flujo continúa sin rollback (ver deuda técnica #11).
7. Actualizar `user.alpacaAccountId` y guardar.
8. Si plan == PREMIUM: crear Stripe Checkout Session con `price_id`
   según `billingCycle`. Retornar `checkoutUrl`.
9. Enviar correo de bienvenida (síncrono, Spring Mail). Si falla: loguear, no rollback.
10. Enviar eventos `USER_REGISTERED` y `ALPACA_ACCOUNT_CREATED` vía `AuditServiceImpl`
    (`@Async`, no bloquea).
11. Retornar `RegisterResponse(message, checkoutUrl)`.

**Tests unitarios** (`RegisterServiceImplTest.java`):
- `register_basicPlan_success`: usuario BASIC creado, checkoutUrl null.
- `register_premiumPlan_success`: checkoutUrl no null.
- `register_duplicateEmail_throws409`: `EmailAlreadyRegisteredException`.
- `register_alpacaFailure_rollback`: `AlpacaAccountCreationException`, sin usuario en BD.

---

### 2.10 Configuraciones Spring — `config/`

#### `config/AsyncConfig.java`
**Crear** — `@EnableAsync`, `@EnableRetry`, define `ThreadPoolTaskExecutor`.

#### `config/SecurityConfig.java`
**Crear** — `@EnableWebSecurity`. Permitir sin JWT:
`/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/mfa/**`,
`/api/v1/subscriptions/webhook`. Resto requiere autenticación (se completa en HU-02).

#### `config/CorsConfig.java`
**Crear** — `WebMvcConfigurer` permitiendo `http://localhost:5173`.

#### `config/PasswordConfig.java`
**Crear** — `@Bean BCryptPasswordEncoder`.

---

### 2.11 Controlador — `controller/`

#### `controller/AuthController.java`
**Crear** — `@RestController`, `@RequestMapping("/api/v1/auth")`.
- `POST /register` → HTTP 201.
- `GET /check-email?email=...` → `{ "available": boolean }`. Permite verificación temprana en Step 1.

**Tests de integración** (`AuthControllerTest.java`):
- `POST /api/v1/auth/register` con plan BASIC → 201, checkoutUrl null.
- `POST /api/v1/auth/register` con plan PREMIUM + billingCycle → 201, checkoutUrl presente.
- `POST /api/v1/auth/register` con email duplicado → 409.
- `POST /api/v1/auth/register` con Alpaca mockeado fallando → 500.
- `POST /api/v1/auth/register` con campos inválidos → 400.

Usa `@SpringBootTest`, `MockMvc`, Mockito para Alpaca/Stripe/Mail,
Testcontainers para PostgreSQL.

---

## 3. Frontend — identity-service

### 3.1 `docs/context/contracts/identity-service.yaml` *(ya indicado arriba)*

### 3.2 `frontend/src/features/register/schemas/registerSchema.ts`
**Modificar**:
- `step5Schema`: eliminar `broker` del tipo (o marcarlo como `z.string().optional()` y excluirlo del payload).
- `step6Schema`: agregar `billingCycle: z.enum(["monthly", "annual"]).optional()` con
  refinement que lo requiera cuando `plan === "premium"`.

### 3.3 `frontend/src/features/register/components/steps/Step5Preferences.tsx`
**Modificar**:
- Deshabilitar opciones LSE, TSE, ASX en el `ChipGroup` de mercados
  (atributo `disabled` por valor).
- Selector de broker: mostrar solo "Sin comisionista asignado" deshabilitado.

### 3.4 `frontend/src/features/register/components/steps/Step6Plan.tsx`
**Modificar**:
- Cuando `selectedPlan === "premium"`, mostrar dos botones de radio:
  "Mensual ($12/mes)" y "Anual ($120/año)" para `billingCycle`.
- Validar que `billingCycle` esté seleccionado antes de habilitar "Completar registro".
- En `onSubmit`: si `checkoutUrl` es no null → `window.location.href = checkoutUrl`.
  Si null → `navigate("/login", { state: { successMessage: "..." } })`.

### 3.5 `frontend/src/features/register/store/registerStore.ts`
**Modificar** (si es necesario):
- El tipo `Step6Data` ya incluirá `billingCycle` tras el cambio del schema.
  No se necesitan cambios en el store si el tipo inferido se actualiza solo.

### 3.6 `frontend/src/features/register/services/registerService.ts`
**Modificar** — reemplazar el mock por una llamada real:
`POST http://localhost:8081/api/v1/auth/register` con el payload completo
construido desde todos los pasos del store.
- Mapear los campos de todos los steps al `RegisterRequest` del contrato.
- Excluir `confirmPassword` y `broker`.
- Retornar `RegisterResponse { message, checkoutUrl }`.
- Lanzar error con `error.message` del body del 409/500 para que Step6 lo muestre.

Tests: ninguno en frontend (las validaciones están en los schemas Zod).

---

## Resumen de archivos

### Backend — nuevos
| Archivo | Acción |
|---|---|
| `pom.xml` | Modificar |
| `application.yaml` | Modificar |
| `model/User.java` | Crear |
| `model/UserPreference.java` | Crear |
| `repository/UserRepository.java` | Crear |
| `repository/UserPreferenceRepository.java` | Crear |
| `dto/request/RegisterRequest.java` | Crear |
| `dto/response/RegisterResponse.java` | Crear |
| `exception/EmailAlreadyRegisteredException.java` | Crear |
| `exception/AlpacaAccountCreationException.java` | Crear |
| `exception/GlobalExceptionHandler.java` | Crear |
| `config/AlpacaClientConfig.java` | Crear |
| `config/StripeConfig.java` | Crear |
| `config/AsyncConfig.java` | Crear |
| `config/SecurityConfig.java` | Crear |
| `config/CorsConfig.java` | Crear |
| `config/PasswordConfig.java` | Crear |
| `service/AuditService.java` | Crear |
| `service/impl/AuditServiceImpl.java` | Crear |
| `service/RegisterService.java` | Crear |
| `service/impl/RegisterServiceImpl.java` | Crear |
| `controller/AuthController.java` | Crear |

### Backend — tests
| Archivo | Tipo |
|---|---|
| `RegisterServiceImplTest.java` | Unitario |
| `AuthControllerTest.java` | Integración |

### Frontend — modificados
| Archivo | Acción |
|---|---|
| `registerSchema.ts` | Modificar |
| `Step5Preferences.tsx` | Modificar |
| `Step6Plan.tsx` | Modificar |
| `registerService.ts` | Modificar |

### Contrato
| Archivo | Acción |
|---|---|
| `docs/context/contracts/identity-service.yaml` | Modificar |
