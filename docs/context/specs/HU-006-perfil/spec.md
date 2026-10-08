# HU-06 — Actualización de perfil y preferencias

## Historia de usuario

**Como** inversionista,
**quiero** modificar mis datos personales, preferencias de notificación, visualización del portafolio
y configuración de operaciones,
**para** personalizar mi experiencia en la plataforma.

---

## Prioridad y estimación

| Campo      | Valor                                    |
|------------|------------------------------------------|
| Prioridad  | Must                                     |
| Estimación | 5 puntos                                 |
| Épica      | Gestión de Perfil y Suscripción          |
| Feature    | Perfil                                   |
| Servicio   | identity-service                         |

---

## Referencias

- `docs/context/architecture.md` — identity-service, ADR-05 (@Async)
- `docs/context/domain.md` — evento `PROFILE_UPDATED`
- `docs/context/contracts/identity-service.yaml` — `GET /api/v1/users/me`, `PUT /api/v1/users/me/personal`, `PUT /api/v1/users/me/preferences`

---

## Criterios de aceptación

### CA-01: Consulta del perfil completo

**Dado que** el inversionista tiene un JWT válido
**Cuando** llama a `GET /api/v1/users/me`
**Entonces** el sistema responde con HTTP 200
**Y** el cuerpo contiene: id, firstName, lastName, email, phone, birthDate, nationality, address, role, plan,
preferencias completas (notificación, visualización, operaciones) e info del broker (null en MVP)

---

### CA-02: Actualización de datos personales

**Dado que** el inversionista envía firstName, lastName, phone, birthDate, nationality y/o address
**Cuando** llama a `PUT /api/v1/users/me/personal`
**Entonces** el sistema responde con HTTP 200
**Y** los campos válidos son persistidos en `identity.users`
**Y** el sistema registra el evento `PROFILE_UPDATED` vía `@Async`

---

### CA-03: Actualización de preferencias de notificación

**Dado que** el inversionista envía valores booleanos para `marketOpen`, `priceAlerts`, `dailySummary`,
`orderConfirm`, `notifEmail`, `notifSms`, `notifWhatsapp`, `monthlyReport`
**Cuando** llama a `PUT /api/v1/users/me/preferences`
**Entonces** el sistema responde con HTTP 200
**Y** las preferencias son persistidas en `identity.user_preferences`
**Y** el sistema registra el evento `PROFILE_UPDATED` vía `@Async`

---

### CA-04: Actualización de preferencias de visualización del portafolio

**Dado que** el inversionista envía `viewType` (list/bars/detail), `defaultOrder` (totalValue/gainPct/symbol)
y/o `showLowWeight` (boolean)
**Cuando** llama a `PUT /api/v1/users/me/preferences`
**Entonces** el sistema responde con HTTP 200
**Y** las preferencias son persistidas en `identity.user_preferences`
**Y** el evento `PROFILE_UPDATED` es registrado vía `@Async`

---

### CA-05: Actualización de configuración de operaciones

**Dado que** el inversionista envía `defaultOrderType` (market), `dailyLimit` (entero >= 0),
`requireDoubleAuth` (boolean), `markets` (nyse/nasdaq) y/o `sectors` (tech/health/consumer)
**Cuando** llama a `PUT /api/v1/users/me/preferences`
**Entonces** el sistema responde con HTTP 200
**Y** las preferencias son persistidas en `identity.user_preferences`
**Y** el evento `PROFILE_UPDATED` es registrado vía `@Async`

---

### CA-06: Validaciones de formato

**Dado que** el inversionista envía datos con formato inválido en `PUT /api/v1/users/me/personal`
(ej: `phone` con menos de 7 caracteres, `firstName` con menos de 2 caracteres)
**Cuando** el sistema procesa la petición
**Entonces** responde con HTTP 400
**Y** el cuerpo contiene un mensaje de error descriptivo indicando el campo inválido

---

## Definition of Done

- [ ] Endpoint `GET /api/v1/users/me` implementado, requiere JWT válido
- [ ] Endpoint `PUT /api/v1/users/me/personal` implementado con validaciones Jakarta Validation
- [ ] Endpoint `PUT /api/v1/users/me/preferences` implementado — acepta cualquier subconjunto de campos (actualización parcial)
- [ ] `UpdatePersonalRequest` valida: `firstName` y `lastName` min 2 chars, `phone` min 7 chars
- [ ] `UpdatePreferencesRequest` valida: `dailyLimit` min 0, enums correctos para `viewType`, `defaultOrder`, `defaultOrderType`
- [ ] Ambos endpoints protegidos por JWT (`userId` extraído del claim `sub`)
- [ ] Evento `PROFILE_UPDATED` registrado vía `@Async` + `@Retryable` en cada actualización exitosa
- [ ] Página de perfil implementada en el frontend con formulario separado por secciones
- [ ] Cada sección tiene botón de guardado independiente
- [ ] Tests unitarios para `UserServiceImpl` cubriendo: obtener perfil, actualizar datos personales con datos válidos, actualizar con datos inválidos, actualizar preferencias
- [ ] Tests de integración del endpoint con MockMvc
- [ ] El código compila y pasa `mvn test` sin errores

---

## Notas técnicas

- El email y el `role` no son actualizables mediante estos endpoints. Son de solo lectura.
- El `plan` tampoco es actualizable aquí — se gestiona exclusivamente en HU-07 (suscripción).
- La actualización de `markets` y `sectors` en preferencias afecta el filtrado del dashboard (HU-08).
- Los endpoints `PUT` aplican actualización parcial: solo los campos presentes en el body son actualizados.
  Los campos no incluidos mantienen su valor actual.
- `defaultOrderType` solo acepta `market` en el MVP (limit, stop_loss, take_profit son para sprints Should).
- Los eventos de auditoría son asíncronos: si audit-service falla, la actualización del perfil igual se completa.
