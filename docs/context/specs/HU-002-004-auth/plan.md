# Plan — HU-02+03+04: Login, MFA y Cierre de sesión

## Contexto

El frontend tiene toda la UI lista (LoginCard, MfaCard, OtpInput, Sidebar) con mocks.
El backend tiene infraestructura base (BCrypt, JavaMailSender, AuditService, AsyncConfig).
Solo hay que conectar las dos capas e implementar el núcleo de autenticación.

Los archivos se crean en el orden indicado para que cada capa dependa solo de
lo que ya existe.

---

## Backend — identity-service

### 1. `application.yaml` — MODIFICAR

Agregar bajo `spring:`:
```yaml
spring:
  cache:
    type: caffeine
```

Tests: ninguno.

---

### 2. `config/CacheConfig.java` — CREAR

`@Configuration @EnableCaching` con `CaffeineCacheManager`.
Dos caches nombrados con TTL 5 minutos:
- `sessions` → almacena `sessionId (String) : email (String)`
- `mfaCodes` → almacena `sessionId (String) : MfaEntry`

Los servicios acceden al caché con `cacheManager.getCache("sessions")` directamente
(no `@Cacheable`) para tener control manual de put/evict.

Tests: ninguno.

---

### 3. `model/MfaEntry.java` — CREAR

POJO serializable (no entidad JPA). Campos:
`String code`, `int attempts`, `int resends`.
Constructor, getters y setters (sin Lombok para evitar dependencia en dominio puro).

Tests: ninguno.

---

### 4. DTOs — CREAR

#### `dto/request/LoginRequest.java`
`email` (`@NotBlank @Email`), `password` (`@NotBlank`).

#### `dto/response/LoginResponse.java`
`String sessionId`.

#### `dto/request/MfaVerifyRequest.java`
`sessionId` (`@NotBlank`), `code` (`@NotBlank @Size(min=6, max=6)`).

#### `dto/request/MfaResendRequest.java`
`sessionId` (`@NotBlank`).

#### `dto/response/MfaVerifyResponse.java`
`String token`, `UserInfo user`.
`UserInfo` como record interno: `String id, firstName, lastName, email, role, plan`.

Tests: ninguno.

---

### 5. Excepciones — CREAR y actualizar `GlobalExceptionHandler`

#### Nuevas excepciones
- `InvalidCredentialsException` extiende `RuntimeException`
- `InvalidMfaCodeException` extiende `RuntimeException`
- `MfaSessionNotFoundException` extiende `RuntimeException`
- `MfaResendLimitException` extiende `RuntimeException`

#### `exception/GlobalExceptionHandler.java` — MODIFICAR
Agregar 4 handlers:
- `InvalidCredentialsException` → 401 `"Email o contraseña inválidos"`
- `InvalidMfaCodeException` → 400 `"Código inválido"`
- `MfaSessionNotFoundException` → 404 `"El código ha expirado"`
- `MfaResendLimitException` → 400 `"Máximo de reenvíos alcanzado"`

Tests: ninguno (cubierto en tests de integración).

---

### 6. `util/JwtUtil.java` — CREAR

`@Component`. Usa JJWT 0.12.x.
- `generateToken(User user): String` — firma HS256 con `${jwt.secret}` (decodificado Base64),
  claims: `sub`=userId, `role`, `plan`, `alpacaAccountId`, exp=`${jwt.expiration}` ms.
- `SecretKey` construida con `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`.

Tests: ninguno directo (cubierto en `MfaServiceImplTest`).

---

### 7. `service/LoginService.java` + `impl/LoginServiceImpl.java` — CREAR

Flujo de `login(LoginRequest)`:
1. `userRepository.findByEmail(email)` → si vacío: audit `LOGIN_FAILED` + `InvalidCredentialsException`
2. `BCrypt.matches(password, user.passwordHash)` → si false: audit `LOGIN_FAILED` + `InvalidCredentialsException`
3. Generar `sessionId` y código MFA de 6 dígitos con `SecureRandom`
4. Enviar email con Spring Mail (síncrono) — si lanza excepción: propagarla (→ 500)
5. Solo si el email se envió OK: `cache("sessions").put(sessionId, email)`
   y `cache("mfaCodes").put(sessionId, new MfaEntry(code, 0, 0))`
