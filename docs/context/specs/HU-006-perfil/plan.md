# HU-06 — Plan de implementación

## Alcance de la HU

- `GET /api/v1/users/me` — perfil completo (datos personales + preferencias + broker null)
- `PUT /api/v1/users/me/personal` — actualización parcial de datos personales
- `PUT /api/v1/users/me/preferences` — actualización parcial de preferencias
  (notificaciones, canales, visualización, operaciones, mercado, sector)
- Evento `PROFILE_UPDATED` vía `@Async` + `@Retryable` en cada actualización
- Frontend `/profile` conectado a la API real (sin mocks)
- `PreferencesCard` ampliado: canales de notificación + selección de mercado y sector
- `MarketStatusBadge` dinámico basado en el mercado del usuario y la hora UTC
- Borrar carpetas HU-002, HU-003, HU-004

---

## Cambios al contrato (`identity-service.yaml`)

- `UpdatePreferencesRequest`:
  - Eliminar `markets: array` y `sectors: array`
  - Agregar `market: string` (enum: nyse, nasdaq) y `sector: string` (enum: tech, health, consumer)
- `UserPreferencesInternal`:
  - Agregar `market: string` y `sector: string`

---

## Archivos a crear

### Backend — identity-service

| # | Archivo |
|---|---------|
| 1 | `security/JwtFilter.java` — OncePerRequestFilter; extrae Bearer token, valida con JWT_SECRET, pone `userId` en SecurityContext |
| 2 | `dto/request/UpdatePersonalRequest.java` — firstName, lastName, phone, birthDate, nationality, address (todos opcionales) |
| 3 | `dto/request/UpdatePreferencesRequest.java` — todos los campos de preferencias opcionales; market y sector como String |
| 4 | `dto/response/PreferencesResponse.java` — refleja todos los campos de `UserPreference` |
| 5 | `dto/response/UserProfileResponse.java` — id, firstName, lastName, email, phone, birthDate, nationality, address, role, plan, preferences, broker |
| 6 | `service/UserService.java` — interfaz: getProfile, updatePersonal, updatePreferences |
| 7 | `service/impl/UserServiceImpl.java` — implementación con @Async PROFILE_UPDATED |
| 8 | `controller/UserController.java` — 3 endpoints protegidos por JWT |
| 9 | `test/.../service/UserServiceImplTest.java` — 5 tests unitarios |
| 10 | `test/.../controller/UserControllerTest.java` — 6 tests de integración |

### Frontend

| # | Archivo |
|---|---------|
| 11 | `features/profile/services/profileService.ts` — getProfile(), updatePersonal(), updatePreferences() |

---

## Archivos a modificar

### Backend — identity-service

| # | Archivo | Cambio |
|---|---------|--------|
| 12 | `util/JwtUtil.java` | Agregar `extractUserId(String token): String` |
| 13 | `config/SecurityConfig.java` | Registrar JwtFilter antes de UsernamePasswordAuthenticationFilter |

### Documentos

| # | Archivo | Cambio |
|---|---------|--------|
| 14 | `docs/context/contracts/identity-service.yaml` | market/sector en UpdatePreferencesRequest y UserPreferencesInternal |

### Frontend

| # | Archivo | Cambio |
|---|---------|--------|
| 15 | `features/profile/store/profileStore.ts` | Agregar notifChannels (email/sms/whatsapp), market, sector; `loadProfile(token)`, save por sección |
| 16 | `features/profile/data/mockProfile.ts` | Separar fullName → firstName + lastName; agregar market/sector defaults |
| 17 | `features/profile/components/PreferencesCard.tsx` | Agregar sección "Canal de notificaciones" (email habilitado, SMS/WhatsApp deshabilitados con tooltip); agregar sección "Mercado y Sector" con selects; botón "Guardar preferencias" |
| 18 | `features/profile/components/EditPersonalInfoModal.tsx` | Separar en firstName + lastName; quitar campo email (read-only); llamar API en save |
| 19 | `features/profile/components/PersonalInfoCard.tsx` | Mostrar firstName + lastName separados; reflejar cambios del store |
| 20 | `features/profile/components/AutoReportsCard.tsx` | Agregar botón "Guardar" que llama API con `{ monthlyReport }` |
| 21 | `features/profile/pages/ProfilePage.tsx` | Cargar perfil al montar con `GET /api/v1/users/me`; quitar botón global de guardar |
| 22 | `features/dashboard/components/MarketStatusBadge.tsx` | Leer mercado del profileStore; calcular abierto/cerrado según hora UTC |

---

## Orden de implementación

```
1.  Borrar carpetas HU-002/003/004
2.  Actualizar identity-service.yaml
3.  JwtUtil — extractUserId()
4.  JwtFilter (nuevo)
5.  SecurityConfig — registrar JwtFilter
6.  DTOs backend (UpdatePersonalRequest, UpdatePreferencesRequest, PreferencesResponse, UserProfileResponse)
7.  UserService interfaz + UserServiceImpl
8.  UserController
9.  Tests backend (UserServiceImplTest + UserControllerTest)
10. profileService.ts
11. profileStore.ts — actualizar
12. PreferencesCard.tsx — ampliar + botón guardar
13. EditPersonalInfoModal.tsx — separar nombre/apellido + API
14. PersonalInfoCard.tsx — ajustar campos
15. AutoReportsCard.tsx — agregar botón guardar
16. ProfilePage.tsx — cargar perfil al montar
17. MarketStatusBadge.tsx — dinámico
```

---

## Tests backend

### UserServiceImplTest (JUnit 5 + Mockito)
- `getProfile_existingUser_returnsFullProfile`
- `getProfile_unknownUser_throwsNotFoundException`
- `updatePersonal_validFields_persistsAndFiresAudit`
- `updatePersonal_unknownUser_throwsNotFoundException`
- `updatePreferences_validFields_persistsAndFiresAudit`

### UserControllerTest (SpringBootTest + MockMvc + Testcontainers)
- `getProfile_validToken_returns200WithProfile`
- `getProfile_noToken_returns401`
- `updatePersonal_validRequest_returns200`
- `updatePersonal_shortFirstName_returns400`
- `updatePreferences_validRequest_returns200`
- `updatePreferences_noToken_returns401`
