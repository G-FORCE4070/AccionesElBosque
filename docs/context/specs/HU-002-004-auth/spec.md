# HU-02 · HU-03 · HU-04 — Login, MFA y Cierre de sesión

> Estas tres historias se planifican e implementan juntas porque forman un flujo
> indivisible: sin login no hay MFA, sin MFA no hay sesión, y sin cierre de sesión
> el flujo queda incompleto. Se entregan en un solo commit.

---

## Prioridad y estimación

| HU     | Historia                          | Puntos |
|--------|-----------------------------------|--------|
| HU-02  | Inicio de sesión                  | 3      |
| HU-03  | Autenticación Multifactor (MFA)   | 5      |
| HU-04  | Cierre de sesión                  | 1      |
|        | **Total**                         | **9**  |

| Campo     | Valor                        |
|-----------|------------------------------|
| Prioridad | Must                         |
| Épica     | Registro y Autenticación     |
| Feature   | Inicio de sesión y seguridad |
| Servicio  | identity-service + frontend  |

---

## Referencias

- `docs/context/architecture.md` — identity-service, caché Caffeine, ADR-04 (JWT), ADR-07 (MFA directo)
- `docs/context/domain.md` — eventos: `LOGIN_FAILED`, `MFA_CODE_SENT`, `MFA_FAILED`, `LOGIN_SUCCESS`
- `docs/context/tech_stack.md` — Caffeine TTL 5 min, JJWT 0.12.x, Zustand, localStorage
- `docs/context/decisions.md` — ADR-04 (JWT con claims), ADR-05 (@Async + Retry)
- `docs/context/contracts/identity-service.yaml` — `POST /auth/login`, `POST /auth/mfa/verify`, `POST /auth/mfa/resend`

---

## Flujo completo

```
LoginCard → POST /auth/login
  → código generado → email enviado (síncrono, si falla → 500 sin sessionId)
  → caches guardados: session:{sessionId}=email, mfa:{sessionId}={code,attempts:0,resends:0}
  → frontend navega a /mfa con {sessionId, email}

MfaCard → POST /auth/mfa/verify {sessionId, code}
  → correcto  → JWT emitido + caches evicted → localStorage + navigate("/dashboard")
  → incorrecto → attempts++
      si attempts < 3  → 400
      si attempts >= 3 → 400 + evict cache (próximo intento devuelve 404)
  → sessionId no encontrado → 404

MfaCard → POST /auth/mfa/resend {sessionId}
  → resends < 3 → nuevo código, email reenviado, attempts reiniciado a 0
  → resends >= 3 → 400

Sidebar → cerrar sesión
  → borrar localStorage + limpiar Zustand + navigate("/login")

apiFetch (wrapper de fetch) → cualquier 401
  → logout automático + navigate("/login") con mensaje "sesión expirada"
```

---

## HU-02 — Inicio de sesión (3 pts)

### CA-01: Login exitoso

**Dado que** el inversionista envía email y contraseña correctos
**Cuando** llama a `POST /api/v1/auth/login`
**Entonces** el sistema:
1. Genera `sessionId = UUID.randomUUID()` y código MFA de 6 dígitos
2. **Envía el email primero** (síncrono con Spring Mail); si falla → HTTP 500 sin retornar sessionId
3. Si el email se envió correctamente → guarda en caché `session:{sessionId}` → email (TTL 5 min)
   y `mfa:{sessionId}` → `{ code, attempts: 0, resends: 0 }` (TTL 5 min)
4. Registra evento `MFA_CODE_SENT` vía `@Async`
5. Responde HTTP 200: `{ "sessionId": "<uuid>" }`

**Y** el frontend navega a `/mfa` con `{ sessionId, email }` por navigation state

### CA-02: Credenciales inválidas

