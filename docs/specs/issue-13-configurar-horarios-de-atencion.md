# Issue #13: Configurar horarios de atención

## Estado de alcance

`reemplazada` / `post-MVP`.

## Motivo

Esta historia queda reemplazada por `#49 Configurar disponibilidad reservable de cancha`. Por directriz de la profesora, los horarios ya no son sólo información de atención ni incorporación; ahora generan slots reservables exactos de 1 hora.

## Trazabilidad

- `#13` conserva la historia original de horarios de atención.
- `#49` es la historia activa para implementación de disponibilidad reservable.
- `#50` consume los slots generados para crear reservas.
- `#29` modela horarios, slots y reservas.

## Alcance descartado del flujo activo

- Horarios sólo informativos.
- Estado `borrador` u incorporación por bloques.
- Definición de 7 días con abierto/cerrado individual como requisito activo.
- Publicación automática por completar horarios.
- Horarios sin propósito de reserva.

## Reemplazo funcional

El reemplazo funcional es:

1. El `dueño` selecciona días disponibles.
2. El `dueño` define un rango horario.
3. El sistema genera slots exactos de 1 hora para los días seleccionados.
4. Días no seleccionados se interpretan como cerrados/no disponibles.
5. La app no gestiona feriados ni excepciones especiales.

## Definition of Done

- La issue queda fuera de capacidad activa Sprint 3-5.
- La issue conserva trazabilidad histórica.
- La implementación activa queda en `#49`.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/12
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#12
Current issue: TheMonstersP4/mejengueros-app#13
