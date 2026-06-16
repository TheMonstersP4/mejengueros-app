# Issue #54: Preparar seed mínimo DB para demo visible del flujo MVP

## Título

Preparar seed mínimo DB para demo visible del flujo MVP.

## Objetivo

Crear datos de ejemplo persistidos en la base de datos para que, al cierre de Sprint 4, el equipo pueda demostrar el flujo visible de búsqueda, detalle, disponibilidad y reserva sin depender de datos quemados en la UI.

## Historia de usuario

Como equipo de desarrollo,
queremos contar con datos demo mínimos conectados a la base de datos,
para mostrar el flujo central del MVP antes del demo de semana 10.

## Alcance

- Usuario dueño demo.
- Usuario mejenguero demo.
- Complejo demo.
- Cancha demo asociada al complejo.
- Servicios mínimos de complejo/cancha.
- Horario/disponibilidad que genere slots de 1 hora.
- Al menos un slot futuro disponible.
- Al menos un slot futuro ya reservado para probar error de doble reserva.
- Datos suficientes para recorrer catálogo -> detalle -> reserva.
- Comando o procedimiento documentado para cargar/reinicioear estos datos.

## Fuera de alcance

- Lista de verificación completo de semana 10; eso queda en `#52`.
- Datos exhaustivos para todos los casos post-MVP.
- Datos reales o credenciales sensibles.
- Notificaciones/reseñas completas, salvo placeholders mínimos si ayudan al flujo visible.

## Reglas de negocio / operación

1. Los datos deben persistirse en la base de datos usada por la app.
2. No se aceptan datos hardcodeados únicamente en componentes UI como sustituto del seed.
3. Los datos deben respetar el modelo `Complejo` -> `Cancha` -> disponibilidad -> slots -> reserva.
4. Los slots demo deben respetar la duración exacta de 1 hora.
5. El seed debe facilitar validar que un slot reservado no se puede reservar de nuevo.

## Criterios de aceptación

1. Dado el ambiente local/desarrollo, cuando se ejecuta el seed mínimo, entonces se crean usuarios, complejo, cancha, servicios, disponibilidad y slots demo.
2. Dado un mejenguero demo, cuando consulta catálogo y detalle, entonces puede ver datos provenientes de la base de datos.
3. Dado un slot disponible del seed, cuando se reserva, entonces queda persistido como reserva.
4. Dado un slot ya reservado del seed, cuando otro usuario intenta reservarlo, entonces el flujo puede demostrar el error de negocio.
5. Dado el objetivo de Sprint 4, cuando se revisa el demo, entonces el flujo visible no depende de datos quemados en la UI.

## Definition of Done

- Seed mínimo base de datos creado o documentado.
- Datos suficientes para catálogo, detalle, disponibilidad y reserva.
- Procedimiento de reinicio/carga documentado.
- Flujo visible listo para revisión de Sprint 4.

---
Current issue: TheMonstersP4/mejengueros-app#54
