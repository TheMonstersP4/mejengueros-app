# Issue #16: Ver detalle de cancha publicada y activa

## Título

Ver detalle de cancha con servicios, rating y disponibilidad para reservar.

## Nota de re-alcance MVP

El detalle de cancha pasa a ser la entrada principal para el flujo de reserva. Ya no debe excluir reservas; debe mostrar información suficiente para elegir un slot de 1 hora y reservar sin pagos.

## Objetivo

Permitir que el `mejenguero` consulte el detalle de una cancha visible, revise información del complejo/cancha, servicios, rating y disponibilidad, e inicie una reserva de 1 hora.

## Relación con issues coordinados

- `#15` permite descubrir complejos/canchas.
- `#14` define servicios de complejo/cancha.
- `#49` define disponibilidad reservable.
- `#50` crea la reserva.
- `#17` permite reseñar después de una reserva finalizada.

## Historia de usuario

Como `mejenguero`,
quiero ver el detalle de una cancha y sus slots disponibles,
para decidir si reservarla.

## Alcance

- Mostrar datos principales del complejo y cancha.
- Mostrar servicios relevantes de complejo/cancha.
- Mostrar rating/calificaciones visibles.
- Mostrar o enlazar disponibilidad reservable en slots de 1 hora.
- Permitir iniciar el flujo de reserva.

## Fuera de alcance

- Pagos o pasarela.
- Cantidad de participantes.
- Reservas de duración distinta a 1 hora.
- Geolocalización avanzada.
- Moderación o edición de reseñas.

## Reglas de negocio

1. Sólo se muestran canchas visibles y activas para reserva.
2. El detalle debe indicar servicios y rating de forma comprensible.
3. La disponibilidad se deriva de los horarios configurados para la cancha.
4. El usuario sólo puede reservar slots disponibles según `#50`.
5. Si una cancha no tiene disponibilidad configurada, el detalle debe comunicar que no hay slots reservables.

## Criterios de aceptación

1. Dada una cancha visible y activa, cuando el mejenguero abre el detalle, entonces ve datos del complejo/cancha, servicios y rating.
2. Dada una cancha con disponibilidad configurada, cuando el detalle carga, entonces el usuario puede consultar slots de 1 hora o navegar a la reserva.
3. Dado un slot disponible, cuando el usuario decide reservar, entonces el sistema inicia el flujo de `#50`.
4. Dada una cancha sin disponibilidad, cuando el usuario revisa el detalle, entonces el sistema muestra un estado claro sin permitir reserva inválida.
5. Dado el alcance MVP, cuando se revisa el spec, entonces no hay pagos ni cantidad de participantes.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El detalle muestra servicios y rating.
- El detalle conecta con disponibilidad/reserva.
- El detalle no depende de QR/código para reseñas.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/15
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#15
Current issue: TheMonstersP4/mejengueros-app#16
