# Issue #39: Visualizar listado maestro de usuarios

## Título

Visualizar listado maestro de usuarios registrados con su estado actual.


## Nota de priorización post-MVP semana 13

Panel administrativo global queda post-MVP semana 13; semana 10 prioriza panel del dueño para complejo/canchas.

## Prioridad

Alta

## Objetivo

Permitir que el administrador consulte un listado maestro de usuarios registrados para tener visibilidad básica del estado actual de las cuentas dentro del sistema.

## Trazabilidad

- `FR29` El administrador podrá visualizar el listado maestro de todos los usuarios registrados con su estado actual.

## Relación con issues coordinados

- `#39` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#39` resuelve la capacidad base de consulta del universo de usuarios.
- `#40` amplía la gestión permitiendo modificar datos de cuenta.
- `#41` amplía la gestión permitiendo eliminación lógica.
- `#42` refina la consulta con filtros por rol o estado.

## Historia de usuario

Como administrador,
quiero visualizar un listado maestro de usuarios registrados,
para conocer qué cuentas existen y cuál es el estado actual de cada una.

## Alcance

- Mostrar una lista de usuarios registrados en el sistema.
- Mostrar información básica de identificación de cada cuenta.
- Mostrar el rol y el estado actual de cada usuario.
- Permitir una lectura clara para control administrativo inicial.
- Mantener el issue en nivel MVP sin bajar a reportes avanzados.

## Columnas mínimas del listado

- `nombre visible` o equivalente identificador principal.
- `correo` de la cuenta.
- `rol` actual.
- `estado` actual (`activo`, `inactivo`).

## Fuera de alcance

- Editar datos del usuario desde esta historia; eso pertenece a `#40`.
- Eliminar o desactivar cuentas desde esta historia; eso pertenece a `#41`.
- Filtrar la lista por criterios; eso pertenece a `#42`.
- Exportar usuarios a archivos, generar reportes o auditorías avanzadas.
- Gestionar permisos complejos o acciones masivas.

## Reglas de negocio

1. Solo el administrador puede acceder al listado maestro de usuarios.
2. La lista debe mostrar el estado actual de cada cuenta de forma visible.
3. La lista debe incluir como mínimo `nombre visible`, `correo`, `rol` y `estado` para identificar cada cuenta dentro del sistema.
4. El listado maestro representa una vista de consulta; no debe mezclar responsabilidades de edición o eliminación.
5. Si no existen usuarios registrados, el sistema debe mostrar un estado vacío claro.

## Flujo principal

1. El administrador accede al panel administrativo.
2. El sistema valida que tenga permisos de administración.
3. El sistema consulta el conjunto de usuarios registrados.
4. El sistema muestra el listado maestro con información básica, rol y estado.
5. El administrador revisa la información disponible para control global.

## Casos alternos/validaciones

- Si el usuario autenticado no tiene permisos de administrador, el sistema no debe exponer el listado.
- Si no existen usuarios registrados, el sistema debe mostrar un estado vacío claro.
- Si ocurre una falla al cargar el listado, el sistema debe comunicar el error sin simular resultados válidos.

## Datos de entrada

- Identidad del usuario autenticado.
- Permisos o rol administrativo.
- Conjunto de usuarios registrados.
- Estado actual de cada cuenta.

## Datos de salida

- Listado maestro de usuarios registrados.
- Información básica de identificación por usuario.
- Rol actual de cada cuenta.
- Estado actual de cada cuenta.
- Estado vacío o mensaje de error cuando aplique.

## Dependencias

- `#10` como épica contenedora.
- Existencia del rol administrador.
- Disponibilidad de datos de usuarios dentro del sistema.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando ingresa al panel de usuarios, entonces puede visualizar el listado maestro de usuarios registrados.
2. Dado un usuario del listado, cuando el administrador revisa la fila correspondiente, entonces puede identificar información básica, rol y estado actual de la cuenta.
3. Dado un usuario sin permisos administrativos, cuando intenta acceder al listado maestro, entonces el sistema no expone la información.
4. Dado que no existan usuarios registrados, cuando el administrador accede al listado, entonces el sistema muestra un estado vacío claro.
5. Dado el alcance de esta historia, cuando se revisa el spec, entonces queda claro que la historia cubre consulta y no edición, eliminación o filtrado.

## Definition of Done

- Existe una vista o capacidad administrativa para consultar el listado maestro de usuarios.
- El listado muestra el estado actual de cada cuenta.
- El acceso queda restringido al rol administrador.
- Se contemplan estados vacíos y errores de carga.
- La historia mantiene separación clara respecto a edición, eliminación lógica y filtrado.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/38
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#38
Current issue: TheMonstersP4/mejengueros-app#39