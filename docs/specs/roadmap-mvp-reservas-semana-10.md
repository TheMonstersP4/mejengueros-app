# Roadmap MVP de reservas — semana 10

## Fuente de verdad

La fuente operativa es el GitHub Project `TheMonstersP4 / Mejengueros`. Este documento resume la reorganización aprobada después del juicio de roadmap.

## Reglas de planificación

- Sprint 1 y Sprint 2 son excepciones de puntaje por directriz del curso.
- Desde Sprint 3 en adelante, cada sprint debe aproximarse a `25` puntos: cinco integrantes con aproximadamente `5` puntos cada uno.
- El MVP debe ser visible con datos dummy persistidos en base de datos al cierre de Sprint 4, alrededor del `2026-07-09`.
- Semana 10 / Sprint 5, alrededor del `2026-07-16`, se usa para estabilizar demo, reseña post-reserva y lista de verificación.
- Sprints 6-8, hasta el máximo `2026-08-06`, quedan para estabilización, integración, bugs y post-MVP selectivo.
- Los datos demo pueden ser dummy, pero deben estar conectados a la base de datos; no deben depender sólo de valores quemados en UI.

## Sprint 1 — Mockups y roadmap

Excepción de puntaje.

| Persona | Issue | Estimate |
|---|---|---:|
| Todos | `#45` Diseñar wireframes navegables de las pantallas MVP | `3` |

## Sprint 2 — Configuración técnica

Excepción de puntaje.

| Persona | Issue | Estimate |
|---|---|---:|
| Todos | `#53` Configurar entorno base del proyecto, ramas, backend, frontend, base de datos y autenticación para el MVP | `5` |

`#53` absorbe el trabajo transversal de ambiente: backend, frontend, base de datos, ramas, `.env.example`, migraciones, seed baseline y configuración mínima equivalente a `#24`/`#25` para habilitar `#23`.

## Sprint 3 — Desarrollo central base

| Persona | Issue | Estimate |
|---|---|---:|
| ShantyCerdasB | `#23` Registrar cuenta de usuario manualmente con correo y contraseña | `5` |
| CarlLRi | `#12` Definir ubicación geográfica y nombre del complejo/cancha | `5` |
| MaxwellChinchilla | `#48` Crear complejo deportivo y primera cancha | `5` |
| DanCasV27 | `#49` Configurar disponibilidad reservable de cancha | `5` |
| ddgutierrezc | `#29` Implementar esquema relacional mínimo MVP | `5` |

Total: `25` puntos.

## Sprint 4 — MVP visible con BD

| Persona | Issue | Estimate |
|---|---|---:|
| ShantyCerdasB | `#50` Reservar slot de cancha de 1 hora | `5` |
| CarlLRi | `#16` Ver detalle de cancha con disponibilidad | `5` |
| MaxwellChinchilla | `#15` Visualizar catálogo / búsqueda | `5` |
| DanCasV27 | `#14` Especificar servicios de complejo/cancha | `5` |
| ddgutierrezc | `#54` Preparar seed mínimo DB para demo visible del flujo MVP | `5` |

Total: `25` puntos.

Corte esperado: flujo visible catálogo -> detalle -> disponibilidad -> reserva con datos persistidos en BD.

## Sprint 5 — Demo semana 10

| Persona | Issue | Estimate |
|---|---|---:|
| ShantyCerdasB | `#51` Notificar reseña post-reserva | `5` |
| CarlLRi | `#21` Visualizar reseñas recibidas para dueño de cancha | `5` |
| MaxwellChinchilla | `#55` Validar flujo MVP end-to-end y errores críticos de reserva | `5` |
| DanCasV27 | `#17` Crear reseña desde reserva finalizada | `5` |
| ddgutierrezc | `#18` Calificar cancha con rating de 1 a 5 estrellas | `5` |

Total: `25` puntos.

`#52` queda en Sprint 5 sin estimación activa como apoyo de lista de verificación y datos demo finales.

## Issues reemplazadas / fuera de capacidad activa

- `#11` queda reemplazada por `#48`.
- `#13` queda reemplazada por `#49`.
- `#36` queda alcance descartado/post-MVP.
- `#46` queda alcance descartado/QR-obsoleto.

## Post-MVP fuera de Sprint 3-5

- `#26`, `#27` social login.
- `#30`, `#31` landing pages.
- `#34`, `#35`, `#37`, `#38` perfil/favoritos/foto.
- `#39`-`#44` admin global.
- `#19`, `#20`, `#22`, `#28` salvo decisión explícita de traerlos de vuelta.

## Cautelas aprobadas por juicio

- `#29` en `5` puntos sólo cubre el esquema mínimo MVP, no polish completo.
- `#18` en `5` puntos debe incluir comportamiento visible de rating, no sólo selector visual.
- `#21` debe quedarse como lectura simple de reseñas del dueño, sin moderación, analytics ni dashboards.
- Sprint 4 demuestra reserva visible con BD; Sprint 5 completa notificación/reseña/rating y estabilización de demo.
