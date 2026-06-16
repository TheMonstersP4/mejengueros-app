# Triage de estimaciones — ShantyCerdasB y DanCasV27

## Fuente de verdad actual

La fuente de verdad vigente es el GitHub Project `TheMonstersP4 / Mejengueros`, vinculado al repositorio `TheMonstersP4/mejengueros-app`.

Este documento fue actualizado después de la migración desde el repositorio anterior. La numeración usada abajo corresponde a la numeración actual del Project.

> Nota de replanificación: el roadmap operativo vigente para el MVP de reservas y semana 10 está resumido en `specs/roadmap-mvp-reservas-semana-10.md`. Las tablas históricas de triage por persona se conservan como trazabilidad y no deben usarse para contradecir la asignación Sprint 3-5 aprobada.

## Contexto

La estimación usa la escala acordada por el equipo:

- `1`: un día de trabajo;
- `3`: media semana;
- `5`: una semana completa.

El criterio considera que:

- el equipo tiene poca o ninguna experiencia práctica con Kotlin Multiplatform;
- el equipo tiene poca o ninguna experiencia práctica con NestJS;
- el equipo no tiene margen para absorber la curva de aprendizaje sin reflejarla en la estimación;
- las pruebas unitarias son parte del Definition of Done de cada issue implementable;
- las épicas o contenedores no deben duplicar el esfuerzo ya estimado en sus subissues.

## Regla para épicas/contenedores

Si una issue funciona principalmente como épica, contenedor o descripción padre:

- se deja sin estimación si no tiene carga implementable propia;
- se estima como `1` sólo si conserva una carga técnica simple, coordinación mínima o cierre conceptual concreto;
- no se suma el esfuerzo de sus subissues.

## Estimaciones históricas — ShantyCerdasB

| Issue | Título | Estimate | Criterio |
|---:|---|---:|---|
| `#5` | Seguridad y Autenticación | Sin estimar | Épica/contenedor de autenticación. Agrupa `#23`–`#27`; estimarla duplicaría el esfuerzo de sus subissues. |
| `#6` | Infraestructura Técnica | Sin estimar | Épica/contenedor técnico. Agrupa `#2`, `#28` y `#29`; no tiene carga implementable propia fuera de coordinación. |
| `#20` | Responder cuestionario breve obligatorio de experiencia | `5` | Catálogo cerrado, respuestas obligatorias, validación, integración con reseña y prueba unitaria. |
| `#23` | Registrar cuenta de usuario manualmente con correo y contraseña | `5` | Registro manual, validaciones de credenciales, integración Cognito/API/cliente y prueba unitaria. |
| `#31` | Implementar el landing page del equipo The Monsters | `3` | Landing acotada, principalmente cliente/contenido, con riesgo moderado por KMP y validación visual. |
| `#35` | Visualizar lista de canchas favoritas | `3` | Lista autenticada de favoritos propios, navegación a detalle, estado vacío, posible remoción vía `#38` y prueba unitaria. |
| `#40` | Modificar datos de cuenta de usuario | `5` | Edición administrativa de cuenta/roles, autorización, validaciones, persistencia y prueba unitaria. |
| `#42` | Filtrar usuarios por rol o estado | `3` | Filtros administrativos sobre listado existente, estados vacíos, autorización y prueba unitaria. |
| `#44` | Reactivar cancha en el catálogo | `5` | Reactivación administrativa de cancha con reglas de visibilidad, estado administrativo/publicación, validaciones, autorización y prueba unitaria. Normalizada a la escala acordada `1/3/5`. |
| `#46` | Ver y regenerar código QR de validación para reseñas de cancha | `3` | Alcance descartado por re-alcance MVP: la validación de reseñas se reemplaza por reserva finalizada y notificación post-reserva. |
| `#48` | Crear complejo deportivo y primera cancha | `5` | Flujo MVP de dueño: crear complejo como entidad raíz y primera cancha sin estado `borrador`; coordina el cambio de modelo `Complejo` -> `Cancha`. |

Total actual ShantyCerdasB según Project, excluyendo contenedores sin estimar y considerando `#46` como alcance descartado: `34` puntos.

## Estimaciones históricas — DanCasV27

| Issue | Título | Estimate | Criterio |
|---:|---|---:|---|
| `#2` | Estandarizar contrato global de respuestas del API | `5` | Cambio transversal NestJS: envelope, interceptor, filtro de errores, metadata, pruebas y documentación. |
| `#8` | Control de calidad del producto | Sin estimar | Issue cerrada como no planificada. Las pruebas unitarias quedan como DoD de cada issue implementable. |
| `#9` | Perfil del Usuario y Fidelización | Sin estimar | Épica contenedora de `#34`–`#38`; no representa una tarea técnica única. |
| `#13` | Configurar horarios de atención | `5` | Modelado semanal, validaciones de rangos, completitud, reglas de publicación y prueba unitaria. |
| `#17` | Crear reseña de cancha con rating, comentario y cuestionario | `5` | Flujo padre con persistencia, integración de bloques, validaciones y prueba unitaria. |
| `#22` | Capturar métricas estructuradas de experiencia | `5` | Modelado/semántica de respuestas estructuradas, validación, persistencia y prueba unitaria. |
| `#26` | Iniciar sesión de forma rápida mediante Social Login con Google | `5` | OAuth federado con Google, Cognito, Hosted UI, callback, sesión cliente, API y pruebas. |
| `#32` | Implementar las pruebas unitarias de backend | Sin estimar | Issue cerrada como no planificada; testing backend queda en el DoD de cada historia que toque backend. |
| `#37` | Subir foto de perfil del usuario | `3` | Integración acotada con imágenes/S3 de `#28`, validación tipo/tamaño, asociación al perfil, reemplazo, errores y prueba unitaria. |
| `#45` | Diseñar wireframes navegables de las pantallas MVP | `3` | Sprint 1 es excepción de puntaje; todos los miembros participan aunque el Project conserve una única estimación baja. |
| `#51` | Notificar reseña post-reserva | `5` | Notificación interna persistida para abrir flujo de reseña después de una reserva finalizada; activa en Sprint 5 dentro del roadmap semana 10. |

Total histórico DanCasV27 de esta tabla: no se usa para capacidad activa. Para Sprint 3-5 rige `specs/roadmap-mvp-reservas-semana-10.md`, con `5` puntos por miembro y sprint.

## Nota de migración

En el repositorio anterior, varias de estas historias tenían otros números y otras asignaciones. Este documento refleja el estado actual del Project después de la migración al repositorio de la organización.
