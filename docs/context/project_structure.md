# project_structure.md — Estructura del Repositorio

```
aeb-trading/                          ← raíz del monorepo
├── CLAUDE.md                         ← instrucciones para Claude Code
├── STATUS.md                         ← estado actual del proyecto
├── README.md                         ← descripción del proyecto
├── .env.example                      ← variables de entorno de ejemplo
├── .gitignore
├── docker-compose.yml                ← levanta backend + infraestructura
│
├── .github/
│   └── workflows/
│       └── ci.yml                    ← GitHub Actions: build + SonarCloud
│
├── db/
│   └── init.sql                      ← crea schemas y datos iniciales
│
├── docs/
│   ├── context/
│   │   ├── architecture.md
│   │   ├── decisions.md
│   │   ├── domain.md
│   │   ├── tech_stack.md
│   │   ├── coding_style.md
│   │   ├── data-model.md
│   │   ├── project_structure.md      ← este archivo
│   │   ├── technical_debt.md
│   │   ├── contracts/
│   │   │   ├── identity-service.yaml
│   │   │   ├── trading-service.yaml
│   │   │   ├── markets-service.yaml
│   │   │   ├── notification-service.yaml
│   │   │   └── audit-service.yaml
│   │   └── specs/
│   │       └── HU-XX/
│   │           ├── spec.md
│   │           ├── plan.md           ← generado por Claude Code
│   │           └── tasks.md          ← generado por Claude Code
│   └── diagrams/
│
├── backend/
│   ├── pom.xml                       ← POM padre (agrega los 5 módulos)
│   ├── identity-service/
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/co/edu/unbosque/acciones_elbosque/identity_service/
│   │       │   │   ├── controller/
│   │       │   │   ├── service/
│   │       │   │   │   └── impl/
│   │       │   │   ├── repository/
│   │       │   │   ├── model/        ← entidades JPA (@Entity)
│   │       │   │   ├── dto/
│   │       │   │   │   ├── request/
│   │       │   │   │   └── response/
│   │       │   │   ├── config/
│   │       │   │   ├── security/
│   │       │   │   └── exception/
│   │       │   └── resources/
│   │       │       └── application.yml
│   │       └── test/
│   ├── trading-service/              ← misma estructura interna
│   ├── markets-service/              ← misma estructura interna
│   ├── notification-service/         ← misma estructura interna
│   └── audit-service/                ← misma estructura interna
│
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    ├── tsconfig.json
    ├── .env.local                    ← VITE_* URLs de cada servicio (no subir al repo)
    └── src/
        ├── App.tsx
        ├── components/
        │   └── ui/
        ├── pages/
        ├── stores/                   ← Zustand stores
        ├── services/                 ← llamadas HTTP al backend
        └── types/
```

---

## Notas

**Base de datos:** `db/init.sql` crea los 4 schemas de PostgreSQL, las tablas iniciales y los
datos iniciales (acciones y horarios de mercado) cuando Docker Compose
levanta el contenedor. Spring Boot crea y actualiza las tablas con
`ddl-auto=update`.

**audit-service** no usa PostgreSQL. Almacena los eventos directamente
en Elasticsearch.

**Frontend:** no está en Docker Compose. Se ejecuta por separado con
`npm run dev` para aprovechar la recarga automática durante el desarrollo.
Las URLs de los servicios van en `frontend/.env.local` como variables
`VITE_*`.

**Monorepo:** `backend/pom.xml` es el POM padre que lista los 5 servicios
como módulos Maven. El CI ejecuta el análisis de SonarCloud desde `backend/`.