6. Audit `MFA_CODE_SENT` vía `@Async`
7. Retornar `new LoginResponse(sessionId)`

**Tests unitarios** (`LoginServiceImplTest.java`):
- `login_success`: sessionId retornado, email enviado, caches populados
- `login_emailNotFound`: lanza `InvalidCredentialsException`, audita `LOGIN_FAILED`, no envía email
- `login_wrongPassword`: lanza `InvalidCredentialsException`, audita `LOGIN_FAILED`, no envía email

---

### 8. `service/MfaService.java` + `impl/MfaServiceImpl.java` — CREAR

**`verify(MfaVerifyRequest)`:**
1. `cache("mfaCodes").get(sessionId)` → si null: `MfaSessionNotFoundException`
2. Si `entry.code != code`:
   - `entry.attempts++`
   - Si `attempts >= 3`: evict `mfaCodes[sessionId]` y `sessions[sessionId]` + audit `MFA_FAILED` + `MfaSessionNotFoundException`
   - Si `attempts < 3`: actualizar cache + audit `MFA_FAILED` + `InvalidMfaCodeException`
3. Si correcto: buscar email en `cache("sessions")`, cargar user desde BD, generar JWT con `JwtUtil`
4. Evict ambos caches, audit `LOGIN_SUCCESS` vía `@Async`
5. Retornar `MfaVerifyResponse(token, UserInfo.from(user))`

**`resend(MfaResendRequest)`:**
1. `cache("mfaCodes").get(sessionId)` → si null: `MfaSessionNotFoundException`
2. Si `entry.resends >= 3`: `MfaResendLimitException`
3. Generar nuevo código, reiniciar `attempts=0`, incrementar `resends`, actualizar cache
4. Enviar email (síncrono)
5. Audit `MFA_CODE_SENT` vía `@Async`

**Tests unitarios** (`MfaServiceImplTest.java`):
- `verify_success`: JWT no nulo, caches evicteados, LOGIN_SUCCESS auditado
- `verify_wrongCode_firstAttempt`: `InvalidMfaCodeException`, attempts=1, cache vigente
- `verify_wrongCode_thirdAttempt`: `MfaSessionNotFoundException`, cache evicteado
- `verify_expiredSession`: `MfaSessionNotFoundException` (cache null desde el inicio)
- `resend_success`: nuevo código en cache, email enviado, resends=1
- `resend_limitReached`: `MfaResendLimitException`

---

### 9. `controller/AuthController.java` — MODIFICAR

Agregar tres endpoints (los tres bajo `/api/v1/auth/**`, ya en `permitAll()`):
- `POST /login` → `loginService.login(request)` → HTTP 200
- `POST /mfa/verify` → `mfaService.verify(request)` → HTTP 200
- `POST /mfa/resend` → `mfaService.resend(request)` → HTTP 200

Tests: integración abajo.

---

### 10. Tests de integración — CREAR

`LoginMfaControllerTest.java` (archivo nuevo, mismo patrón que `AuthControllerTest`):
Usa `@SpringBootTest`, `MockMvc`, `@MockitoBean` para `JavaMailSender` y `AuditService`,
Testcontainers PostgreSQL con `test-init.sql`.

Casos:
- `POST /login` credenciales correctas → 200, sessionId no nulo
- `POST /login` credenciales incorrectas → 401
- `POST /login` campos vacíos → 400
- `POST /mfa/verify` código correcto → 200, token no nulo
- `POST /mfa/verify` código incorrecto → 400
- `POST /mfa/verify` tercer intento → 400 + siguiente call retorna 404
- `POST /mfa/resend` → 200

---

## Frontend

### 11. `src/lib/apiFetch.ts` — CREAR

Wrapper sobre `fetch` nativo que:
- Agrega `Authorization: Bearer {token}` desde `sessionStore` si existe
- Si la respuesta es 401: llama a `sessionStore.clearSession()`, limpia `localStorage`,
  navega a `/login` con `state: { successMessage: "Tu sesión ha expirado..." }` y lanza error
- En otros errores: lanza con el mensaje del body

Tests: ninguno.

---

### 12. `src/store/sessionStore.ts` — MODIFICAR

