# mejengueros-app

Repositorio oficial de desarrollo de **Mejengueros**, el proyecto del equipo TheMonstersP4. Este repositorio concentra la implementación del backend, la primera base de aplicación móvil/frontend y la documentación operativa que conecta el backlog del curso con GitHub Projects.

## Estado actual

El proyecto está en etapa de integración inicial para el MVP. La prioridad actual es consolidar la base técnica que permita avanzar sobre reservas, canchas, disponibilidad, catálogo y flujo de demo.

La fuente operativa de seguimiento es el GitHub Project del equipo:

- Organización: [`TheMonstersP4`](https://github.com/orgs/TheMonstersP4)
- Project: [`Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1)
- Repositorio vinculado a issues: [`TheMonstersP4/mejengueros-app`](https://github.com/TheMonstersP4/mejengueros-app)

## Estructura principal

| Ruta | Propósito |
|------|-----------|
| `app-backend/` | Backend, infraestructura y despliegue. |
| `app-frontend/` | Primera base de aplicación móvil/frontend del proyecto. |
| `docs/specs/` | Specs funcionales, roadmap y notas de planificación sincronizadas con issues y GitHub Projects. |
| `.github/` | Workflows y configuración de automatización del repositorio. |

## Backend

El backend vive en `app-backend/`.

- `app-backend/api`: API NestJS con Fastify, Prisma y lambdas WebSocket.
- `app-backend/infra`: Terraform de AWS, Cognito, API Gateway, S3, ECR y Lambdas.
- `.github`: workflows y scripts de despliegue ajustados para ejecutar el backend desde `app-backend`.

## Aplicación móvil/frontend

La primera plantilla de aplicación móvil vive en `app-frontend/`.

Esta base funciona como punto de partida técnico para el frontend del MVP. Puede incluir código de ejemplo, pantallas de muestra o flujos temporales propios de una plantilla. Ese contenido debe entenderse como scaffolding inicial, no como alcance funcional final del producto.

El objetivo de esta integración inicial es dejar disponible una base revisable para que el equipo pueda:

- ejecutar y validar el proyecto frontend;
- revisar la estructura técnica inicial;
- conectar progresivamente los flujos reales de Mejengueros;
- reemplazar ejemplos de plantilla por casos del dominio del proyecto.

## Specs, backlog, OpenSpec y Engram

La carpeta `docs/specs/` contiene documentación funcional y de planificación usada para mantener trazabilidad entre decisiones del curso, issues y GitHub Projects.

Estas specs sí deben versionarse en este repositorio porque explican el porqué del backlog y permiten revisar cambios funcionales junto con el código. Lo importante es no confundir responsabilidades: `docs/specs/` es documentación funcional del curso y del Project, no una instalación OpenSpec estándar.

Si el equipo activa un flujo SDD/OpenSpec formal, debe vivir en `openspec/` con su propia estructura de cambios, specs delta, diseño y tareas. Engram puede usarse como memoria persistente complementaria para conservar decisiones, contexto entre sesiones y aprendizajes que no necesariamente pertenecen al repo.

La convención recomendada es:

| Herramienta | Responsabilidad |
|-------------|-----------------|
| GitHub Projects e issues | Seguimiento operativo del trabajo. |
| `docs/specs/` | Contexto funcional, specs por issue, criterios, jerarquía, roadmap y decisiones de alcance del curso. |
| `openspec/` | Especificaciones formales de cambios cuando se use SDD/OpenSpec real. |
| Engram | Memoria persistente de decisiones, contexto entre sesiones y aprendizajes del equipo. |
| Pull requests | Cambios implementables vinculados a un issue aprobado o aceptado por el equipo. |

## Convención editorial

La documentación funcional del proyecto se escribe en español claro, con tildes y `ñ` cuando corresponda: por ejemplo, `contraseña`, `autenticación`, `configuración`, `sesión`, `catálogo`, `métricas` y `reseña`.

Los identificadores técnicos pueden mantenerse en ASCII cuando la implementación lo requiera, como nombres de variables, campos JSON, rutas, endpoints, archivos, variables de entorno o claves de configuración.

También se permiten términos técnicos ampliamente aceptados cuando sean más claros que una traducción forzada, como `frontend`, `backend`, `seed`, `wireframes`, `roadmap`, `backlog`, `Social Login`, `end-to-end`, `MVP`, `QR` y `post-MVP`.

## Alcance vigente del MVP

El backlog activo se concentra en:

- configuración técnica del proyecto;
- complejo, cancha, servicios y disponibilidad reservable;
- catálogo, detalle y reserva de slots de 1 hora;
- seed mínimo en base de datos para demo visible;
- notificación post-reserva;
- reseña básica, rating visible y lectura simple de reseñas por el dueño.

Quedan fuera del camino crítico inicial funcionalidades como social login, landing pages, perfil/favoritos/foto, admin global, QR/código, imágenes de reseña, cuestionarios y métricas avanzadas, salvo que el equipo las reprograme explícitamente en el Project.
