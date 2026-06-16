# Jerarquía del backlog — GitHub Project Mejengueros

## Fuente de verdad actual

La fuente de verdad vigente es el GitHub Project [`TheMonstersP4 / Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1), vinculado al repositorio [`TheMonstersP4/mejengueros-app`](https://github.com/TheMonstersP4/mejengueros-app).

Después de la migración desde el repositorio anterior, la numeración local debe seguir la numeración actual del repositorio de la organización.

## Criterio

El backlog se organiza con issues padre de tipo épica/contenedor y subissues implementables.

- Las épicas/contenedores tienen una descripción breve y no deberían duplicar estimaciones de sus subissues.
- Las historias implementables o HU conservan specs funcionales detalladas.
- La jerarquía real se refleja con subissues de GitHub para que el Project pueda mostrar `Parent issue` / progreso de subissues.
- Las issues cerradas por decisión de alcance se mantienen sólo como referencia histórica cuando todavía aparecen en el Project.

## Jerarquía actual configurada

| Padre actual | Tipo | Subissues actuales |
|---:|---|---|
| `#3` Gestión de Canchas | Épica contenedora | `#11`, `#12`, `#13`, `#14`, `#15`, `#16` |
| `#4` Sistema de Reseñas y Calificaciones | Épica contenedora | `#17`, `#18`, `#19`, `#20`, `#21`, `#22`, `#46` |
| `#5` Seguridad y Autenticación | Épica contenedora | `#23`, `#24`, `#25`, `#26`, `#27` |
| `#6` Infraestructura Técnica | Épica contenedora | `#2`, `#28`, `#29` |
| `#7` Landing page del producto y el equipo | Épica contenedora | `#30`, `#31` |
| `#8` Control de calidad del producto | Cerrada / no planificada | `#32`, `#33` cerradas / no planificadas |
| `#9` Perfil del Usuario y Fidelización | Épica contenedora | `#34`, `#35`, `#36`, `#37`, `#38` |
| `#10` Panel de Administración y Control Global | Épica contenedora / post-MVP semana 13 | `#39`, `#40`, `#41`, `#42`, `#43`, `#44` |
| `#47` Sistema de reservas MVP | Épica contenedora | `#48`, `#49`, `#50`, `#51`, `#52` |

## Notas sobre favoritos

`#38` queda dentro del bloque `#9` porque su valor de producto es fidelización/perfil del usuario: guardar una cancha para volver a encontrarla.

Relaciones funcionales importantes:

- `#15` define qué canchas puede descubrir y ver el mejenguero.
- `#16` define la visualización de la cancha individual.
- `#38` define marcar/desmarcar favorita desde esa visualización.
- `#35` visualiza la lista de favoritas guardadas; si ofrece quitar una cancha, debe invocar la acción de `#38` sin redefinirla.
- `#34` es issue hermano dentro de actividad personal del usuario, pero no dependencia directa.

## Decisión sobre calidad transversal

`#8`, `#32` y `#33` fueron cerradas por decisión de alcance: la prueba unitaria mínima queda como Definition of Done de cada issue implementable, no como épica/subissues transversales.

## Estado de sincronización local

- Los archivos `specs/issue-*.md` fueron regenerados desde los cuerpos actuales de las issues del repositorio `TheMonstersP4/mejengueros-app`.
- La numeración local ahora usa los números actuales del GitHub Project.
- Las referencias históricas como `Migrated from` u `Original issue` dentro de cuerpos importados deben tratarse como trazabilidad, no como dependencias vigentes.

## Advertencia operativa

`gh project item-list` puede no mostrar inmediatamente el campo `Parent issue`, aunque la relación exista en GitHub Issues. La verificación confiable se hace consultando la relación `parent` / `subIssues` vía GraphQL de Issues o comparando contra los cuerpos actuales del Project.