- Actualizar tipo `User`: `{ id, firstName, lastName, email, role, plan }`
- `setSession(token, user)`: guarda en Zustand y en `localStorage` (`aeb_session`)
- `clearSession()`: limpia Zustand y `localStorage.removeItem("aeb_session")`
- Inicialización: leer `localStorage.getItem("aeb_session")` al crear el store y
  precargar `token` y `user` si existe

Tests: ninguno.

---

### 13. `src/features/auth/services/authService.ts` — REEMPLAZAR

- `login(email, password)` → `fetch POST /api/v1/auth/login` → retorna `{ sessionId }`
- `verifyMfa(sessionId, code)` → `fetch POST /api/v1/auth/mfa/verify` → retorna `{ token, user }`
- `resendMfa(sessionId)` → `fetch POST /api/v1/auth/mfa/resend` → retorna void
- Errores no-ok: leer `body.error` y lanzar `Error(body.error)`

Tests: ninguno (cubierto en tests de integración backend).

---

### 14. `src/features/auth/components/LoginCard.tsx` — MODIFICAR

- `authService.login()` ahora retorna `{ sessionId }` (eliminar `requiresMfa`)
- Navegar a `/mfa` con `state: { sessionId, email: data.email }`

Tests: ninguno.

---

### 15. `src/features/auth/components/MfaCard.tsx` — MODIFICAR

- Leer `sessionId` desde `useLocation().state`
- `verify(code)` → `authService.verifyMfa(sessionId, code)`
  → `setSession(result.token, result.user)` → `navigate("/dashboard")`
- `handleResend()` → `authService.resendMfa(sessionId)` (real, con manejo de error
  para límite de reenvíos)
- Si CA-06 (404): mostrar "El código ha expirado" con botón "Volver al login"

Tests: ninguno.

---

### 16. `src/components/layout/Sidebar.tsx` — MODIFICAR

- Leer `user` de `sessionStore` para mostrar nombre (`firstName + lastName`)
- Botón "Cerrar sesión": llama a `sessionStore.clearSession()` y navega a `/login`

Tests: ninguno.

---

### 17. `src/components/layout/ProtectedRoute.tsx` — VERIFICAR

Leer `token` de `sessionStore` (ya rehidratado de `localStorage`).
Si `token` es null → `<Navigate to="/login" replace />`.
Si existe → `<Outlet />`.

Tests: ninguno.

---

## Resumen de archivos

### Backend — nuevos
| Archivo | Acción |
|---|---|
| `application.yaml` | Modificar |
| `config/CacheConfig.java` | Crear |
| `model/MfaEntry.java` | Crear |
| `dto/request/LoginRequest.java` | Crear |
| `dto/response/LoginResponse.java` | Crear |
| `dto/request/MfaVerifyRequest.java` | Crear |
| `dto/request/MfaResendRequest.java` | Crear |
| `dto/response/MfaVerifyResponse.java` | Crear |
| `exception/InvalidCredentialsException.java` | Crear |
| `exception/InvalidMfaCodeException.java` | Crear |
| `exception/MfaSessionNotFoundException.java` | Crear |
| `exception/MfaResendLimitException.java` | Crear |
| `exception/GlobalExceptionHandler.java` | Modificar |
| `util/JwtUtil.java` | Crear |
| `service/LoginService.java` | Crear |
| `service/impl/LoginServiceImpl.java` | Crear |
| `service/MfaService.java` | Crear |
| `service/impl/MfaServiceImpl.java` | Crear |
| `controller/AuthController.java` | Modificar |

### Backend — tests
| Archivo | Tipo |
|---|---|
| `LoginServiceImplTest.java` | Unitario |
| `MfaServiceImplTest.java` | Unitario |
| `LoginMfaControllerTest.java` | Integración |

### Frontend — modificados
| Archivo | Acción |
|---|---|
| `src/lib/apiFetch.ts` | Crear |
| `src/store/sessionStore.ts` | Modificar |
| `src/features/auth/services/authService.ts` | Reemplazar |
| `src/features/auth/components/LoginCard.tsx` | Modificar |
| `src/features/auth/components/MfaCard.tsx` | Modificar |
| `src/components/layout/Sidebar.tsx` | Modificar |
| `src/components/layout/ProtectedRoute.tsx` | Verificar/Modificar |
