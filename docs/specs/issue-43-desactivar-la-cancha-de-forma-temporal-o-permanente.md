# Issue #43: Desactivar la cancha de forma temporal o permanente

## Título

Desactivar una cancha sin borrar los datos históricos.


## Nota de priorización post-MVP semana 13

Administración global de desactivación queda post-MVP semana 13, salvo regla mínima de no exponer reservas de canchas inactivas.

## Prioridad

Alta

## Objetivo

Permitir que el administrador retire una cancha del uso público del sistema, conservando la información histórica asociada.

## Trazabilidad

- `FR33` El administrador podrá desactivar una cancha sin borrar los datos históricos.

## Relación con issues coordinados

- `#43` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#43` cubre la desactivación administrativa de canchas.
- `#44` cubre la reactivación posterior de una cancha en el catálogo.
- `#43` no implica borrado físico del registro ni de su historial.

## Historia de usuario

Como administrador,
quiero desactivar una cancha,
para retirarla del catálogo público sin perder sus datos históricos.

## Alcance

- Permitir desactivar una cancha desde una acción administrativa.
- Cambiar el estado administrativo de una cancha de `activo` a `inactivo`.
- Retirar la cancha del catálogo público cuando quede desactivada.
- Conservar los datos históricos de la cancha.

## Fuera de alcance

- Borrado físico de la cancha y su historial.
- Edición completa de los datos de la cancha.
- Reactivación de la cancha; eso pertenece a `#44`.
- Moderación avanzada, auditoría o flujos de apelación.

## Reglas de negocio

1. Solo el administrador puede desactivar una cancha.
2. La desactivación administrativa cambia únicamente el estado administrativo de la cancha de `activo` a `inactivo`.
3. La desactivación administrativa no modifica el estado de publicación de la cancha, cuyos únicos valores válidos son `borrador` y `publicada`.
4. El servidor debe priorizar el estado administrativo de la cancha sobre el estado de publicación al resolver visibilidad y disponibilidad pública.
5. Una cancha con estado administrativo `inactivo` no debe aparecer en catálogo ni en vistas públicas, incluso si su estado de publicación es `publicada`.
6. Una cancha con estado administrativo `inactivo` no debe exponer nuevos slots reservables ni habilitar reseñas nuevas sin una reserva finalizada válida.
7. La desactivación no debe borrar datos históricos relacionados con la cancha.
7. Si la cancha no existe, la operación no debe aplicarse.

## Flujo principal

1. El administrador identifica la cancha a gestionar.
2. El sistema muestra la opción de desactivación.
3. El administrador confirma la desactivación de la cancha.
4. El sistema valida la operación.
5. El sistema cambia el estado administrativo de la cancha a `inactivo`.
6. El sistema deja de mostrar la cancha en el catálogo público.
7. El sistema informa el resultado de la acción.

## Casos alternos/validaciones

- Si la cancha no existe, el sistema debe informar que no puede completar la operación.
- Si un usuario sin permisos administrativos intenta desactivar una cancha, el sistema debe bloquear la operación.
- Si la cancha ya se encuentra en estado administrativo `inactivo`, el sistema debe responder de forma idempotente informando que la cancha ya estaba inactiva.

## Datos de entrada

- Identidad del administrador autenticado.
- Identificador de la cancha.
- Confirmación de la acción administrativa.

## Datos de salida

- Estado actualizado de la cancha.
- Confirmación de éxito o mensaje de error.
- Exclusión de la cancha del catálogo público.
- Disponibilidad de reservas deshabilitada mientras la cancha esté inactiva.
- Conservación del historial asociado.

## Dependencias

- `#10` como épica contenedora.
- Existencia de canchas registradas en el sistema.
- Definición de los estados administrativos `activo` e `inactivo`.
- Definición de los estados de publicación `borrador` y `publicada`.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando desactiva una cancha existente, entonces el sistema cambia su estado administrativo a `inactivo` y la retira del catálogo público.
2. Dada una cancha desactivada, cuando se revisa su información histórica, entonces los datos asociados continúan conservándose.
3. Dado un usuario sin permisos administrativos, cuando intenta desactivar una cancha, entonces el sistema bloquea la operación.
4. Dada una cancha inexistente, cuando se intenta desactivar, entonces el sistema informa que no puede completar la acción.
5. Dado una cancha con estado de publicación `publicada`, cuando el administrador la marca como `inactivo`, entonces el servidor prioriza el estado administrativo y la cancha deja de mostrarse públicamente.
6. Dada una cancha desactivada, cuando un usuario intenta reservar un slot nuevo, entonces el sistema debe bloquear la operación mientras la cancha permanezca inactiva.
7. Dado el alcance de esta historia, cuando se revisa el spec, entonces queda claro que la desactivación no implica borrado físico de datos.

## Definition of Done

- Existe capacidad administrativa para desactivar canchas.
- La desactivación cambia el estado administrativo de la cancha a `inactivo`.
- Queda explícito que el estado administrativo tiene prioridad sobre el estado de publicación en servidor.
- La cancha desactivada deja de aparecer en el catálogo público.
- Los datos históricos de la cancha se conservan.
- La historia mantiene separación clara respecto a reactivación y borrado físico.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/42
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#42
Current issue: TheMonstersP4/mejengueros-app#43