# Issue #15: Visualizar catálogo de canchas publicadas y activas

## Título

Buscar complejos/canchas y acceder al flujo de reserva.

## Nota de re-alcance MVP

El catálogo deja de ser sólo informativo. Para semana 10 debe ser el inicio del flujo del `mejenguero`: buscar, ver detalle y reservar un slot de 1 hora.

## Objetivo

Permitir que los `mejengueros` descubran complejos/canchas visibles, consulten información básica, rating y servicios, y entren al detalle para reservar.

## Relación con issues coordinados

- `#12` define identidad/ubicación del complejo.
- `#14` define servicios de complejo/cancha.
- `#16` muestra el detalle con disponibilidad.
- `#49` genera disponibilidad reservable.
- `#50` crea reservas.

## Historia de usuario

Como `mejenguero`,
quiero buscar canchas disponibles,
para elegir una y reservar un slot de 1 hora.

## Alcance

- Mostrar complejos/canchas visibles y activas.
- Permitir búsqueda básica por texto, provincia y cantón.
- Mostrar rating/calificación visible cuando exista.
- Mostrar servicios relevantes a nivel de complejo/cancha.
- Enlazar al detalle y reserva.

## Fuera de alcance

- Pagos o pasarela.
- Cantidad de participantes.
- Geolocalización avanzada o `near me`.
- Ranking complejo.
- Reservas de duración distinta a 1 hora.

## Reglas de negocio

1. El catálogo sólo muestra canchas asociadas a complejos visibles y activos.
2. Una cancha sin disponibilidad configurada puede mostrarse sólo si el detalle comunica que no hay slots reservables.
3. El catálogo debe orientar al flujo `buscar -> ver detalle -> reservar`.
4. Rating y servicios ayudan a decidir, pero no reemplazan la disponibilidad.
5. El catálogo no debe mostrar pagos ni cantidad de participantes.

## Criterios de aceptación

1. Dado un mejenguero que abre el catálogo, cuando existen canchas visibles, entonces el sistema muestra resultados con información básica.
2. Dado un filtro por provincia o cantón, cuando existen coincidencias, entonces el sistema muestra resultados filtrados.
3. Dado un resultado del catálogo, cuando el usuario lo selecciona, entonces navega al detalle de `#16`.
4. Dado el alcance semana 10, cuando se revisa el catálogo, entonces queda conectado al flujo de reserva y no sólo a información estática.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El catálogo muestra complejos/canchas visibles.
- El catálogo permite llegar al detalle y reserva.
- El catálogo conserva búsqueda básica por texto/provincia/cantón.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/14
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#14
Current issue: TheMonstersP4/mejengueros-app#15
