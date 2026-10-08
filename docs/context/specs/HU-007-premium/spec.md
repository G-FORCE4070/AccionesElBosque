# HU-07 — Suscripción Premium

## Historia de usuario

**Como** inversionista con plan BASIC,
**quiero** suscribirme al plan Premium mediante pago con Stripe,
**para** acceder a funcionalidades exclusivas como watchlist y alertas.

---

## Prioridad y estimación

| Campo      | Valor                                    |
|------------|------------------------------------------|
| Prioridad  | Must                                     |
| Estimación | 5 puntos                                 |
| Épica      | Gestión de Perfil y Suscripción          |
| Feature    | Suscripción                              |
| Servicio   | identity-service                         |

---

## Referencias

- `docs/context/architecture.md` — identity-service, Stripe, ADR-11 (registro crea BASIC primero)
- `docs/context/domain.md` — eventos: `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_CANCELED`, `PLAN_CHANGED`
- `docs/context/decisions.md` — ADR-05 (@Async), ADR-11
- `docs/context/contracts/identity-service.yaml` — `POST /api/v1/subscriptions/checkout`, `POST /api/v1/subscriptions/webhook`, `POST /api/v1/subscriptions/cancel`

---

## Precios

| Plan    | Precio    | `billingCycle` | Variable de entorno                    |
|---------|-----------|----------------|----------------------------------------|
| Premium | $12/mes   | `monthly`      | `STRIPE_PREMIUM_MONTHLY_PRICE_ID`      |
| Premium | $120/año  | `annual`       | `STRIPE_PREMIUM_ANNUAL_PRICE_ID`       |

---

## Criterios de aceptación

### CA-01: Inicio del flujo de suscripción

**Dado que** el inversionista tiene plan BASIC y un JWT válido
**Cuando** llama a `POST /api/v1/subscriptions/checkout` con `{ "billingCycle": "monthly" | "annual" }`
**Entonces** el sistema crea una Stripe Checkout Session usando el `price_id` correspondiente
**Y** responde con HTTP 200 y cuerpo:
```json
{ "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_..." }
```
**Y** el frontend redirige al usuario a esa URL

---

### CA-02: Pago exitoso — webhook de Stripe

**Dado que** el usuario completó el pago en Stripe
**Cuando** Stripe envía el evento `checkout.session.completed` a `POST /api/v1/subscriptions/webhook`
**Entonces** el sistema valida la firma del webhook con `STRIPE_WEBHOOK_SECRET`
**Y** actualiza el plan del usuario a `PREMIUM` en `identity.users`
**Y** guarda la suscripción activa en `identity.subscriptions` (con `stripeSubscriptionId`, `billingCycle`, `currentPeriodEnd`)
**Y** responde con HTTP 200
**Y** registra los eventos `SUBSCRIPTION_CREATED` y `PLAN_CHANGED` vía `@Async`
**Y** el frontend (en la pantalla de espera) detecta el cambio y muestra:
```
"Tu plan ha sido actualizado a Premium. Vuelve a iniciar sesión para continuar."
```
**Y** el frontend fuerza el logout (limpia JWT) y redirige a `/login`

---

### CA-03: Pago fallido o cancelado

**Dado que** el usuario abandonó el checkout o el pago fue rechazado
**Cuando** el usuario regresa a la plataforma
**Entonces** el plan del usuario permanece BASIC
**Y** el frontend muestra un mensaje informativo:
```
"El pago no fue completado. Tu plan sigue siendo Basic."
```

---

### CA-04: Cancelación de suscripción

**Dado que** el inversionista tiene plan PREMIUM y un JWT válido
**Cuando** llama a `POST /api/v1/subscriptions/cancel`
**Entonces** el sistema cancela la suscripción en Stripe (al final del período actual)
**Y** actualiza `identity.subscriptions` marcando la suscripción como cancelada (`cancelAtPeriodEnd: true`)
**Y** responde con HTTP 200
**Y** el plan del usuario permanece PREMIUM hasta `currentPeriodEnd`
**Y** registra el evento `SUBSCRIPTION_CANCELED` vía `@Async`
**Y** cuando Stripe envía `customer.subscription.deleted` (al vencer el período), el plan se actualiza a BASIC
**Y** registra el evento `PLAN_CHANGED` vía `@Async`

---

## Definition of Done

- [ ] Endpoint `POST /api/v1/subscriptions/checkout` implementado con JWT requerido
- [ ] Usa `STRIPE_PREMIUM_MONTHLY_PRICE_ID` o `STRIPE_PREMIUM_ANNUAL_PRICE_ID` según `billingCycle`
- [ ] Stripe Checkout Session creada con `successUrl` y `cancelUrl` configuradas en `application.yaml`
- [ ] Endpoint `POST /api/v1/subscriptions/webhook` implementado sin JWT, valida firma Stripe con `STRIPE_WEBHOOK_SECRET`
- [ ] Maneja eventos `checkout.session.completed` y `customer.subscription.deleted`
- [ ] Endpoint `POST /api/v1/subscriptions/cancel` implementado con JWT requerido
- [ ] Cancela la suscripción en Stripe con `cancelAtPeriodEnd: true`
- [ ] Tabla `identity.subscriptions` con campos: `userId`, `stripeSubscriptionId`, `stripeCustomerId`, `billingCycle`, `status`, `currentPeriodEnd`, `cancelAtPeriodEnd`
- [ ] Probado con tarjetas de prueba de Stripe (`4242 4242 4242 4242` para pago exitoso)
- [ ] Stripe CLI o test de webhook en local para probar el webhook
- [ ] Eventos `SUBSCRIPTION_CREATED`, `SUBSCRIPTION_CANCELED`, `PLAN_CHANGED` registrados vía `@Async` + `@Retryable`
- [ ] Pantalla de suscripción en el frontend con botones "Mensual ($12/mes)" y "Anual ($120/año)"
- [ ] Página de retorno del checkout muestra estado del pago
- [ ] Tests unitarios para `StripeCheckoutServiceImpl` cubriendo: crear checkout, procesar webhook exitoso, cancelar suscripción
- [ ] Tests de integración con MockMvc (mock de Stripe SDK)
- [ ] El código compila y pasa `mvn test` sin errores

---

## Notas técnicas

- Tras el pago exitoso, el JWT del usuario aún contiene `plan: BASIC`. El nuevo JWT con `plan: PREMIUM`
  solo se genera al hacer login nuevamente (ADR-04). Por eso el frontend redirige al login tras el pago.

---

## Aclaraciones de sesión

- **Sin webhook de Stripe**: se reutiliza el patrón redirect-to-confirm ya implementado en HU-01.
  El flujo es: checkout → Stripe → `/payment/success?session_id=...` → `POST /confirm` (ya existe).
  No se implementa `POST /subscriptions/webhook`. No se requiere Stripe CLI.
- **Cancelación simplificada**: `POST /subscriptions/cancel` baja el plan a BASIC directamente en `identity.users`
  sin llamar a Stripe. No se procesan reembolsos en el MVP.
- **Sin entidad Subscription**: no se crea la tabla/entidad `identity.subscriptions` en este MVP.
- **Sin stripeCustomerId**: no se agrega `stripe_customer_id` a `identity.users` en este MVP.
- **Definition of Done ajustado**: se eliminan los ítems de webhook, Stripe CLI, entidad subscriptions y
  stripeCustomerId. Los ítems vigentes son los endpoints `/checkout` y `/cancel`, los tests, el frontend
  y los eventos de auditoría.
