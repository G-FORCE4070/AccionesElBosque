# HU-06 — Tasks

## Limpieza

- [x] T-01: Borrar carpetas `docs/context/specs/HU-002-login/`, `HU-003-mfa/`, `HU-004-logout/`

## Documentos

- [x] T-02: Actualizar `docs/context/contracts/identity-service.yaml`
  - `UpdatePreferencesRequest`: reemplazar `markets`/`sectors` (array) por `market`/`sector` (string)
  - `UserPreferencesInternal`: agregar `market: string` y `sector: string`

## Backend — identity-service

- [x] T-03: `util/JwtUtil.java` — agregar método `extractUserId(String token): String`
- [x] T-04: Crear `security/JwtFilter.java` — `OncePerRequestFilter` que valida Bearer token y pone `userId` (UUID string) como principal en `SecurityContextHolder`
- [x] T-05: `config/SecurityConfig.java` — inyectar `JwtFilter` y agregarlo antes de `UsernamePasswordAuthenticationFilter`
- [x] T-06: Crear `dto/request/UpdatePersonalRequest.java` — campos opcionales: firstName, lastName, phone, birthDate, nationality, address; validaciones Jakarta
- [x] T-07: Crear `dto/request/UpdatePreferencesRequest.java` — todos los campos opcionales; market y sector como String con enum validation
- [x] T-08: Crear `dto/response/PreferencesResponse.java` — todos los campos de `UserPreference` incluyendo market y sector
- [x] T-09: Crear `dto/response/UserProfileResponse.java` — id, firstName, lastName, email, phone, birthDate, nationality, address, role, plan, preferences (PreferencesResponse), broker (null en MVP)
- [x] T-10: Crear `service/UserService.java` — interfaz con: `getProfile(String userId)`, `updatePersonal(String userId, UpdatePersonalRequest)`, `updatePreferences(String userId, UpdatePreferencesRequest)`
- [x] T-11: Crear `service/impl/UserServiceImpl.java` — implementación con `@Async` + `@Retryable` para `PROFILE_UPDATED`
- [x] T-12: Crear `controller/UserController.java` — `GET /api/v1/users/me`, `PUT /api/v1/users/me/personal`, `PUT /api/v1/users/me/preferences`
- [x] T-13: Crear `test/.../service/UserServiceImplTest.java` — 5 tests unitarios con Mockito
- [x] T-14: Crear `test/.../controller/UserControllerTest.java` — 6 tests de integración con MockMvc + Testcontainers

## Frontend

- [x] T-15: Crear `features/profile/services/profileService.ts` — `getProfile()`, `updatePersonal()`, `updatePreferences()` con Bearer token
- [x] T-16: `features/profile/store/profileStore.ts` — agregar `notifChannels` (email/sms/whatsapp), `market`, `sector`; agregar `loadProfile(token)`; separar saves por sección
- [x] T-17: `features/profile/data/mockProfile.ts` — separar `fullName` → `firstName` + `lastName`; agregar defaults de `market` y `sector`
- [x] T-18: `features/profile/components/PreferencesCard.tsx` — agregar sección "Canal de notificaciones" (email toggle + SMS/WhatsApp deshabilitados); agregar sección "Mercado y Sector" con selects; botón "Guardar preferencias"
- [x] T-19: `features/profile/components/EditPersonalInfoModal.tsx` — separar firstName/lastName; quitar campo email; llamar `profileService.updatePersonal()` al guardar
- [x] T-20: `features/profile/components/PersonalInfoCard.tsx` — mostrar `firstName` y `lastName` por separado
- [x] T-21: `features/profile/components/AutoReportsCard.tsx` — agregar botón "Guardar" que llama `profileService.updatePreferences({ monthlyReport })`
- [x] T-22: `features/profile/pages/ProfilePage.tsx` — llamar `loadProfile(token)` al montar; eliminar botón global de guardar
- [x] T-23: `features/dashboard/components/MarketStatusBadge.tsx` — leer `market` del profileStore; calcular abierto/cerrado según hora UTC (NYSE/NASDAQ: 14:30–21:00 UTC, lun–vie)

## Correcciones (sesión fix — 2026-05-27)

- [x] T-24: `store/profileStore.ts` — estado inicial `broker: null`, `subscription.plan: "basic"`; draft state en `PreferencesCard`; nuevos campos: postalCode, incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal
- [x] T-25: `components/ToggleRow.tsx` + `SelectRow.tsx` — agregar prop `disabled`
- [x] `data/profileConstants.ts` — nuevo archivo con mapas de países, nacionalidades y campos financieros a etiquetas legibles
- [x] T-26: `data/mockProfile.ts` — limpiar datos hardcodeados; estado inicial vacío
- [x] T-27: `services/profileService.ts` — ProfileData y UpdatePersonalPayload con nuevos campos; eliminar phone de payload
- [x] T-28: `components/PersonalInfoCard.tsx` — tres secciones (Identidad RO, Residencia, Perfil de inversión); lookup de país/campos financieros a nombres completos
- [x] T-29: `components/EditPersonalInfoModal.tsx` — solo campos editables (residencia + perfil financiero); useEffect reinicializa form al abrir; nota KYC
- [x] T-30: `components/PreferencesCard.tsx` — draft local; todos los toggles disabled excepto orderConfirm; priceAlerts gateado por plan premium
- [x] T-31: `components/SubscriptionCard.tsx` — botones condicionales por plan; básico muestra "Actualizar a Premium"
- [x] T-32: `components/AutoReportsCard.tsx` — deshabilitado con badge "Próximamente"
- [x] T-33: Backend `UpdatePersonalRequest.java` — agregar city, postalCode, country, campos financieros; campos KYC mantenidos solo para compatibilidad API
- [x] T-34: Backend `UserProfileResponse.java` — agregar postalCode y campos del perfil financiero
- [x] T-35: Backend `UserServiceImpl.java` — updatePersonal maneja nuevos campos de contacto y financieros
- [x] T-36: `.github/workflows/ci.yml` — separar mvn verify de sonar:sonar; sonar condicional a SONAR_TOKEN
