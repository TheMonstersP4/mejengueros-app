# Issue #11: Registrar nueva cancha (Dueño)

## Estado de alcance

`reemplazada` / `post-MVP`.

## Motivo

Esta historia queda reemplazada por `#48 Crear complejo deportivo y primera cancha`. Por directriz de la profesora, el flujo vigente ya no crea una cancha en `borrador` ni usa incorporación largo; el MVP crea primero un `Complejo` y luego una `Cancha` asociada en un flujo rápido.

## Trazabilidad

- `#11` conserva la historia original de creación de cancha.
- `#48` es la historia activa para implementación del flujo `Complejo` -> `Cancha`.
- `#12` conserva la identidad/ubicación del complejo.
- `#14` conserva servicios de complejo/cancha.
- `#49` conserva disponibilidad reservable.

## Alcance descartado del flujo activo

- Crear una cancha en estado `borrador`.
- Incorporación por bloques obligatorios.
- Publicación automática posterior por completitud de bloques.
- State machine o ciclo de vida de publicación para semana 10.

## Reemplazo funcional

El reemplazo funcional es:

1. El `dueño` crea un `Complejo`.
2. El `dueño` registra una o más `Canchas` dentro del complejo.
3. Las canchas reciben servicios y disponibilidad reservable.
4. El flujo evita estados intermedios complejos y valida antes de guardar.

## Definition of Done

- La issue queda fuera de capacidad activa Sprint 3-5.
- La issue conserva trazabilidad histórica.
- La implementación activa queda en `#48`.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/10
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#10
Current issue: TheMonstersP4/mejengueros-app#11
