# Issue #50: Reservar slot de cancha de 1 hora

## Título

Permitir que un `mejenguero` reserve un slot disponible de cancha por 1 hora.

## Objetivo

Definir el flujo MVP para que un usuario autenticado reserve una cancha en un slot disponible de 1 hora, sin pagos, sin cantidad de participantes y con prevención de doble reserva.

## Historia de usuario

Como `mejenguero`,
quiero seleccionar un slot disponible de una cancha y reservarlo,
para asegurar el uso de esa cancha durante una hora.

## Alcance

- Reservar un único slot de 1 hora.
- Asociar la reserva a un usuario autenticado.
- Bloquear el slot para otros usuarios al confirmar la reserva.
- Mostrar error si el slot ya fue reservado por otra persona.
- Mantener la reserva básica para el MVP.

## Fuera de alcance

- Pagos o pasarela.
- Cantidad de participantes.
- Reservas de más o menos de 1 hora.
- Reservas recurrentes.
- Organización de mejengas o equipos.
- Cancelaciones avanzadas, penalizaciones o no-show.

## Reglas de negocio

1. Una reserva pertenece a un solo usuario.
2. Una reserva ocupa exactamente un slot de 1 hora.
3. Sólo puede existir una reserva activa para la misma cancha y el mismo inicio de slot.
4. Si dos usuarios intentan reservar el mismo slot, el primero que confirme gana y el segundo recibe un error de negocio.
5. La validación debe existir en servidor y modelo de datos, no sólo en cliente.
6. El slot debe pertenecer a la disponibilidad generada desde `#49`.
7. Una reserva finalizada puede habilitar una reseña según `#17` y `#51`.

## Criterios de aceptación

1. Dado un `mejenguero` autenticado y un slot disponible, cuando confirma la reserva, entonces el sistema crea la reserva asociada a ese usuario y cancha.
2. Dado un slot ya reservado, cuando otro usuario intenta reservarlo, entonces el sistema rechaza la operación con un error claro.
3. Dado un slot fuera de la disponibilidad de la cancha, cuando el usuario intenta reservarlo, entonces el sistema rechaza la operación.
4. Dado el alcance MVP, cuando se revisa el spec, entonces queda claro que no hay pagos ni cantidad de participantes.

## Definition of Done

- Existe regla de unicidad por cancha y hora.
- La reserva se asocia a un usuario y una cancha.
- El slot queda bloqueado para otros usuarios.
- La historia queda conectada con disponibilidad, notificación y reseñas post-reserva.

---
Current issue: TheMonstersP4/mejengueros-app#50
