# mejengueros-app

Repositorio oficial de desarrollo de **Mejengueros**, el proyecto del equipo TheMonstersP4. Este repositorio concentra la implementación del backend, la primera base de aplicación móvil/frontend y la documentación operativa que conecta el backlog del curso con GitHub Projects.

## Estado actual

El proyecto está en etapa de integración inicial para el MVP. La prioridad actual es consolidar la base técnica que permita avanzar sobre reservas, canchas, disponibilidad, catálogo y flujo de demo.

La fuente operativa de seguimiento es el GitHub Project del equipo:

- Organización: [`TheMonstersP4`](https://github.com/orgs/TheMonstersP4)
- Project: [`Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1)
- Repositorio vinculado a issues: [`TheMonstersP4/mejengueros-app`](https://github.com/TheMonstersP4/mejengueros-app)
- Milestone vigente: [`Semana 10 — Flujo MVP`](https://github.com/TheMonstersP4/mejengueros-app/milestone/1)

## Estructura principal

| Ruta | Propósito |
|------|-----------|
| `app-backend/` | Backend, infraestructura y despliegue. |
| `app-frontend/` | Primera base de aplicación móvil/frontend del proyecto. |
| `docs/design/` | Artefactos visuales y técnicos de diseño, como mockups y diagramas de base de datos. |
| `openspec/` | Cambios formales SDD/OpenSpec cuando el equipo use ese flujo. |
| `.github/` | Workflows y configuración de automatización del repositorio. |

## Guías internas por subproyecto

Antes de trabajar dentro de cada área, revisa su README interno:

| Área | Cuándo leerlo | Referencia |
|------|---------------|------------|
| Backend | Si vas a tocar API, infraestructura, despliegue o el POC web. | [`app-backend/README.md`](app-backend/README.md) |
| Frontend | Si vas a ejecutar la app KMP, revisar arquitectura o usar los comandos de desarrollo. | [`app-frontend/README.md`](app-frontend/README.md) |
| Diseño | Si necesitas contexto funcional, mockups o diagramas de soporte. | [`docs/design/README.md`](docs/design/README.md) |

## Cómo trabajar por área

### Backend

El backend vive en `app-backend/`.

- `app-backend/api`: API NestJS con Fastify, Prisma y lambdas WebSocket.
- `app-backend/infra`: Terraform de AWS, Cognito, API Gateway, S3, ECR y Lambdas.
- `.github`: workflows y scripts de despliegue ajustados para ejecutar el backend desde `app-backend`.

Para más detalles técnicos, revisa [`app-backend/README.md`](app-backend/README.md).

### Aplicación móvil/frontend

La primera plantilla de aplicación móvil vive en `app-frontend/`.

Esta base funciona como punto de partida técnico para el frontend del MVP. Puede incluir código de ejemplo, pantallas de muestra o flujos temporales propios de una plantilla. Ese contenido debe entenderse como scaffolding inicial, no como alcance funcional final del producto.

El objetivo de esta integración inicial es dejar disponible una base revisable para que el equipo pueda:

- ejecutar y validar el proyecto frontend;
- revisar la estructura técnica inicial;
- conectar progresivamente los flujos reales de Mejengueros;
- reemplazar ejemplos de plantilla por casos del dominio del proyecto.

Para trabajar el frontend, ejecuta los comandos desde `app-frontend/`. La superficie recomendada es el `Taskfile.yml` de ese subproyecto (`task check`, `task test`, `task verify`, `task spotless:apply`, `task spotless:check`) y los comandos crudos de Gradle quedan como fallback cuando Task no esté disponible.

Para más detalles técnicos, revisa [`app-frontend/README.md`](app-frontend/README.md).

### Diseño y documentación funcional

Los artefactos visuales y técnicos de diseño viven en `docs/design/`. Esa carpeta concentra referencias como mockups, diagramas y decisiones visuales o estructurales que ayudan a entender el producto antes de tocar código.

La guía de entrada de esa carpeta está en [`docs/design/README.md`](docs/design/README.md).

## Seguimiento del trabajo

El seguimiento operativo del proyecto se realiza en [GitHub Projects](https://github.com/orgs/TheMonstersP4/projects/1) e [issues](https://github.com/TheMonstersP4/mejengueros-app/issues). Ahí deben vivir las tareas, prioridades, decisiones de alcance y vínculos entre trabajo pendiente y Pull Requests.

La documentación versionada del repositorio debe complementar ese seguimiento, no reemplazarlo. En general:

| Espacio | Responsabilidad |
|---------|-----------------|
| [GitHub Project `Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1) | Backlog operativo, prioridad, estado, estimación y trazabilidad del MVP. |
| [Issues del repositorio](https://github.com/TheMonstersP4/mejengueros-app/issues) | Descripción del trabajo, criterios de aceptación y discusión antes del Pull Request. |
| [`docs/design/`](docs/design/) | Mockups, diagramas y artefactos de diseño. |
| [`openspec/`](openspec/) | Especificaciones formales de cambios cuando se use SDD/OpenSpec. |
| Pull requests | Cambios implementables vinculados a un issue o decisión aceptada por el equipo. |

## Flujo de ramas y trabajo paralelo

Cada cambio debe partir de `main` en una rama propia y enfocada. Si necesitas trabajar en más de una feature a la vez, no reutilices el mismo directorio ni mezcles cambios sin relación: usa `git worktree` para mantener cada feature aislada.

Ruta recomendada para trabajo paralelo:

```powershell
git switch main
git pull
git worktree add ..\mejengueros-app-<feature> -b <tipo>/<descripcion> main
```

Ejemplo:

```powershell
git worktree add ..\mejengueros-app-reservas -b feat/reservas main
```

Reglas prácticas:

- una rama por issue, fix o feature;
- cada worktree debe apuntar a una rama distinta;
- antes de abrir un Pull Request, verifica que el cambio esté vinculado a un issue aprobado;
- no mezcles documentación, infraestructura, backend y frontend en una misma rama salvo que formen parte del mismo cambio revisable.

## Lecturas recomendadas

- `app-backend/README.md`: guía técnica del backend, API, infraestructura y despliegue.
- `app-frontend/README.md`: guía técnica del frontend y comandos de trabajo.
- `docs/design/README.md`: referencias visuales y técnicas de diseño.

## Convención editorial

La documentación funcional del proyecto se escribe en español claro, con tildes y `ñ` cuando corresponda: por ejemplo, `contraseña`, `autenticación`, `configuración`, `sesión`, `catálogo`, `métricas` y `reseña`.

Los identificadores técnicos pueden mantenerse en ASCII cuando la implementación lo requiera, como nombres de variables, campos JSON, rutas, endpoints, archivos, variables de entorno o claves de configuración.

También se permiten términos técnicos ampliamente aceptados cuando sean más claros que una traducción forzada, como `frontend`, `backend`, `seed`, `wireframes`, `roadmap`, `backlog`, `Social Login`, `end-to-end`, `MVP`, `QR` y `post-MVP`.

## Alcance vigente del MVP

El backlog activo del MVP se consulta en el [GitHub Project `Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1) y en el milestone [`Semana 10 — Flujo MVP`](https://github.com/TheMonstersP4/mejengueros-app/milestone/1). A nivel de producto, el camino crítico se concentra en:

- configuración técnica del proyecto;
- complejo, cancha, servicios y disponibilidad reservable;
- catálogo, detalle y reserva de slots de 1 hora;
- seed mínimo en base de datos para demo visible;
- notificación post-reserva;
- reseña básica, rating visible y lectura simple de reseñas por el dueño.

Quedan fuera del camino crítico inicial funcionalidades como social login, landing pages, perfil/favoritos/foto, admin global, QR/código, imágenes de reseña, cuestionarios y métricas avanzadas, salvo que el equipo las reprograme explícitamente en el [Project](https://github.com/orgs/TheMonstersP4/projects/1).
