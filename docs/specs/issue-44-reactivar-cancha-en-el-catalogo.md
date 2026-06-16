# Issue #44: Reactivar cancha en el catálogo

## Título

Reactivar una cancha en el catálogo público.


## Nota de priorización post-MVP semana 13

Administración global de reactivación queda post-MVP semana 13, salvo regla mínima de volver a exponer reservas sólo si la cancha está activa y disponible.

## Prioridad

Alta

## Objetivo

Permitir que el administrador vuelva a habilitar una cancha previamente desactivada, restableciendo su estado administrativo y permitiendo su reaparición en el catálogo público solo cuando también cumpla la condición de publicación.

## Trazabilidad

- `FR34` El administrador podrá reactivar una cancha en el catálogo público.

## Relación con issues coordinados

- `#44` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#43` define la desactivación temporal o permanente de canchas.
- `#44` cubre la capacidad inversa de reactivación cuando el producto la permita.
- `#44` no redefine la información de la cancha; solo restituye su estado operativo en catálogo.

## Historia de usuario

Como administrador,
quiero reactivar una cancha previamente desactivada,
para restablecer su estado administrativo y permitir que vuelva a estar visible públicamente cuando corresponda.

## Alcance

- Permitir reactivar una cancha desactivada.
- Restaurar su estado administrativo activo.
- Mantener la información histórica y operativa existente.
- Reflejar el nuevo estado de la cancha dentro del panel administrativo.

## Fuera de alcance

- Crear una cancha nueva.
- Editar información completa de la cancha.
- Borrar historial de desactivación.
- Reactivar automáticamente canchas por procesos externos o masivos.

## Reglas de negocio

1. Solo el administrador puede reactivar una cancha.
2. Solo puede reactivarse una cancha que esté en estado administrativo `inactivo`.
3. La reactivación administrativa cambia únicamente el estado administrativo de la cancha de `inactivo` a `activo`.
4. La reactivación administrativa no modifica el estado de publicación de la cancha, cuyos únicos valores válidos son `borrador` y `publicada`.
5. El servidor debe priorizar el estado administrativo de la cancha sobre el estado de publicación al resolver visibilidad y disponibilidad pública.
6. Una cancha reactivada solo reaparece en el catálogo público si queda en estado administrativo `activo` y además su estado de publicación es `publicada`.
7. Una cancha reactivada sólo vuelve a exponer slots reservables si queda `activa`, visible y tiene disponibilidad configurada.
8. La reactivación no debe crear un nuevo registro de cancha; debe reutilizar el existente.
8. Si la cancha no existe, la operación no debe ejecutarse.

## Flujo principal

1. El administrador identifica una cancha desactivada.
2. El sistema muestra la opción de reactivación.
3. El administrador confirma la acción.
4. El sistema valida que la cancha se encuentra en estado administrativo `inactivo`.
5. El sistema actualiza el estado administrativo de la cancha a `activo`.
6. Si la cancha además tiene estado de publicación `publicada`, el sistema vuelve a mostrarla en el catálogo público.
7. El sistema informa el resultado de la operación.

## Casos alternos/validaciones

- Si la cancha no existe, el sistema debe informar que no puede completar la reactivación.
- Si la cancha ya está en estado administrativo `activo`, el sistema debe responder de forma idempotente informando que la cancha ya estaba activa.
- Si un usuario sin permisos administrativos intenta reactivar una cancha, el sistema debe bloquear la operación.
- Si la cancha tiene estado administrativo `activo` pero estado de publicación `borrador`, la reactivación no debe forzar su publicación ni mostrarla en catálogo.

## Datos de entrada

- Identidad del administrador autenticado.
- Identificador de la cancha.
- Estado administrativo actual de la cancha.
- Estado de publicación actual de la cancha.
- Confirmación administrativa de la acción.

## Datos de salida

- Estado administrativo actualizado de la cancha a `activo`.
- Confirmación de éxito o mensaje de error.
- Reaparición de la cancha en el catálogo público, cuando además tenga estado de publicación `publicada`.
- Disponibilidad de reserva utilizable nuevamente sólo cuando la cancha esté `activa`, visible y con horario configurado.

## Dependencias

- `#10` como épica contenedora.
- `#43` para la existencia del flujo de desactivación.
- Definición de los estados administrativos `activo` e `inactivo`.
- Definición de los estados de publicación `borrador` y `publicada`.
- Disponibilidad de catálogo público de canchas.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando reactiva una cancha en estado administrativo `inactivo`, entonces el sistema actualiza su estado administrativo a `activo`.
2. Dada una cancha con estado administrativo `inactivo` y estado de publicación `publicada`, cuando se reactiva, entonces puede volver a mostrarse en el catálogo público.
3. Dada una cancha con estado administrativo `inactivo` y estado de publicación `borrador`, cuando se reactiva, entonces no aparece en el catálogo porque la reactivación no fuerza su publicación.
4. Dada una cancha que ya está en estado administrativo `activo`, cuando se intenta reactivar, entonces el sistema responde de forma idempotente informando que la cancha ya estaba activa.
5. Dado un usuario sin permisos administrativos, cuando intenta reactivar una cancha, entonces el sistema bloquea la operación.
6. Dada una cancha inexistente, cuando se intenta reactivar, entonces el sistema informa que no puede completar la acción.
7. Dada una cancha reactivada y visible, cuando tiene disponibilidad configurada, entonces el sistema puede volver a exponer slots reservables para nuevas reservas.
8. Dado el alcance de esta historia, cuando se revisa el spec, entonces queda claro que la reactivación reutiliza el registro existente y no crea una cancha nueva.

## Definition of Done

- Existe capacidad administrativa para reactivar una cancha desactivada.
- La reactivación cambia el estado administrativo de la cancha a `activo`.
- Queda explícito que el estado administrativo tiene prioridad sobre el estado de publicación en servidor.
- La cancha reactivada vuelve a aparecer en el catálogo público solo si además está `publicada`.
- La operación está restringida al rol administrador.
- Se contemplan validaciones para canchas inexistentes o ya activas.
- La historia mantiene separación clara respecto a creación, edición y desactivación inicial.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/43
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#43
Current issue: TheMonstersP4/mejengueros-app#44