**Dado que** el email no existe o la contraseña no coincide
**Cuando** llama a `POST /api/v1/auth/login`
**Entonces** responde HTTP 401: `{ "error": "Email o contraseña inválidos" }`
**Y** no se genera sessionId ni se envía código
**Y** registra evento `LOGIN_FAILED` vía `@Async`
**Nota:** el mensaje es deliberadamente ambiguo — no indica cuál de los dos campos falló

### CA-03: Campos vacíos

**Dado que** el inversionista envía email o contraseña vacíos (o email con formato inválido)
**Cuando** llama a `POST /api/v1/auth/login`
**Entonces** responde HTTP 400 con mensaje de validación descriptivo

---

## HU-03 — Autenticación Multifactor (5 pts)

### CA-04: Verificación MFA exitosa

**Dado que** el `sessionId` existe en caché y el código es correcto
**Cuando** llama a `POST /api/v1/auth/mfa/verify` con `{ sessionId, code }`
**Entonces** responde HTTP 200:
```json
{
  "token": "<JWT firmado HS256, exp 1h>",
  "user": {
    "id": "...",
    "firstName": "...",
    "lastName": "...",
    "email": "...",
    "role": "INVESTOR",
    "plan": "BASIC | PREMIUM"
  }
}
```
**Y** el JWT contiene claims `sub` (userId), `role`, `plan`, `alpacaAccountId`
**Y** limpia `session:{sessionId}` y `mfa:{sessionId}` del caché
**Y** registra evento `LOGIN_SUCCESS` vía `@Async`
**Y** el frontend guarda `{ token, user }` en `localStorage` bajo clave `aeb_session`,
actualiza Zustand y navega a `/dashboard`

### CA-05: Código incorrecto — hasta 3 intentos

**Dado que** el código enviado es incorrecto
**Cuando** llama a `POST /api/v1/auth/mfa/verify`
**Entonces** incrementa `attempts` en 1

- Si `attempts < 3`: responde HTTP 400 `{ "error": "Código inválido" }`, sesión sigue vigente
- Si `attempts >= 3`: responde HTTP 400 `{ "error": "Código inválido" }` **y** evict del caché
  (el siguiente intento devolverá 404 porque ya no existe la sesión)

**Y** registra evento `MFA_FAILED` vía `@Async`

### CA-06: Código expirado o sessionId no encontrado

**Dado que** el `sessionId` no existe en caché (TTL vencido, 3 intentos fallidos, o servicio reiniciado)
**Cuando** llama a `POST /api/v1/auth/mfa/verify` o `POST /api/v1/auth/mfa/resend`
**Entonces** responde HTTP 404: `{ "error": "El código ha expirado" }`
**Y** el frontend muestra el error y ofrece volver al login

### CA-07: Reenvío del código MFA

**Dado que** el `sessionId` existe y `resends < 3`
**Cuando** llama a `POST /api/v1/auth/mfa/resend` con `{ sessionId }`
**Entonces** genera nuevo código, reinicia `attempts` a 0, incrementa `resends`,
envía email y responde HTTP 200
**Y** registra evento `MFA_CODE_SENT` vía `@Async`

**Dado que** `resends >= 3`
**Entonces** responde HTTP 400: `{ "error": "Máximo de reenvíos alcanzado" }`

---

## HU-04 — Cierre de sesión (1 pt)

### CA-08: Cierre de sesión manual

**Dado que** el inversionista tiene sesión activa
**Cuando** hace clic en "Cerrar sesión" en el sidebar
**Entonces** se elimina `aeb_session` de `localStorage`, se limpia el estado de Zustand
**Y** el frontend redirige a `/login`
**Y** si intenta navegar a rutas protegidas, `ProtectedRoute` lo redirige a `/login`

### CA-09: Cierre automático por sesión expirada

**Dado que** el backend responde HTTP 401 a cualquier petición (JWT expirado o inválido)
**Cuando** el wrapper `apiFetch` recibe esa respuesta
**Entonces** ejecuta el mismo flujo de logout (limpia localStorage + Zustand)
**Y** redirige a `/login` con mensaje: `"Tu sesión ha expirado. Inicia sesión nuevamente."`

