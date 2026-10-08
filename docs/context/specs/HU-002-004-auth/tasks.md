# Tasks — HU-02 + HU-03 + HU-04: Login, MFA y Cierre de sesión

## Bloque 1 — Backend: configuración de caché

- [x] **T-01** Modificar `application.yaml`: agregar `spring.cache.type: caffeine`
- [x] **T-02** Crear `config/CacheConfig.java`: `@Configuration @EnableCaching`, `CaffeineCacheManager` con dos caches TTL 5 min (`sessions`, `mfaCodes`)

---

## Bloque 2 — Backend: modelo

- [x] **T-03** Crear `model/MfaEntry.java`: POJO serializable con campos `code`, `attempts`, `resends`; constructor, getters y setters (sin Lombok)

---

## Bloque 3 — Backend: DTOs

- [x] **T-04** Crear `dto/request/LoginRequest.java`: `email` (`@NotBlank @Email`), `password` (`@NotBlank`)
- [x] **T-05** Crear `dto/response/LoginResponse.java`: `String sessionId`
- [x] **T-06** Crear `dto/request/MfaVerifyRequest.java`: `sessionId` (`@NotBlank`), `code` (`@NotBlank @Size(min=6,max=6)`)
- [x] **T-07** Crear `dto/request/MfaResendRequest.java`: `sessionId` (`@NotBlank`)
- [x] **T-08** Crear `dto/response/MfaVerifyResponse.java`: `token`, `UserInfo` (record interno: `id, firstName, lastName, email, role, plan`)

---

## Bloque 4 — Backend: excepciones

- [x] **T-09** Crear `exception/InvalidCredentialsException.java`
- [x] **T-10** Crear `exception/InvalidMfaCodeException.java`
- [x] **T-11** Crear `exception/MfaSessionNotFoundException.java`
- [x] **T-12** Crear `exception/MfaResendLimitException.java`
- [x] **T-13** Modificar `exception/GlobalExceptionHandler.java`: agregar 4 handlers (401, 400, 404, 400)

---

## Bloque 5 — Backend: utilidad JWT

- [x] **T-14** Crear `util/JwtUtil.java`: `@Component`, `generateToken(User)` firmado HS256 con JJWT 0.12.x, claims `sub/role/plan/alpacaAccountId`, exp configurable

---

## Bloque 6 — Backend: servicio Login

- [x] **T-15** Crear `service/LoginService.java` (interfaz)
- [x] **T-16** Crear `service/impl/LoginServiceImpl.java`: flujo completo (find user → BCrypt → generar sessionId+código → enviar email síncrono → guardar caches → audit async → retornar LoginResponse)

---

## Bloque 7 — Backend: servicio MFA

- [x] **T-17** Crear `service/MfaService.java` (interfaz)
- [x] **T-18** Crear `service/impl/MfaServiceImpl.java`: `verify()` con control de intentos y eviction; `resend()` con límite 3 reenvíos

---

## Bloque 8 — Backend: controlador

- [x] **T-19** Modificar `controller/AuthController.java`: agregar `POST /login`, `POST /mfa/verify`, `POST /mfa/resend`

---

## Bloque 9 — Backend: tests unitarios

- [x] **T-20** Crear `LoginServiceImplTest.java` (3 casos: login_success, login_emailNotFound, login_wrongPassword)
- [x] **T-21** Crear `MfaServiceImplTest.java` (6 casos: verify_success, verify_wrongCode_firstAttempt, verify_wrongCode_thirdAttempt, verify_expiredSession, resend_success, resend_limitReached)

---

## Bloque 10 — Backend: tests de integración

- [x] **T-22** Crear `LoginMfaControllerTest.java` (7 casos: login OK, login 401, login 400, verify OK, verify 400, verify 3er intento → 400+siguiente 404, resend OK)

---

## Bloque 11 — Frontend: apiFetch wrapper

- [x] **T-23** Crear `src/lib/apiFetch.ts`: wrapper sobre `fetch` nativo con inyección de `Authorization: Bearer`, interceptor 401 (logout + navigate `/login` con mensaje de sesión expirada)

---

## Bloque 12 — Frontend: sessionStore

- [x] **T-24** Modificar `src/store/sessionStore.ts`: expandir tipo `User` (`id, firstName, lastName, email, role, plan`), `setSession(token, user)` con persistencia en `localStorage` (`aeb_session`), `clearSession()` con limpieza, rehidratación al crear el store

---

## Bloque 13 — Frontend: authService

- [x] **T-25** Reemplazar `src/features/auth/services/authService.ts`: `login()`, `verifyMfa()`, `resendMfa()` conectados a API real

---

## Bloque 14 — Frontend: LoginCard

- [x] **T-26** Modificar `src/features/auth/components/LoginCard.tsx`: consumir `authService.login()`, navegar a `/mfa` con `{ sessionId, email }` en navigation state

---

## Bloque 15 — Frontend: MfaCard

- [x] **T-27** Modificar `src/features/auth/components/MfaCard.tsx`: leer `sessionId` del navigation state, `verify()` → `setSession()` → navigate `/dashboard`, `handleResend()` real, manejo de CA-06 (404 → mensaje + botón volver al login)

---

## Bloque 16 — Frontend: Sidebar

- [x] **T-28** Modificar `src/components/layout/Sidebar.tsx`: leer `user` de `sessionStore`, botón "Cerrar sesión" → `clearSession()` + navigate `/login`

---

## Bloque 17 — Frontend: ProtectedRoute

- [x] **T-29** Verificar/Modificar `src/components/layout/ProtectedRoute.tsx`: leer `token` de `sessionStore` (rehidratado de `localStorage`); si null → `<Navigate to="/login" replace />`
