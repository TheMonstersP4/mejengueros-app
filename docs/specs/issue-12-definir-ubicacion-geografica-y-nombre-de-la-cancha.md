# Issue #12: Definir ubicación geográfica y nombre del complejo/cancha

## Título

Definir identidad y ubicación del complejo deportivo.

## Nota de re-alcance MVP

Por directriz de la profesora, `Complejo` pasa a ser la entidad principal. La ubicación y datos de identidad viven principalmente en el complejo; la cancha puede tener un nombre o identificador interno dentro de ese complejo.

## Objetivo

Definir los datos mínimos de identidad y ubicación para crear un complejo deportivo y asociar sus canchas, manteniendo el flujo rápido y sin estado `borrador`.

## Relación con issues coordinados

- `#48` crea el complejo y la primera cancha.
- `#14` define servicios de complejo o cancha.
- `#13` / `#49` definen disponibilidad reservable por cancha.
- `#15` y `#16` muestran esta información al mejenguero.

## Historia de usuario

Como `dueño`,
quiero registrar el nombre y ubicación de mi complejo,
para que los mejengueros puedan encontrarlo y reservar una de sus canchas.

## Alcance

- Nombre del complejo.
- Provincia, cantón y referencia de dirección.
- Ubicación suficiente para búsqueda y visualización MVP.
- Nombre o identificador simple de cancha dentro del complejo.
- Validaciones mínimas antes de guardar.

## Fuera de alcance

- Geolocalización avanzada o cálculo de cercanía.
- Múltiples sedes bajo un mismo complejo.
- Estado `borrador` o publicación diferida.
- Reservar desde esta historia; eso corresponde a `#50`.

## Reglas de negocio

1. Todo complejo debe tener nombre y ubicación mínima.
2. Una cancha dentro de un complejo puede tener nombre o identificador interno.
3. El sistema debe validar los datos obligatorios antes de guardar.
4. La ubicación del complejo se usa para búsqueda y detalle.
5. No se persiste un borrador parcial por falta de información obligatoria.

## Criterios de aceptación

1. Dado un dueño autenticado, cuando registra nombre y ubicación válida del complejo, entonces el sistema acepta la identidad del complejo.
2. Dado un complejo con una cancha, cuando se registra un nombre o identificador de cancha, entonces queda asociado al complejo.
3. Dado que falta nombre o ubicación mínima, cuando el dueño intenta guardar, entonces el sistema muestra validaciones y no guarda un borrador parcial.
4. Dado el re-alcance MVP, cuando se revisa el spec, entonces queda claro que ubicación/nombre pertenecen principalmente al complejo.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define identidad mínima de complejo.
- El spec permite identificar canchas dentro del complejo.
- El spec elimina dependencia de `borrador`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/11
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#11
Current issue: TheMonstersP4/mejengueros-app#12
