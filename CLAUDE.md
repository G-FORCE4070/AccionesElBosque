# CLAUDE.md — Acciones ElBosque

## Contexto del proyecto

Plataforma de Day Trading universitaria (Universidad El Bosque, Colombia).
Lee **todos** estos documentos antes de cualquier tarea:

- `docs/context/architecture.md`
- `docs/context/domain.md`
- `docs/context/tech_stack.md`
- `docs/context/coding_style.md`
- `docs/context/decisions.md`
- `docs/context/technical_debt.md`
- `docs/context/contracts/`

Revisa también `STATUS.md` y `docs/context/changelog.md` al inicio de cada sesión para saber qué está hecho y qué decisiones se tomaron.

---

## Flujo obligatorio por HU

### 1. Leer el spec

Lee `docs/context/specs/HU-XXX/spec.md` completo.

Antes de continuar, pregúntame:
- Cualquier cosa que no esté clara.
- Cualquier contradicción con otro documento.
- Cualquier decisión que no esté documentada.

Si durante la conversación aclaramos algo que no estaba en el spec,
muéstrame el texto exacto que agregarías. Si lo apruebo, añádelo al
final del `spec.md` bajo esta sección:

```markdown
## Aclaraciones de sesión
- [aclaración acordada]
```

### 2. Generar plan.md

Genera `docs/context/specs/HU-XXX/plan.md` con:
- Qué archivos vas a crear o modificar (backend y frontend).
- En qué orden.
- Qué tests vas a escribir por cada archivo.

Espera que yo diga **"aprobado"** antes de continuar.

### 3. Generar tasks.md

Genera `docs/context/specs/HU-XXX/tasks.md` con las tareas
desglosadas del plan, ordenadas, con casillas de verificación.

Espera que yo diga **"aprobado"** antes de continuar.

### 4. Implementar tarea por tarea

Antes de ejecutar cada tarea individual, descríbela brevemente y
espera mi aprobación. Al terminar cada una, marca su casilla en
`tasks.md`.

### 5. Tests

Al terminar todas las tareas ejecuta `mvn test` dentro del servicio.
Reporta cuántos tests pasaron y cuántos fallaron.
Si alguno falla, corrígelo antes de continuar.

### 6. Changelog

Actualiza `docs/context/changelog.md`.
Agrega una nueva entrada al inicio del archivo con la fecha, el número de HU y:
- Decisiones que se desviaron del plan (tabla Elemento / Plan original / Cambio / Razón).
- Correcciones a specs o contratos.
- Lista de archivos creados o modificados (backend y frontend por separado).

Sigue el formato ya existente en el archivo.

### 7. Git

Cuando todo esté listo dime:
- Nombre de la rama: `feat/HU-XX-nombre-corto`
- Archivos que modificaste.
- Mensaje del commit: `feat(HU-XX): descripción breve en inglés`

---

## Base de datos

- **Una sola instancia PostgreSQL** con cuatro schemas separados:
  `identity`, `trading`, `markets`, `notification`.
- Los schemas y datos iniciales se crean con `db/init.sql` al levantar Docker Compose.
- Cada servicio usa `spring.jpa.hibernate.ddl-auto=update` y apunta
  a su propio schema vía `spring.jpa.properties.hibernate.default_schema`.
- **Regla estricta:** cada servicio solo lee y escribe en su propio schema.
  Si necesita datos de otro dominio, los pide por REST.

---

## Frontend

El frontend está en `frontend/`. Usa React + TypeScript + Vite.
El stack completo está documentado en `tech_stack.md`.

---

## Restricciones

- No modifiques archivos fuera de los servicios que indica el spec.
- No cambies el schema de BD de otro servicio.
- No agregues dependencias Maven no documentadas en `tech_stack.md`.
- No instales paquetes npm no documentados sin pedirme aprobación.
- No empieces ningún paso sin mi aprobación del anterior.
- Si algo no está documentado, pregunta antes de asumir.

---

## Idioma

| Contexto | Idioma |
|---|---|
| Tus respuestas | Español |
| Código (clases, métodos, variables) | Inglés |
| Comentarios en código | Inglés |
| Endpoints REST | Inglés |
| Mensajes de error al usuario final | Español |
| Eventos de auditoría | Inglés SCREAMING_SNAKE_CASE |
| Commits | Inglés |

---

## Convención de ramas y commits

**Rama:** `feat/HU-XX-descripcion-corta` (máx. 5 palabras, guiones, minúsculas)

**Commit:** `feat(HU-XX): descripción breve en inglés`

Tipos de prefijo: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Ejemplos:**
```
feat/HU-01-user-registration
feat(HU-01): implement investor registration with Alpaca account creation

fix/HU-03-mfa-expiry
fix(HU-03): handle expired MFA code on service restart
```