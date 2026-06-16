# Issue #52: Completar datos demo y lista de verificación semana 10

## Título

Completar datos demo y lista de verificación de presentación para semana 10.

## Nota de re-alcance roadmap

El seed mínimo para que el flujo MVP sea visible al cierre de Sprint 4 se separa en `#54`. `#52` queda como cierre de demo semana 10: completar datos, verificar consistencia, preparar reinicio y lista de verificación de presentación.

## Objetivo

Asegurar que el demo de semana 10 tenga datos suficientes, repetibles y conectados a base de datos para mostrar el flujo completo del MVP con estabilidad.

## Alcance

- Completar datos demo iniciados en `#54`.
- Validar usuarios demo para roles `mejenguero` y `dueño`.
- Validar complejos, canchas, servicios y disponibilidad demo.
- Validar reservas futuras y finalizadas.
- Validar ratings/reseñas visibles.
- Validar notificaciones de reseña post-reserva cuando aplique.
- Preparar lista de verificación de presentación y reinicio/reseed.

## Fuera de alcance

- Datos reales o sensibles.
- Panel administrativo de seed data.
- Procesos masivos de carga.
- Social login, landing pages, favoritos, perfil extendido o admin global.

## Reglas de negocio / operación

1. Los datos demo deben persistirse en la base de datos usada por la aplicación.
2. Los datos demo deben cubrir el flujo semana 10 sin depender de valores quemados sólo en UI.
3. El lista de verificación debe permitir repetir la demo en ensayos.
4. Los datos deben respetar las reglas de reserva de 1 hora y no doble booking.
5. Las reseñas demo deben estar asociadas a reservas finalizadas cuando corresponda.

## Criterios de aceptación

1. Dado el ambiente demo, cuando se ejecuta el reinicio/seed final, entonces quedan datos suficientes para recorrer el flujo principal.
2. Dado un mejenguero demo, cuando consulta catálogo y detalle, entonces puede ver disponibilidad y reservar un slot válido.
3. Dada una reserva demo finalizada sin reseña, cuando el usuario revisa notificaciones, entonces puede acceder al flujo de reseña.
4. Dado un dueño demo, cuando revisa reseñas recibidas, entonces puede ver retroalimentación asociado a sus canchas.
5. Dado el lista de verificación de demo, cuando el equipo lo sigue, entonces puede reproducir la presentación de semana 10.

## Definition of Done

- Datos demo finales preparados.
- Lista de verificación de demo documentado.
- Reinicio/reseed documentado.
- Flujo semana 10 validado contra datos en base de datos.

---
Current issue: TheMonstersP4/mejengueros-app#52
