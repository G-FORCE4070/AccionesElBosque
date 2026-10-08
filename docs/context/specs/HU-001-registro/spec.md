# HU-01 — Registro de inversionista

## Historia de usuario

**Como** visitante de la plataforma,
**quiero** registrarme completando un formulario de 6 pasos,
**para** crear una cuenta de inversionista y acceder a las funcionalidades
de trading.

---

## Prioridad y estimación

| Campo        | Valor                          |
|--------------|--------------------------------|
| Prioridad    | Must                           |
| Estimación   | 8 puntos                       |
| Épica        | Registro y Autenticación       |
| Feature      | Registro                       |
| Servicio     | identity-service               |

---

## Referencias

- `docs/context/architecture.md` — identity-service, ADR-11 (registro crea BASIC primero), ADR-07 (correo MFA directo)
- `docs/context/domain.md` — roles (INVESTOR), planes (BASIC/PREMIUM), eventos de auditoría
- `docs/context/data-model.md` — entidades `identity.users`, `identity.user_preferences`, `identity.subscriptions`
- `docs/context/decisions.md` — ADR-05 (@Async + Spring Retry), ADR-11
- `docs/context/contracts/identity-service.yaml` — endpoint `POST /api/v1/auth/register`

---

## Pasos del formulario

El formulario agrupa los campos en 6 pasos. La distribución puede ajustarse
durante el diseño de UI, pero debe cubrir todos los campos obligatorios de
`identity.users`.

| Paso | Nombre              | Campos principales                                                                 |
|------|---------------------|------------------------------------------------------------------------------------|
| 1    | Acceso              | email, password                                                                    |
| 2    | Datos personales    | firstName, lastName, phone, birthDate, docType, docNumber                          |
| 3    | Residencia          | nationality, country, address, city, postalCode                                    |
| 4    | Perfil financiero   | incomeSource, annualIncome, netWorth, riskTolerance, tradingExperience, investmentGoal |
| 5    | Cumplimiento        | isPep, isFinraAffiliated, acceptsTerms, acceptsPrivacy, authorizeDataUse           |
| 6    | Plan                | plan (BASIC o PREMIUM)                                                             |

---

## Criterios de aceptación

### CA-01: Registro exitoso con plan BASIC