### CA-10: Rutas protegidas sin sesión

**Dado que** no hay token en `localStorage` (o fue limpiado)
**Cuando** el usuario navega a cualquier ruta protegida (`/dashboard`, `/portfolio`, etc.)
**Entonces** `ProtectedRoute` lo redirige automáticamente a `/login`

---

## Definition of Done

**HU-02:**
- [ ] `POST /api/v1/auth/login` implementado; retorna `{ sessionId }`
- [ ] Credenciales validadas con `BCryptPasswordEncoder.matches()`
- [ ] `LoginRequest` validado con `@NotBlank` y `@Email`
- [ ] Email enviado **antes** de guardar en caché; si falla → 500 sin sessionId huérfano
- [ ] Caché Caffeine configurado: `sessions` y `mfaCodes` con TTL 5 min

**HU-03:**
- [ ] `POST /api/v1/auth/mfa/verify` implementado; retorna JWT + `UserInfo`
- [ ] JWT firmado HS256, claims `sub/role/plan/alpacaAccountId`, expiración 1 hora
- [ ] Caché limpiado tras verificación exitosa
- [ ] Sesión invalidada en el 3er intento fallido (responde 400, próximo intento 404)
- [ ] `POST /api/v1/auth/mfa/resend` con límite de 3 reenvíos
- [ ] Código MFA generado con `SecureRandom` (`String.format("%06d", random.nextInt(1_000_000))`)
- [ ] Eventos `LOGIN_FAILED`, `MFA_CODE_SENT`, `MFA_FAILED`, `LOGIN_SUCCESS` vía `@Async` + `@Retryable`

**HU-04:**
- [ ] Botón "Cerrar sesión" en el sidebar limpia `localStorage` y Zustand; redirige a `/login`
- [ ] `apiFetch` wrapper creado: captura 401 y ejecuta logout automático con mensaje de sesión expirada
- [ ] `ProtectedRoute` redirige a `/login` si no hay token

**Común:**
- [ ] `sessionStore.ts`: tipo `User` expandido (`id, firstName, lastName, email, role, plan`),
  persistencia en `localStorage` bajo clave `aeb_session`, rehidratación al arrancar
- [ ] `authService.ts` conectado a API real (`login`, `verifyMfa`, `resendMfa`)
- [ ] `LoginCard.tsx` navega a `/mfa` con `{ sessionId, email }`
- [ ] `MfaCard.tsx` usa `sessionId` del navigation state; "Reenviar" llama a la API real
- [ ] Tests unitarios: `LoginServiceImplTest` (3 casos), `MfaServiceImplTest` (6 casos)
- [ ] Tests de integración: `LoginMfaControllerTest` (7 casos, MockMvc + Testcontainers)
- [ ] `mvn test` pasa sin errores

---

## Notas técnicas

- El `sub` del JWT es el `userId` (UUID), **no** el email.
- Código MFA: `String.format("%06d", secureRandom.nextInt(1_000_000))` garantiza siempre 6 dígitos.
- El orden en `LoginServiceImpl` es: generar código → enviar email → si OK, guardar caches.
  Esto evita sessionIds huérfanos si Spring Mail lanza excepción.
- Si identity-service se reinicia, los caches en memoria se pierden. El 404 en verify/resend
  indica al frontend que vuelva al login (sin mensaje de error técnico).
- HU-04 no tiene endpoint de logout en el backend (MVP). El JWT expira por TTL (`exp`).
  En producción se agregaría una lista negra de tokens (deuda técnica).
- `apiFetch` es un wrapper sobre `fetch` nativo que intercepta 401 globalmente.
  No se usa axios para no agregar dependencias no documentadas en `tech_stack.md`.
- El frontend puede decodificar el claim `exp` del JWT (base64, sin verificar firma)
  para detectar expiración proactiva antes de un request.
