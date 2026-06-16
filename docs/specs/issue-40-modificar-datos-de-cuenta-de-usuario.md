# Issue #40: Modificar datos de cuenta de usuario

## Título

Modificar datos de cuenta de usuario, incluyendo información básica o roles.


## Nota de priorización post-MVP semana 13

Edición administrativa global de usuarios queda post-MVP semana 13; semana 10 prioriza roles mínimos para reserva y dueño.

## Prioridad

Alta

## Objetivo

Permitir que el administrador actualice datos básicos de una cuenta registrada y, cuando corresponda, ajuste su rol dentro del sistema para mantener el control administrativo del producto.

## Trazabilidad

- `FR30` El administrador podrá modificar los datos de cuenta de un usuario, incluyendo información básica o roles.

## Relación con issues coordinados

- `#40` forma parte de la épica `#10 Panel de Administración y Control Global`.
- `#39` provee la vista base desde la cual el administrador identifica cuentas.
- `#40` permite gestionar información básica o roles.
- `#41` cubre la eliminación lógica de cuentas, por separado.
- `#42` cubre filtrado y no debe absorber la edición.
- `#36` cubre la autogestión del apodo y posición favorita del `mejenguero`; `#40` no debe absorber esos campos salvo decisión explícita de override administrativo.

## Historia de usuario

Como administrador,
quiero modificar datos de cuenta de un usuario,
para mantener actualizada su información básica o su rol dentro del sistema.

## Alcance

- Permitir la edición administrativa de información básica de la cuenta.
- Permitir la actualización del rol del usuario cuando aplique.
- Mostrar el resultado exitoso o fallido de la actualización.
- Mantener una experiencia administrativa clara y controlada.

## Campos editables del MVP

- `nombre administrativo visible` o equivalente de cuenta, siempre que no sea el `apodo` autogestionado por `#36`.
- `rol` asignado dentro del catálogo válido (`administrador`, `dueño`, `mejenguero`).

## Campos no editables en esta historia

- `correo` de la cuenta cuando actúe como identificador de autenticación.
- Contraseña.
- Identificadores del proveedor de autenticación.
- Historial del usuario.
- Apodo y posición favorita del `mejenguero`, salvo que el producto defina explícitamente una regla de override administrativo coordinada con `#36`.
- Estado `inactivo`, cuya gestión corresponde a `#41`.

## Fuera de alcance

- Cambiar contraseñas directamente desde esta historia.
- Recuperación de acceso del usuario.
- Eliminación lógica o desactivación de la cuenta; eso pertenece a `#41`.
- Edición masiva de usuarios.
- Gestión avanzada de permisos más allá de roles definidos por el MVP.

## Reglas de negocio

1. Solo el administrador puede modificar datos de cuenta de otros usuarios.
2. La actualización debe permitir editar únicamente los campos definidos para el MVP dentro de lo permitido por el sistema.
3. Los cambios aplicados deben quedar asociados a una cuenta válida existente.
4. La edición no debe convertirse en recuperación de credenciales ni en gestión de autenticación externa.
5. Si el rol ingresado no pertenece al catálogo permitido del sistema, el cambio no debe aplicarse.
6. Esta historia no debe usarse para cambiar contraseñas ni credenciales federadas.
7. Esta historia no debe modificar el apodo ni la posición favorita definidos en `#36`, salvo que se agregue una regla explícita de override administrativo.

## Flujo principal

1. El administrador identifica un usuario desde el panel administrativo.
2. El sistema muestra la información editable de la cuenta.
3. El administrador modifica datos básicos o el rol.
4. El sistema valida los datos ingresados.
5. El sistema guarda la actualización.
6. El sistema informa que la cuenta fue actualizada correctamente.

## Casos alternos/validaciones

- Si el administrador intenta editar una cuenta inexistente, el sistema debe informar que no puede completar la operación.
- Si el rol indicado no es válido, el sistema debe rechazar la actualización.
- Si algún dato obligatorio no cumple validación, el sistema no debe guardar cambios incompletos.
- Si un usuario sin permisos administrativos intenta modificar una cuenta, el sistema debe bloquear la operación.

## Datos de entrada

- Identidad del administrador autenticado.
- Identificador del usuario a modificar.
- Datos básicos actualizados.
- Rol actualizado, cuando aplique.

## Datos de salida

- Cuenta actualizada.
- Confirmación de éxito o mensaje de error.
- Información modificada reflejada en el panel administrativo.

## Dependencias

- `#10` como épica contenedora.
- `#39` para localizar o consultar la cuenta a gestionar.
- Existencia de un catálogo de roles válidos.
- Disponibilidad de persistencia de usuarios.

## Criterios de aceptación

1. Dado un administrador autenticado, cuando modifica información básica válida de una cuenta, entonces el sistema guarda la actualización.
2. Dado un administrador autenticado, cuando asigna un rol válido permitido por el sistema, entonces el cambio de rol se aplica correctamente.
3. Dado un rol inválido, cuando el administrador intenta guardarlo, entonces el sistema rechaza la actualización.
4. Dado un usuario sin permisos administrativos, cuando intenta editar una cuenta, entonces el sistema bloquea la operación.
5. Dado el alcance de esta historia, cuando se revisa el spec, entonces queda claro que la historia cubre modificación de datos y no eliminación, recuperación de acceso o edición masiva.
6. Dado el vínculo con `#36`, cuando se revisa el spec, entonces queda claro que `#40` no absorbe la edición del apodo ni la posición favorita del `mejenguero` por defecto.

## Definition of Done

- Existe capacidad administrativa para modificar datos básicos de una cuenta.
- Existe capacidad administrativa para modificar roles válidos del sistema.
- La edición está restringida al rol administrador.
- Se contemplan validaciones de datos y de catálogo de roles.
- La historia mantiene separación clara respecto a eliminación lógica, recuperación de credenciales y autogestión de perfil de `#36`.


## Notas de partición

- `#36` responde: `cómo el mejenguero edita su apodo y posición favorita dentro de su propio perfil`.
- `#40` responde: `cómo el administrador modifica datos administrativos de cuenta y roles`.
- Si la decisión pertenece a credenciales, recuperación de acceso o contraseña, cae fuera de `#40`.
- Si la decisión pertenece a apodo o posición favorita autogestionada por el usuario, cae en `#36` salvo decisión explícita de override administrativo.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/39
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#39
Current issue: TheMonstersP4/mejengueros-app#40