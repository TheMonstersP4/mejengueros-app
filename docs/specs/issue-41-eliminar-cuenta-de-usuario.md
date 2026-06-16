# Issue #41: Eliminar cuenta de usuario

## Título

Eliminar cuenta de usuario mediante borrado lógico.


## Nota de priorización post-MVP semana 13

Eliminación lógica de usuarios por admin queda post-MVP semana 13; no bloquea flujo demo de reserva.

## Prioridad

Alta

## Objetivo

Permitir que el administrador elimine una cuenta de usuario de forma lógica para retirar su acceso operativo sin perder los datos históricos que deban conservarse dentro del sistema.

## Trazabilidad

- `FR31` El administrador podrá eliminar (borrado lógico) la cuenta de un usuario.

## Relación con issues coordinados

- `#41` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#39` provee la vista base para identificar la cuenta a gestionar.
- `#40` cubre modificación de datos, no eliminación.
- `#41` se enfoca en borrado lógico y conservación histórica.
- `#42` se relaciona porque una cuenta eliminada lógicamente debe poder reflejarse por estado.

## Historia de usuario

Como administrador,
quiero eliminar una cuenta de usuario mediante borrado lógico,
para desactivar su uso sin perder la trazabilidad histórica asociada.

## Alcance

- Permitir que un administrador marque una cuenta en estado `inactivo`.
- Retirar la cuenta de su estado operativo normal.
- Preservar los datos históricos que no deben perderse.
- Reflejar el nuevo estado de la cuenta dentro del panel administrativo.

## Fuera de alcance

- Borrado físico irreversible de registros históricos.
- Eliminación masiva de múltiples cuentas a la vez.
- Recuperación o reactivación de la cuenta dentro de esta historia.
- Modificación de datos de cuenta; eso pertenece a `#40`.

## Reglas de negocio

1. La eliminación de cuenta en este MVP debe ser lógica, no física.
2. Solo el administrador puede ejecutar la eliminación lógica.
3. La cuenta eliminada debe dejar de considerarse activa para uso normal del sistema.
4. La operación debe conservar la información histórica necesaria para trazabilidad y relaciones existentes.
5. Si la cuenta no existe, la operación no debe ejecutarse.
6. El resultado canónico del borrado lógico en esta historia es el estado `inactivo`.

## Flujo principal

1. El administrador identifica la cuenta a eliminar desde el panel administrativo.
2. El sistema muestra la acción de eliminación lógica.
3. El administrador confirma la operación.
4. El sistema cambia el estado de la cuenta a `inactivo`.
5. El sistema conserva los datos históricos asociados.
6. El sistema informa el resultado de la operación.

## Casos alternos/validaciones

- Si la cuenta no existe, el sistema debe informar que no puede completar la eliminación.
- Si un usuario sin permisos administrativos intenta eliminar una cuenta, el sistema debe bloquear la operación.
- Si la cuenta ya estaba eliminada lógicamente, el sistema debe evitar duplicar la acción o comunicar el estado actual.

## Datos de entrada

- Identidad del administrador autenticado.
- Identificador de la cuenta a eliminar.
- Confirmación administrativa de la operación.

## Datos de salida

- Estado actualizado de la cuenta a `inactivo`.
- Confirmación de éxito o mensaje de error.
- Conservación de datos históricos asociados.

## Dependencias

- `#10` como épica contenedora.
- `#39` para consulta previa de la cuenta.
- Disponibilidad del estado `inactivo` dentro del sistema.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando confirma la eliminación de una cuenta existente, entonces el sistema aplica borrado lógico y la cuenta deja de estar operativa.
2. Dada una cuenta eliminada lógicamente, cuando se revisa su historial relacionado, entonces la información histórica continúa disponible según las reglas del sistema.
3. Dado un usuario sin permisos administrativos, cuando intenta eliminar una cuenta, entonces el sistema bloquea la operación.
4. Dada una cuenta inexistente, cuando se intenta eliminar, entonces el sistema informa que no puede completar la operación.
5. Dado el alcance de esta historia, cuando se revisa el spec, entonces queda claro que la historia cubre borrado lógico y no eliminación física irreversible.

## Definition of Done

- Existe capacidad administrativa para aplicar borrado lógico a una cuenta.
- La cuenta deja de estar operativa tras la acción administrativa.
- Los datos históricos relevantes se conservan.
- El acceso queda restringido al rol administrador.
- La historia deja explícito que no realiza borrado físico irreversible.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/40
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#40
Current issue: TheMonstersP4/mejengueros-app#41