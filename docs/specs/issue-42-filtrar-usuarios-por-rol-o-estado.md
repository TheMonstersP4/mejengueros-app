# Issue #42: Filtrar usuarios por rol o estado

## Título

Filtrar usuarios por rol o estado dentro del panel administrativo.


## Nota de priorización post-MVP semana 13

Filtrado administrativo de usuarios queda post-MVP semana 13; no bloquea flujo demo de reserva.

## Prioridad

Alta

## Objetivo

Permitir que el administrador refine el listado maestro de usuarios usando filtros por rol o estado para localizar cuentas de forma más rápida y controlada.

## Trazabilidad

- `FR32` El administrador podrá filtrar usuarios por rol o estado.

## Relación con issues coordinados

- `#42` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#39` define el listado maestro base sobre el cual actúan los filtros.
- `#42` refina la consulta y no reemplaza la visualización base.
- `#40` y `#41` operan sobre cuentas individuales y pueden apoyarse en los resultados filtrados.

## Historia de usuario

Como administrador,
quiero filtrar usuarios por rol o estado,
para encontrar más rápido las cuentas que necesito revisar o gestionar.

## Alcance

- Permitir filtrar el listado maestro por rol.
- Permitir filtrar el listado maestro por estado.
- Mostrar resultados consistentes con los filtros aplicados.
- Mantener una experiencia administrativa simple y clara para el MVP.

## Valores de filtro del MVP

### Roles

- `administrador`
- `dueño`
- `mejenguero`

### Estados

- `activo`
- `inactivo`

## Fuera de alcance

- Búsqueda avanzada por múltiples campos libres.
- Exportación de resultados filtrados.
- Acciones masivas sobre el conjunto filtrado.
- Edición o eliminación desde esta historia; esas capacidades pertenecen a `#40` y `#41`.

## Reglas de negocio

1. Solo el administrador puede usar los filtros del panel administrativo.
2. Los filtros deben aplicarse sobre el listado maestro de usuarios.
3. El sistema debe permitir filtrar al menos por rol o por estado actual.
4. Si no existen resultados para un filtro aplicado, el sistema debe mostrar un estado vacío claro.
5. Filtrar no debe alterar los datos originales de los usuarios; solo cambia la vista consultada.

## Flujo principal

1. El administrador accede al listado maestro de usuarios.
2. El sistema muestra los controles de filtrado disponibles.
3. El administrador selecciona un rol, un estado o ambos según el diseño del MVP.
4. El sistema aplica el filtro al listado.
5. El sistema muestra solo los usuarios que cumplen los criterios seleccionados.

## Casos alternos/validaciones

- Si no existen usuarios que cumplan el filtro, el sistema debe mostrar un estado vacío claro.
- Si un usuario sin permisos administrativos intenta acceder al filtrado, el sistema debe bloquear la vista.
- Si se limpian los filtros, el sistema debe volver al listado maestro completo.

## Datos de entrada

- Identidad del administrador autenticado.
- Listado maestro de usuarios.
- Rol seleccionado como filtro, cuando aplique.
- Estado seleccionado como filtro, cuando aplique.

## Datos de salida

- Listado filtrado de usuarios.
- Estado vacío si no hay coincidencias.
- Vista restablecida al listado completo cuando se limpian filtros.

## Dependencias

- `#10` como épica contenedora.
- `#39` para la existencia del listado maestro base.
- Catálogo de roles y estados válidos del sistema.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando aplica un filtro por rol, entonces el sistema muestra únicamente los usuarios que pertenecen a ese rol.
2. Dado un administrador autenticado, cuando aplica un filtro por estado, entonces el sistema muestra únicamente los usuarios que tienen ese estado.
3. Dado que no existan coincidencias para un filtro aplicado, cuando el administrador revisa el resultado, entonces el sistema muestra un estado vacío claro.
4. Dado un usuario sin permisos administrativos, cuando intenta acceder a la vista filtrada, entonces el sistema bloquea el acceso.
5. Dado un filtro aplicado, cuando el administrador lo limpia, entonces el sistema vuelve a mostrar el listado maestro completo.

## Definition of Done

- Existe capacidad administrativa para filtrar usuarios por rol.
- Existe capacidad administrativa para filtrar usuarios por estado.
- Los filtros afectan solo la vista del listado y no modifican datos.
- Se contemplan estados vacíos y limpieza de filtros.
- La historia mantiene separación clara respecto a edición y eliminación de cuentas.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/41
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#41
Current issue: TheMonstersP4/mejengueros-app#42