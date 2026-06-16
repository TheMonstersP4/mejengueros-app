# Issue #51: Notificar reseña post-reserva

## Título

Notificar al usuario después de una reserva finalizada para que deje una reseña.

## Objetivo

Conceptualizar el módulo MVP de notificaciones para que, cuando una reserva haya concluido, el sistema invite al usuario a dejar una reseña de la cancha reservada.

## Historia de usuario

Como `mejenguero`,
quiero recibir una notificación después de usar una cancha reservada,
para poder dejar una reseña de forma directa.

## Alcance

- Detectar reservas cuyo slot ya concluyó.
- Crear una notificación interna persistida para el usuario de la reserva.
- Permitir que el usuario abra la creación de reseña desde la notificación.
- Usar WebSocket como mejora de entrega en tiempo real si está disponible.
- Mantener la notificación persistida como fuente de verdad.

## Fuera de alcance

- Push notifications del sistema operativo como requisito de semana 10.
- Emails, SMS o campañas externas.
- Recordatorios múltiples o reglas avanzadas de engagement.
- Notificaciones para usuarios que no participaron en la reserva.

## Reglas de negocio

1. La notificación se genera sólo para el usuario dueño de la reserva.
2. La reserva debe haber finalizado para habilitar la notificación de reseña.
3. Si ya existe una reseña para la reserva, no debe generarse una nueva notificación pendiente para reseñar.
4. La notificación debe persistirse para que el usuario la vea aunque no esté conectado en tiempo real.
5. WebSocket puede usarse para entregar la notificación si el usuario está conectado, pero no reemplaza la persistencia.
6. Al presionar la notificación, el sistema abre el flujo de reseña asociado a esa reserva.

## Criterios de aceptación

1. Dada una reserva finalizada sin reseña, cuando el sistema evalúa notificaciones pendientes, entonces crea una notificación interna para el usuario de esa reserva.
2. Dado un usuario conectado, cuando se crea la notificación y existe canal WebSocket, entonces el sistema puede entregarla en tiempo real.
3. Dado un usuario no conectado, cuando vuelve a entrar, entonces puede ver la notificación persistida.
4. Dada una notificación de reseña, cuando el usuario la presiona, entonces se abre el flujo de creación de reseña para la reserva correspondiente.
5. Dada una reserva que ya tiene reseña, cuando se evalúan notificaciones, entonces no se duplica la invitación.

## Definition of Done

- El spec distingue persistencia de notificación y entrega por WebSocket.
- La reseña post-reserva queda conectada con la notificación.
- QR/código no participa en este flujo.

---
Current issue: TheMonstersP4/mejengueros-app#51
