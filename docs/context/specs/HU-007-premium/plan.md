# HU-07 — Plan de implementación

## Decisiones de sesión

- **Sin webhook de Stripe**: se reutiliza el patrón ya funcional de HU-01
  (registro → Stripe → `/payment/success?session_id=...` → `POST /confirm`).
  El endpoint `/confirm` ya existe y ya actualiza el plan a PREMIUM.
  No se implementa `POST /subscriptions/webhook` ni se requiere Stripe CLI.
- **Cancelación simplificada**: `POST /cancel` baja el plan a BASIC directamente
  en `identity.users` sin llamar a Stripe. No requiere entidad `Subscription` ni
  `stripeSubscriptionId`.
- **Sin entidad Subscription**: no se crea la tabla/entidad `identity.subscriptions`.
  El plan se gestiona directamente en `identity.users.plan`.
- **Sin stripeCustomerId**: no se agrega `stripe_customer_id` a `identity.users`.

---

## Backend — identity-service

### Archivos a crear

| Archivo | Contenido |
|---|---|
| `dto/request/CheckoutRequest.java` | `billingCycle: String` con `@NotBlank @Pattern(monthly\|annual)` |
| `dto/response/CheckoutResponse.java` | `checkoutUrl: String` (puede ser null si Stripe no está configurado) |

### Archivos a modificar

| Archivo | Cambio |
|---|---|
| `service/SubscriptionService.java` | Agregar `checkout(userId, billingCycle): CheckoutResponse` y `cancel(userId): void` |
| `service/impl/SubscriptionServiceImpl.java` | Implementar `checkout`: busca usuario por UUID, llama `createCheckoutSession(email, cycle)`, audita `SUBSCRIPTION_CREATED`. Implementar `cancel`: busca usuario, pone `plan = "BASIC"`, guarda, audita `SUBSCRIPTION_CANCELED` + `PLAN_CHANGED` |
| `controller/SubscriptionController.java` | Agregar `POST /checkout` y `POST /cancel`; userId desde `SecurityContextHolder` |
| `config/SecurityConfig.java` | Cambiar `permitAll` de `/api/v1/subscriptions/**` a solo `/api/v1/subscriptions/confirm`; los nuevos endpoints caen en `anyRequest().authenticated()` |

### Tests a escribir

| Archivo | Tests (5) |
|---|---|
| `test/.../service/SubscriptionServiceImplTest.java` (nuevo) | checkout retorna URL; checkout con Stripe placeholder retorna null en URL; checkout con usuario inexistente lanza excepción; cancel pone plan BASIC; cancel con usuario inexistente lanza excepción |
| `test/.../controller/SubscriptionControllerTest.java` (nuevo) | POST /checkout sin JWT → 401; POST /checkout con JWT válido → 200 con checkoutUrl; POST /cancel sin JWT → 401; POST /cancel con JWT plan BASIC ya → 200 |

---

## Frontend

### Archivos a crear

| Archivo | Contenido |
|---|---|
| `features/profile/services/subscriptionService.ts` | `checkout(billingCycle, token): Promise<string>` llama `POST /subscriptions/checkout`; `cancelSubscription(token): Promise<void>` llama `POST /subscriptions/cancel` |

### Archivos a modificar

| Archivo | Cambio |
|---|---|
| `features/profile/components/SubscriptionCard.tsx` | "Actualizar a Premium" abre modal con opción Mensual/Anual → llama `checkout` → `window.location.href = checkoutUrl`. "Cancelar suscripción" llama `cancelSubscription` → en éxito actualiza store a "basic" |

---

## Orden de implementación

1. `CheckoutRequest.java` + `CheckoutResponse.java`
2. `SubscriptionService.java` (interface)
3. `SubscriptionServiceImpl.java`
4. `SubscriptionController.java`
5. `SecurityConfig.java`
6. `SubscriptionServiceImplTest.java`
7. `SubscriptionControllerTest.java`
8. `subscriptionService.ts`
9. `SubscriptionCard.tsx`