**Dado que** el visitante completó correctamente los 6 pasos del formulario
con todos los campos obligatorios
**Y** seleccionó el plan "basic"
**Cuando** envía el formulario final
**Entonces** el sistema responde con código HTTP 201
**Y** el cuerpo contiene:
```json
{
  "message": "Registro exitoso. Revisa tu correo de bienvenida.",
  "checkoutUrl": null
}
```
**Y** se crea una cuenta de usuario con plan `BASIC` en el sistema
**Y** se crea una cuenta en Alpaca Broker API (sandbox) y se intenta fondear
con $50,000 USD virtuales (el fondeo puede quedar diferido si la cuenta aún
no está en estado ACTIVE — ver deuda técnica #11)
**Y** el usuario recibe un correo electrónico de bienvenida en la dirección
registrada
**Y** el frontend redirige al usuario a la pantalla de inicio de sesión

---

### CA-02: Registro exitoso con plan PREMIUM

**Dado que** el visitante completó correctamente los 6 pasos del formulario
**Y** seleccionó el plan "premium"
**Cuando** envía el formulario
**Entonces** el sistema crea la cuenta con plan `BASIC` temporalmente
**Y** el cuerpo de la respuesta incluye un campo `checkoutUrl` con una URL
válida de Stripe Checkout (no nula)
**Y** el frontend redirige al usuario a esa URL para completar el pago
**Y** tras completar el pago, Stripe redirige al usuario a
`/payment/success?session_id={CHECKOUT_SESSION_ID}`
**Y** el frontend llama a `POST /api/v1/subscriptions/confirm` con el
`session_id` para que el backend verifique el pago contra la API de Stripe
**Y** si el pago está confirmado, el plan del usuario se actualiza a `PREMIUM`
en la base de datos y se registra el evento `PLAN_UPGRADED`
**Y** el frontend redirige al usuario a `/login` con mensaje de éxito

---

### CA-06: Pago no completado en confirmación

**Dado que** el usuario llega a `/payment/success` con un `session_id` de
una sesión Stripe que aún no fue pagada (o es inválida)
**Cuando** el frontend llama a `POST /api/v1/subscriptions/confirm`
**Entonces** el sistema responde con código HTTP 402
**Y** el cuerpo contiene un mensaje de error
**Y** el plan del usuario permanece `BASIC`

---

### CA-03: Correo electrónico ya registrado

**Dado que** ya existe una cuenta registrada con el correo
"inversionista@example.com"
**Cuando** un visitante intenta registrarse con ese mismo correo
**Entonces** el sistema responde con código HTTP 409
**Y** el cuerpo del error es:
```json
{ "error": "El correo electrónico ya está registrado" }
```
**Y** no se crea ninguna cuenta nueva en el sistema
**Y** no se realiza ninguna llamada a Alpaca Broker API

---

### CA-04: Fallo en la creación de cuenta Alpaca

**Dado que** el visitante completó correctamente todos los campos del formulario
**Y** Alpaca Broker API devuelve un error (5xx o timeout) al intentar crear
la cuenta
**Cuando** el sistema intenta procesar el registro
**Entonces** el sistema responde con código HTTP 500
**Y** el cuerpo del error es:
```json
{ "error": "No se pudo completar el registro. Intente nuevamente." }
```
**Y** no se persiste ningún dato del usuario en la base de datos
**Y** no se envía correo de bienvenida al visitante

---

### CA-05: Validación de campos en el formulario

**Dado que** el visitante intenta avanzar de un paso a otro
u omite un campo obligatorio
**O** ingresa un email con formato inválido (ej: sin "@" o sin dominio)
**O** ingresa una contraseña de menos de 8 caracteres
**Cuando** intenta avanzar al paso siguiente o enviar el formulario
**Entonces** el sistema muestra un mensaje de error descriptivo debajo
de cada campo con problema
**Y** no permite avanzar ni enviar el formulario hasta que todos los errores
sean corregidos

---

## Definition of Done

- [ ] Formulario de 6 pasos implementado en el frontend con validaciones
      por paso (React Hook Form + Zod)
- [ ] Endpoint `POST /api/v1/auth/register` implementado en identity-service
      siguiendo el contrato en `identity-service.yaml`
- [ ] Todos los campos de `RegisterRequest` validados con anotaciones
      de Jakarta Validation (`@NotBlank`, `@Email`, `@Size`, etc.)
- [ ] La creación de usuario + cuenta Alpaca está envuelta en `@Transactional`;
      si la creación de cuenta Alpaca falla, se hace rollback completo.
      El fondeo (`JNLC`) es no-fatal: si falla (cuenta en estado SUBMITTED),
      se loguea warning y el registro continúa (ver deuda técnica #11)
- [ ] Integración con Alpaca Broker API para creación de cuenta y fondeo
      con $50,000 USD virtuales usando `ALPACA_BROKER_API_KEY`
      y `ALPACA_BROKER_SECRET_KEY`
- [ ] Si el plan seleccionado es PREMIUM, se genera una Stripe Checkout
      Session con el `price_id` correspondiente y se devuelve `checkoutUrl`
- [ ] Endpoint `POST /api/v1/subscriptions/confirm` implementado: verifica la
      sesión de Stripe contra la API (sin webhooks ni CLI), actualiza el plan
      del usuario a `PREMIUM` y registra el evento `PLAN_UPGRADED`
- [ ] Página `/payment/success` en el frontend: lee `session_id` de la URL,
      llama al endpoint de confirmación, muestra resultado y redirige al login
- [ ] Se crean los registros `identity.users` y `identity.user_preferences`
      con valores por defecto para las preferencias
- [ ] El correo de bienvenida se envía con Spring Mail directamente desde
      identity-service (no pasa por notification-service)
- [ ] Los eventos `USER_REGISTERED` y `ALPACA_ACCOUNT_CREATED` se envían
      asíncronamente a audit-service usando `@Async` + `@Retryable`
      (3 intentos, 1 segundo de espera entre intentos)
- [ ] Pruebas unitarias escritas para `RegisterServiceImpl` cubriendo:
      registro exitoso BASIC, registro exitoso PREMIUM, email duplicado,
      fallo de Alpaca
- [ ] Pruebas de integración del endpoint con MockMvc, mock de Alpaca
      y Testcontainers para PostgreSQL
- [ ] El código compila y pasa `mvn test` sin errores
- [ ] SonarCloud no reporta bloqueantes ni críticos nuevos

---

## Notas técnicas

- El código MFA **no** se envía durante el registro. Solo se envía
  el correo de bienvenida.
- La cuenta de Alpaca se crea en modo sandbox. El `alpacaAccountId`
  devuelto por Alpaca se almacena en `identity.users.alpaca_account_id`.
- Los eventos de auditoría son asíncronos (ADR-05): si audit-service
  falla, el registro del usuario igual se completa. El fallo de auditoría
  no debe bloquear ni revertir el registro.
- El correo de bienvenida es síncrono respecto al registro: si el email falla,
  se loguea el error pero no se hace rollback (el usuario ya fue creado
  exitosamente en ese punto).

---

## Aclaraciones de sesión

- **Confirmación de pago Stripe sin CLI:** En lugar de webhooks (que requieren
  el Stripe CLI en local), el flujo es: Stripe redirige al usuario a
  `/payment/success?session_id={CHECKOUT_SESSION_ID}` → el frontend llama a
  `POST /api/v1/subscriptions/confirm` → el backend consulta la API de Stripe
  con `Session.retrieve(sessionId)` y verifica que `paymentStatus == "paid"` →
  actualiza el plan a `PREMIUM`. Sin CLI, sin `stripe listen`, sin webhooks.
  La `successUrl` en `application.yaml` debe incluir `{CHECKOUT_SESSION_ID}`
  como literal para que Stripe lo reemplace.

- **billingCycle en registro Premium:** El paso 6 del formulario muestra dos
  botones adicionales cuando el usuario selecciona Premium: "Mensual ($12/mes)"
  y "Anual ($120/año)". El campo `billingCycle: monthly | annual` se agrega al
  `RegisterRequest`. El backend usa el `price_id` correspondiente para crear la
  Stripe Checkout Session. Hay que actualizar el contrato `identity-service.yaml`.

- **Campo broker en Step 5:** El selector de comisionista se mantiene en el UI
  como decorativo. Se deshabilitan todas las opciones de broker reales y solo
  se muestra "Sin comisionista asignado". No se incluye en el payload que se
  envía al backend (no forma parte de `RegisterRequest`).

- **Mercados en Step 5:** Las opciones LSE, TSE y ASX permanecen en el UI pero
  deshabilitadas. Solo NYSE y NASDAQ son seleccionables, en línea con el
  contrato que acepta únicamente `nyse | nasdaq`. **Selección única** (no múltiple):
  el usuario elige 1 mercado y 1 sector. El frontend envía arrays de 1 elemento
  al backend (`["nyse"]`, `["tech"]`).

- **Validación de edad:** El campo `birthDate` (Step 2) valida que el usuario
  tenga al menos 18 años. Error: "Debes tener al menos 18 años para registrarte".

- **Nacionalidad como desplegable:** El campo `nationality` en Step 2 es un
  `<Select>` con las mismas opciones de país, con etiquetas de gentilicio
  (ej. "Colombiana/o"). El valor almacenado es el código alpha-2 (ej. "CO").

- **Verificación de email en Step 1:** Al salir del campo de email (onBlur),
  el frontend llama a `GET /api/v1/auth/check-email?email=...`. Si el email ya
  existe en la BD, muestra "Este correo ya está registrado" inmediatamente sin
  esperar al envío final.

- **Fondeo en Alpaca:** Se realiza con `POST /v1/journals` usando
  `entry_type: JNLC` (Journal Cash) desde la cuenta sweep del broker hacia la
  cuenta del usuario recién creada. Monto: $50,000 USD virtuales. En sandbox
  se ejecuta inmediatamente. No usar `JNLS` (ese tipo es para mover acciones,
  no cash).

- **Validación de contraseña en backend:** El DTO `RegisterRequest` incluye
  `@Pattern` que exige mínimo 8 caracteres, al menos 1 mayúscula, 1 número y
  1 símbolo, igual que la validación Zod del frontend.
