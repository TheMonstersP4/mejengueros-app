# Issue #55: Validar flujo MVP end-to-end y errores críticos de reserva

## Título

Validar el flujo MVP end-to-end y los errores críticos de reserva.

## Objetivo

Asegurar que el flujo principal del MVP pueda recorrerse end-to-end para el demo de semana 10, incluyendo errores críticos de reserva y estados visibles importantes.

## Historia de usuario

Como equipo de desarrollo,
queremos validar el flujo completo del MVP y sus errores críticos,
para presentar un demo estable y coherente en semana 10.

## Alcance

- Recorrer flujo `mejenguero`: catálogo -> detalle -> disponibilidad -> reserva.
- Validar error de slot ya reservado / doble reserva.
- Validar estado sin slots disponibles.
- Validar usuario no autenticado intentando reservar.
- Validar reserva finalizada habilitando reseña cuando `#17` y `#51` estén disponibles.
- Validar que la reseña no pueda duplicarse para la misma reserva si aplica.
- Crear lista de verificación de demo y smoke test manual/repetible.

## Fuera de alcance

- Automatización completa E2E si el stack no está listo.
- control de calidad exhaustivo de post-MVP.
- Social login, landing pages, favoritos, perfil extendido, admin global o pagos.

## Reglas de negocio / operación

1. La validación debe centrarse en el flujo central aprobado para semana 10.
2. Los datos usados deben provenir de la base de datos o del seed definido en `#54` / `#52`.
3. Los errores críticos deben documentarse con pasos de reproducción y resultado esperado.
4. Si una parte del flujo no está lista, debe quedar registrada como riesgo del demo y no ocultarse.

## Criterios de aceptación

1. Dado el ambiente demo, cuando se ejecuta el lista de verificación, entonces se puede recorrer catálogo, detalle y reserva con datos de base de datos.
2. Dado un slot ya reservado, cuando se intenta reservar nuevamente, entonces se observa un error claro.
3. Dado un usuario sin autenticación, cuando intenta reservar, entonces el sistema bloquea la acción o solicita autenticación.
4. Dado un día o cancha sin slots disponibles, cuando el usuario consulta disponibilidad, entonces el sistema muestra un estado claro sin permitir una reserva inválida.
5. Dada una reserva finalizada y elegible para reseña, cuando el usuario accede al flujo post-reserva, entonces el lista de verificación cubre la navegación hacia reseña.
6. Dada una reserva que ya tiene reseña, cuando se intenta reseñar de nuevo, entonces el sistema bloquea o comunica la duplicidad según la regla definida.
7. Dado el cierre de Sprint 5, cuando se revisa el demo, entonces existe un lista de verificación reproducible con pasos, datos usados, resultado esperado y riesgos pendientes.

## Definition of Done

- Lista de verificación E2E documentado.
- Errores críticos de reserva validados.
- Estados de usuario no autenticado, slot reservado y sin disponibilidad cubiertos.
- Riesgos del demo registrados si existen.
- Flujo central listo para ensayo de semana 10.

---
Current issue: TheMonstersP4/mejengueros-app#55
