# Issue #48: Crear complejo deportivo y primera cancha

## Título

Permitir que el `dueño` cree un complejo deportivo y registre su primera cancha en un flujo rápido.

## Objetivo

Reemplazar el flujo anterior centrado en crear una cancha en `borrador` por un flujo MVP donde el `dueño` crea primero el `Complejo` y luego registra una o más `Canchas` asociadas.

## Historia de usuario

Como `dueño`,
quiero crear mi complejo deportivo y registrar una cancha,
para que pueda ofrecerla en el catálogo y habilitar reservas de forma rápida.

## Alcance

- Crear un `Complejo` como entidad principal.
- Permitir que un complejo tenga una o más canchas.
- Permitir que un complejo tenga una sola cancha sin tratarlo como caso especial.
- Registrar una primera cancha asociada al complejo.
- Mantener el flujo ágil, sin estado intermedio `borrador`.
- Dejar la cancha lista para completar servicios y disponibilidad reservable.

## Fuera de alcance

- Estado `borrador` o publicación diferida compleja.
- Límite máximo de canchas por complejo.
- Administración global del catálogo.
- Pagos o reservas durante la creación.

## Reglas de negocio

1. Todo `dueño` debe crear un `Complejo` antes de registrar canchas.
2. Toda `Cancha` pertenece a un `Complejo`.
3. Un `Complejo` puede tener una o más canchas.
4. El MVP no define un límite máximo de canchas por complejo.
5. La creación no debe depender de un estado `borrador`; si faltan datos obligatorios, el sistema debe bloquear el guardado y mostrar la validación.
6. La disponibilidad de reservas se define por cancha en `#49`.
7. Los servicios se definen como servicios de complejo o de cancha según `#14`.

## Criterios de aceptación

1. Dado un `dueño` autenticado, cuando crea un complejo con los datos mínimos requeridos, entonces el sistema registra el complejo.
2. Dado un complejo existente del `dueño`, cuando registra una cancha dentro de ese complejo, entonces la cancha queda asociada al complejo.
3. Dado un complejo con una sola cancha, cuando se consulta el modelo, entonces sigue cumpliendo la relación `Complejo` -> `Cancha`.
4. Dado que falta un dato obligatorio, cuando el `dueño` intenta guardar, entonces el sistema bloquea la operación sin crear un borrador parcial.
5. Dado el alcance MVP, cuando se revisa el flujo, entonces no existe estado `borrador` ni publicación diferida compleja.

## Definition of Done

- Existe un spec claro para `Complejo` como entidad raíz.
- Existe relación explícita `Complejo` -> `Cancha`.
- El flujo reemplaza el borrador por validaciones antes de guardar.
- La historia queda vinculada con `#11`, `#12`, `#13` y `#14` como re-alcance del flujo de creación.

---
Current issue: TheMonstersP4/mejengueros-app#48
