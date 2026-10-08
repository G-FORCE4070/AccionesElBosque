# HU-07 — Tasks

## Backend — DTOs

- [x] T-01: Crear `dto/request/CheckoutRequest.java` — campo `billingCycle: String` con `@NotBlank` y `@Pattern(regexp = "monthly|annual")`
- [x] T-02: Crear `dto/response/CheckoutResponse.java` — campo `checkoutUrl: String`

## Backend — Service

- [x] T-03: Actualizar `service/SubscriptionService.java` — agregar firmas `checkout(String userId, String billingCycle): CheckoutResponse` y `cancel(String userId): void`
- [x] T-04: Actualizar `service/impl/SubscriptionServiceImpl.java` — implementar `checkout`: busca usuario por UUID, llama `stripeCheckoutService.createCheckoutSession(email, billingCycle)`, audita `SUBSCRIPTION_CREATED`, retorna `CheckoutResponse`. Implementar `cancel`: busca usuario, pone `plan = "BASIC"`, guarda, audita `SUBSCRIPTION_CANCELED` + `PLAN_CHANGED`

## Backend — Controller y Seguridad

- [x] T-05: Actualizar `controller/SubscriptionController.java` — agregar `POST /checkout` (lee userId del `SecurityContextHolder`, llama `checkout`) y `POST /cancel` (lee userId del `SecurityContextHolder`, llama `cancel`)
- [x] T-06: Actualizar `config/SecurityConfig.java` — reemplazar `permitAll` de `/api/v1/subscriptions/**` por solo `/api/v1/subscriptions/confirm`; los endpoints `/checkout` y `/cancel` quedan protegidos por `anyRequest().authenticated()`

## Backend — Tests

- [x] T-07: Crear `test/.../service/SubscriptionServiceImplTest.java` — 5 tests unitarios con Mockito: checkout retorna URL; checkout con Stripe placeholder retorna null en checkoutUrl; checkout con usuario inexistente lanza excepción; cancel pone plan BASIC y guarda; cancel con usuario inexistente lanza excepción
- [x] T-08: Crear `test/.../controller/SubscriptionControllerTest.java` — 4 tests de integración: POST /checkout sin JWT → 401; POST /checkout con JWT válido → 200 con checkoutUrl; POST /cancel sin JWT → 401; POST /cancel con JWT → 200

## Frontend

- [x] T-09: Crear `features/profile/services/subscriptionService.ts` — funciones `checkout(billingCycle, token)` y `cancelSubscription(token)` con Bearer token
- [x] T-10: Actualizar `features/profile/components/SubscriptionCard.tsx` — botón "Actualizar a Premium" abre modal con opciones Mensual/Anual → llama `checkout` → redirige a Stripe. Botón "Cancelar suscripción" llama `cancelSubscription` → actualiza store a "basic" en éxito

## Documentación

- [x] T-11: Actualizar `README.md` — quitar requisito Stripe CLI y paso "stripe listen"
- [x] T-12: Actualizar `STATUS.md`
- [x] T-13: Actualizar `docs/context/changelog.md`
