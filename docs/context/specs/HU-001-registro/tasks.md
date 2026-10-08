# Tasks — HU-01: Registro de inversionista

## Bloque 0 — Contrato

- [x] **T-00** Agregar `billingCycle: monthly | annual` (opcional) a `RegisterRequest` en `identity-service.yaml`

---

## Bloque 1 — Backend: configuración base

- [x] **T-01** Agregar dependencias faltantes en `backend/identity-service/pom.xml`
      (spring-security, jjwt, cache/caffeine, mail, stripe-java, spring-retry, testcontainers)
- [x] **T-02** Completar `application.yaml` con datasource, JPA schema, JWT, mail, Stripe, Alpaca y Caffeine

---

## Bloque 2 — Backend: modelo y repositorios

- [x] **T-03** Crear `model/User.java` (entidad JPA → `identity.users`)
- [x] **T-04** Crear `model/UserPreference.java` (entidad JPA → `identity.user_preferences`)
- [x] **T-05** Crear `repository/UserRepository.java`
- [x] **T-06** Crear `repository/UserPreferenceRepository.java`

---

## Bloque 3 — Backend: DTOs y excepciones

- [x] **T-07** Crear `dto/request/RegisterRequest.java` con todas las anotaciones de validación
- [x] **T-08** Crear `dto/response/RegisterResponse.java`
- [x] **T-09** Crear `exception/EmailAlreadyRegisteredException.java`
- [x] **T-10** Crear `exception/AlpacaAccountCreationException.java`
- [x] **T-11** Crear `exception/GlobalExceptionHandler.java`

---

## Bloque 4 — Backend: configuraciones Spring

- [x] **T-12** Crear `config/PasswordConfig.java` (BCryptPasswordEncoder bean)
- [x] **T-13** Crear `config/CorsConfig.java`
- [x] **T-14** Crear `config/AsyncConfig.java` (@EnableAsync, @EnableRetry, ThreadPoolTaskExecutor)
- [x] **T-15** Crear `config/AlpacaClientConfig.java` (RestClient con credenciales Alpaca)
- [x] **T-16** Crear `config/StripeConfig.java` (Stripe.apiKey en @PostConstruct)
- [x] **T-17** Crear `config/SecurityConfig.java` (rutas públicas, deshabilitar CSRF para API)

---

## Bloque 5 — Backend: servicios

- [x] **T-18** Crear `service/AuditService.java` (interfaz)
- [x] **T-19** Crear `service/impl/AuditServiceImpl.java` (@Async + @Retryable, POST a audit-service)
- [x] **T-20** Crear `service/RegisterService.java` (interfaz)
- [x] **T-21** Crear `service/impl/RegisterServiceImpl.java` (flujo completo con @Transactional)
      + `service/AlpacaBrokerService.java` + `service/impl/AlpacaBrokerServiceImpl.java`
      + `service/StripeCheckoutService.java` + `service/impl/StripeCheckoutServiceImpl.java`

---

## Bloque 6 — Backend: controlador

- [x] **T-22** Crear `controller/AuthController.java` (POST /api/v1/auth/register → 201)

---

## Bloque 7 — Backend: tests

- [x] **T-23** Escribir `RegisterServiceImplTest.java` (4 casos unitarios con Mockito)
- [x] **T-24** Escribir `AuthControllerTest.java` (5 casos de integración con MockMvc + Testcontainers)

---

## Bloque 8 — Frontend

- [x] **T-25** Modificar `registerSchema.ts`: agregar `billingCycle` en step6Schema, eliminar `broker` de step5Schema
- [x] **T-26** Modificar `Step5Preferences.tsx`: deshabilitar LSE/TSE/ASX; broker decorativo
- [x] **T-27** Modificar `Step6Plan.tsx`: selector mensual/anual para Premium; redirección correcta post-submit
- [x] **T-28** Modificar `registerService.ts`: reemplazar mock por llamada real a POST /api/v1/auth/register

---

## Bloque 8b — Correcciones post-pruebas (sesión fix/cors-stripe-alpaca-env)

- [x] **T-F1** Corregir autenticación Alpaca: OAuth2 Client Credentials con Bearer token cacheado (`AlpacaTokenService`)
- [x] **T-F2** Agregar `tax_id` y `tax_id_type` (COL_CC) al payload de creación de cuenta Alpaca
- [x] **T-F3** Convertir `country` de alpha-2 a alpha-3 (CO → COL) en payload Alpaca
- [x] **T-F4** Normalizar campos de texto a ASCII antes de enviar a Alpaca (`Normalizer.NFD`)
- [x] **T-F5** Hacer el fondeo de cuenta Alpaca no-fatal (try/catch con warning)
- [x] **T-F6** Agregar `GET /api/v1/auth/check-email` para verificación de email disponible
- [x] **T-F7** Validación de edad mínima 18 años en `step2Schema` (Zod refine)
- [x] **T-F8** Convertir campo `nationality` de texto libre a Select dropdown
- [x] **T-F9** Cambiar mercados y sectores a selección única (string en lugar de string[])
- [x] **T-F10** Deshabilitar SMS, WhatsApp y reporte mensual en Step 5 (próximamente)
- [x] **T-F11** Verificación de email en Step 1 con debounce (llama `check-email`, muestra error inmediato)
- [x] **T-F12** CORS fix: agregar `.cors(Customizer.withDefaults())` a `SecurityConfig` para que Spring Security delegue preflights OPTIONS al `CorsConfig`

---

## Bloque 9 — Confirmación de pago Stripe (sin CLI)

- [x] **T-29** Actualizar `application.yaml`: success-url con `{CHECKOUT_SESSION_ID}`; cancel-url a `/register`
- [x] **T-30** Agregar `getCustomerEmailIfPaid(sessionId)` a `StripeCheckoutService` e implementar en `StripeCheckoutServiceImpl`
- [x] **T-31** Crear `PaymentNotCompletedException`, `ConfirmPaymentRequest`, `ConfirmPaymentResponse`
- [x] **T-32** Crear `SubscriptionService` + `SubscriptionServiceImpl`: verifica sesión, actualiza plan a PREMIUM, audita `PLAN_UPGRADED`
- [x] **T-33** Crear `SubscriptionController`: `POST /api/v1/subscriptions/confirm`
- [x] **T-34** Actualizar `SecurityConfig`: `/api/v1/subscriptions/**` público; `GlobalExceptionHandler`: handler 402 para `PaymentNotCompletedException`
- [x] **T-35** Crear `PaymentSuccessPage.tsx`: lee `session_id`, llama confirm, muestra estados loading/success/error
- [x] **T-36** Registrar ruta `/payment/success` en `App.tsx`; agregar `confirmPayment()` a `registerService.ts`
