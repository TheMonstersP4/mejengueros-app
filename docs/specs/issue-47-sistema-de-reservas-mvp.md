# Issue #47: Sistema de reservas MVP

## Título

Incorporar el sistema de reservas como capacidad central del MVP.

## Nota de planificación en Project

`#47` es épica/contenedor de reservas MVP. Se mantiene sin assignee, estimate ni sprint propio para evitar duplicar el esfuerzo ya estimado en sus subissues `#48`–`#52`.

## Objetivo

Definir la épica contenedora para que el MVP permita a un `mejenguero` buscar una cancha, consultar slots disponibles de 1 hora y reservar uno sin pagos ni confirmación de cantidad de personas.

## Alcance

- Reservas sin pasarela ni módulo de pagos.
- Una reserva pertenece a una sola persona.
- Cada reserva ocupa un slot exacto de 1 hora.
- El slot queda bloqueado para cualquier otra persona cuando se confirma la reserva.
- La disponibilidad se deriva del horario configurado para cada cancha.
- La reserva finalizada habilita el flujo de reseña post-reserva.

## Fuera de alcance

- Pagos, pasarela, depósitos o cobros.
- Cantidad de participantes por reserva.
- Reservas de duración distinta a 1 hora.
- Feriados o excepciones especiales de días cerrados.
- Organización de mejengas, equipos o asistencia de jugadores.

## Subissues esperadas

- `#48` Crear complejo deportivo y primera cancha.
- `#49` Configurar disponibilidad reservable de cancha.
- `#50` Reservar slot de cancha de 1 hora.
- `#51` Notificar reseña post-reserva.
- `#52` Preparar datos demo para semana 10.

## Definition of Done

- Las historias de reserva quedan separadas de pagos, grupos y organización de mejengas.
- Las historias correlacionadas dejan de tratar las reservas como fuera de alcance.
- La elegibilidad para reseñar se basa en reserva finalizada, no en QR/código.

---
Current issue: TheMonstersP4/mejengueros-app#47
