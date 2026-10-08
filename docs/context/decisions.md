# decisions.md — Decisiones Arquitectónicas

## ADR-01: Service-Based Architecture

**Decisión:** Usar Service-Based Architecture con 5 servicios organizados por dominio.

**Por qué no microservicios:**
Cada microservicio normalmente maneja su propia base de datos. Cuando una
operación necesita trabajar con varios datos al mismo tiempo — por ejemplo,
registrar un usuario, crear su cuenta en Alpaca y guardar sus preferencias —
la implementación se vuelve más compleja. Para este MVP preferimos una
arquitectura más simple de administrar y desarrollar.

**Por qué no un monolito:**
Si todo estuviera en una sola aplicación, una falla en un módulo podría
afectar el resto del sistema. Con servicios separados, si fallan las
notificaciones, el trading puede seguir funcionando.

Referencia: Richards & Ford, Fundamentals of Software Architecture, cap. 13.

---

## ADR-02: Base de datos compartida con schemas separados

**Decisión:** Una sola instancia PostgreSQL con 4 schemas separados
(`identity`, `trading`, `markets`, `notification`).
Elasticsearch para audit-service.

**Regla:** cada servicio lee y escribe solo en su propio schema.
Si necesita datos de otro dominio, los solicita por REST.

**Razón:** Una sola base de datos es más simple de administrar para el MVP.
Los schemas permiten separar los datos de cada servicio aunque compartan
el mismo servidor PostgreSQL.

Referencia: Richards & Ford, Fundamentals of Software Architecture, cap. 13.

---

## ADR-03: Sin API Gateway

**Decisión:** El frontend accede directamente a los servicios.

**Razón:** En la forma básica de Service-Based Architecture, el frontend
puede consumir directamente los servicios. Para este MVP solo existe un
frontend consumidor y cada servicio maneja su propia autenticación.
Agregar un API Gateway añadía más configuración e infraestructura sin
resolver una necesidad real del proyecto actual.

Referencia: Richards & Ford, Fundamentals of Software Architecture, cap. 13.

---

## ADR-04: JWT con claims de role, plan y alpacaAccountId

**Decisión:** El JWT incluye `sub`, `role`, `plan` y `alpacaAccountId`.

**Por qué `role`:** permite que cada servicio sepa qué permisos tiene
el usuario sin hacer consultas adicionales.

**Por qué `plan`:** permite saber si el usuario es BASIC o PREMIUM sin
consultar a identity-service en cada request. Si el usuario cambia de plan,
debe iniciar sesión nuevamente para recibir un nuevo JWT actualizado.

**Por qué `alpacaAccountId`:** el ID de la cuenta de Alpaca se asigna
una sola vez y no cambia. Guardarlo en el JWT evita consultas adicionales
a identity-service durante las órdenes.

**Por qué `brokerId` NO está en el JWT:** el broker asignado sí puede
cambiar durante una sesión activa. Para evitar usar datos desactualizados,
trading-service lo consulta directamente cuando lo necesita.

---

## ADR-05: @Async + Spring Retry en lugar de un sistema de mensajería

**Decisión:** Comunicación asíncrona usando `@Async` + Spring Retry.
Si falla una operación, se reintenta hasta 3 veces con 1 segundo de espera.

**Razón:** Herramientas como Kafka añaden más infraestructura y complejidad
de configuración. Para el MVP, `@Async` + Spring Retry cubre las necesidades
del sistema de forma más simple.

**Deuda técnica:** en producción podría reemplazarse por Kafka para manejar
mejor grandes volúmenes de eventos.

---

## ADR-06: trading-service construye el portafolio completo

**Decisión:** trading-service consulta internamente a markets-service y
devuelve el portafolio ya calculado. El frontend consume un solo endpoint.

**Razón:** Los cálculos de negocio — valor actual, ganancias y pérdidas —
deben mantenerse en el backend y no en el frontend. Esto simplifica el
cliente y centraliza la lógica del sistema.

---

## ADR-07: identity-service envía el código MFA directamente

**Decisión:** identity-service envía directamente el código MFA por email,
sin pasar por notification-service.

**Razón:** El MFA hace parte del proceso de autenticación y es un flujo
crítico del sistema. Hacer que dependa de otro servicio añadiría una
dependencia innecesaria durante el login. Además, si notification-service
tuviera problemas, los usuarios no podrían autenticarse.

---

## ADR-08: Saldo local en trading-service

**Decisión:** trading-service mantiene una copia local del saldo del usuario
y la actualiza después de cada orden usando la respuesta de Alpaca.

**Razón:** Esto permite validar fondos suficientes sin consultar a Alpaca
en cada operación, reduciendo tiempos de respuesta y dependencia de un
servicio externo.

**Consecuencia:** el saldo local puede diferir ligeramente del saldo de
Alpaca porque algunas comisiones solo se manejan localmente. El frontend
siempre muestra el saldo calculado por trading-service.

**Deuda técnica:** en producción se podrían realizar sincronizaciones
periódicas con Alpaca para corregir diferencias.

---

## ADR-09: Endpoints internos sin JWT

**Decisión:** Los endpoints internos de notification-service y audit-service
no requieren JWT.

**Razón:** Estos endpoints solo son usados por otros servicios dentro de la
red interna de Docker. Agregar autenticación entre servicios añadía más
complejidad para el alcance actual del MVP.

**Deuda técnica:** en producción se implementarían mecanismos de
autenticación entre servicios.

---

## ADR-10: El portafolio responde aunque los precios no estén disponibles

**Decisión:** Si markets-service falla o no responde, el endpoint de
portafolio devuelve igualmente las posiciones del usuario y marca los
precios como no disponibles.

**Razón:** El usuario todavía puede ver cuántas acciones posee y sus
precios de compra aunque los precios en tiempo real no estén disponibles.
Esto evita bloquear completamente la funcionalidad.

---

## ADR-11: El registro Premium crea primero una cuenta Basic

**Decisión:** El registro siempre crea inicialmente una cuenta BASIC.
Si el usuario selecciona Premium, el sistema genera la URL de pago de
Stripe y el frontend redirige al usuario al checkout.

Solo después de que Stripe confirma el pago, la cuenta se actualiza
a PREMIUM.

**Razón:** El sistema no debe activar beneficios Premium antes de confirmar
el pago exitoso.

---

## ADR-12: La orden espera la confirmación de Alpaca con fallback a precio estimado

**Decisión:** Los endpoints de compra y venta intentan ejecutar la orden en
Alpaca y esperan el fill. Si Alpaca rechaza (p.ej. cuenta sin fondos aún —
ACH en proceso) o falla por error de red, el sistema usa el precio estimado
(obtenido de Alpaca Market Data API) como precio ejecutado.

La respuesta incluye el precio real de ejecución si Alpaca lo confirma,
o el precio estimado si no.

**Razón:** El fondeo ACH en sandbox tarda ~60s–horas en reflejarse en el
buying power de Alpaca. Sin el fallback, las órdenes fallarían con 500
durante esa ventana. Con él, el sistema es funcional desde el primer momento.
Una vez que el ACH liquida, las órdenes pasan automáticamente por Alpaca.

**Consecuencia:** hay una ventana donde el precio ejecutado es estimado.
Para un MVP académico con paper trading esto es aceptable.

**Deuda técnica:** en producción no existiría esta ventana porque el fondeo
sería inmediato (wire transfer, ACH mismo día, etc.).