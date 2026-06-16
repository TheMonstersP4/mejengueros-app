# Triage de estimaciones — issues asignadas a ddgutierrezc

## Fuente de verdad actual

La fuente de verdad vigente es el GitHub Project `TheMonstersP4 / Mejengueros`, vinculado al repositorio `TheMonstersP4/mejengueros-app`.

Este documento fue actualizado después de la migración desde el repositorio anterior. La numeración usada abajo corresponde a la numeración actual del Project.

> Nota de replanificación: el roadmap operativo vigente para el MVP de reservas y semana 10 está resumido en `specs/roadmap-mvp-reservas-semana-10.md`. Las tablas históricas de triage por persona se conservan como trazabilidad y no deben usarse para contradecir la asignación Sprint 3-5 aprobada.

## Contexto

La estimación se hizo con valores `1`, `3` y `5`, considerando que:

- el equipo tiene poca o ninguna experiencia práctica con Kotlin Multiplatform;
- el equipo tiene poca o ninguna experiencia práctica con NestJS;
- el equipo tiene poca o ninguna experiencia práctica escribiendo pruebas unitarias en NestJS y KMP;
- las estimaciones deben evitar subestimar el costo de aprendizaje, integración, pruebas y coordinación;
- las issues padre, épicas o contenedores no deben duplicar el esfuerzo ya estimado en sus subissues.

## Regla de estimación usada

- `1`: tarea pequeña, regla puntual, coordinación de historia padre o trabajo de bajo alcance si las dependencias ya existen.
- `3`: historia implementable de alcance medio, con integración acotada, prueba unitaria mínima o complejidad funcional moderada.
- `5`: historia compleja para el MVP por involucrar UI + servidor, persistencia, validaciones, autorización, filtros, estados vacíos, composición de datos, pruebas unitarias en tecnologías poco conocidas o alta incertidumbre técnica.

## Regla para issues padre y subissues

Si una issue funciona principalmente como épica, contenedor o descripción padre, no debe recibir la suma completa del esfuerzo de sus subissues.

En esos casos:

- se deja sin estimación si no es implementable por sí misma;
- o se estima como `1` si representa coordinación mínima, contrato general o cierre conceptual;
- se sube a `3` si además exige una prueba unitaria mínima o alguna implementación concreta no cubierta completamente por sus subissues;
- sólo se estima mayor a `3` cuando todavía contiene trabajo implementable real significativo no cubierto por sus subissues.

## Criterio adicional de pruebas unitarias

Todas las historias implementables deben incluir en su Definition of Done al menos una prueba unitaria asociada al comportamiento principal del issue.

La prueba puede vivir en:

- servidor NestJS, si el comportamiento principal está en API, dominio, caso de uso, validación o filtro/interceptor;
- KMP, si el comportamiento principal está en UI state, ViewModel, validación cliente o lógica compartida;
- la capa que corresponda según la implementación real.

## Estimaciones históricas en el Project

| Issue | Título | Estimate | Criterio |
|---:|---|---:|---|
| `#3` | Gestión de Canchas | Sin estimar | Épica/contenedor sin cuerpo implementable propio. |
| `#4` | Sistema de Reseñas y Calificaciones | Sin estimar | Épica/contenedor sin cuerpo implementable propio. |
| `#14` | Especificar servicios (Parqueo, Iluminación, Tipo de césped) | `5` | Catálogo cerrado, validaciones, integración con publicabilidad y prueba unitaria en stack poco conocido. |
| `#18` | Calificar cancha con rating de 1 a 5 estrellas | `5` | Componente/interacción, validación, integración con reseña y prueba unitaria en KMP/NestJS. |
| `#27` | Iniciar sesión de forma rápida mediante Social Login con Outlook (Microsoft) | `5` | OAuth federado Microsoft/Entra, callbacks, tokens Cognito, frontend/API y pruebas. |
| `#29` | Implementar esquema relacional de datos | `5` | Modelo Prisma/PostgreSQL, migración inicial, relaciones, claves únicas, índices, repositorios/mappers y pruebas. |
| `#38` | Agregar una cancha como favorita | `3` | Mutación acotada de favorito, marcar/desmarcar, autenticación, duplicados, persistencia y prueba unitaria. |
| `#39` | Visualizar listado maestro de usuarios | `5` | Listado administrativo, autorización, estados vacíos, consumo de usuarios y prueba unitaria. |
| `#52` | Completar datos demo y lista de verificación semana 10 | Sin estimar | Soporte de Sprint 5 sin capacidad activa; el seed mínimo estimado vive en `#54`. |

## Total estimado histórico

Esta tabla se conserva como trazabilidad y no representa la capacidad activa vigente. Para Sprint 3-5 rige `specs/roadmap-mvp-reservas-semana-10.md`, con `5` puntos por miembro y sprint.

`#3` y `#4` no cuentan en el total porque quedaron sin estimar como épicas/contenedores.

## Nota de migración

En el repositorio anterior, varias de estas historias tenían otros números. Este documento ya no usa la numeración anterior; la numeración vigente es la del Project actual en `TheMonstersP4/mejengueros-app`.
