# Issue #45: Diseñar wireframes navegables de las pantallas MVP

## Título

Diseñar wireframes navegables del flujo MVP semana 10.

## Prioridad

Alta

## Nota de re-alcance MVP

Por directriz de la profesora, los wireframes navegables deben enfocarse en el demo funcional de semana 10: búsqueda, detalle, reserva, notificación post-reserva, reseña y panel básico del dueño para complejo/canchas. Landing pages, social login, perfil/favoritos/foto y panel admin global quedan post-MVP y no son pantallas requeridas para este entregable.

## Objetivo

Definir una base visual y navegable de baja fidelidad para representar el flujo principal del MVP semana 10, reduciendo ambigüedad antes de implementación.

## Relación con issues coordinados

- Búsqueda/catálogo y detalle: `#15`, `#16`.
- Complejo, cancha, servicios y disponibilidad: `#11`, `#12`, `#13`, `#14`, `#48`, `#49`.
- Reservas: `#47`, `#50`.
- Reseñas/rating post-reserva: `#17`, `#18`, `#21`, `#51`.
- Datos demo: `#52`.
- Post-MVP/no requerido en estos wireframes navegables: `#26`, `#27`, `#30`, `#31`, `#35`, `#36`, `#37`, `#38`, `#39`, `#40`, `#41`, `#42`, `#43`, `#44`, `#46`.

## Historia de usuario

Como equipo de desarrollo y diseño,
quiero wireframes navegables del MVP semana 10,
para validar el flujo de búsqueda, reserva y reseña antes de implementar pantallas.

## Alcance

- Crear wireframes navegables de baja fidelidad para el flujo del `mejenguero`: buscar, ver detalle, reservar, recibir notificación y reseñar.
- Crear wireframes navegables de baja fidelidad para el panel básico del `dueño`: gestionar complejo, canchas, servicios y disponibilidad reservable.
- Representar estados principales, vacíos y errores relevantes.
- Mantener trazabilidad entre pantallas e issues.
- Usar datos de ejemplo compatibles con `#52`.

## Fuera de alcance

- Landing page pública del producto o equipo.
- Social login.
- Perfil extendido, apodo, posición favorita, favoritos o foto de perfil.
- Panel administrativo global de usuarios.
- QR/código de validación para reseñas.
- Pagos, pasarela, cantidad de participantes, dashboards avanzados o moderación.
- Diseños finales de alta fidelidad o implementación en código.

## Pantallas y flujos mínimos esperados

### Mejenguero

- Registro/inicio de sesión manual mínimo si es necesario para reservar.
- Catálogo/búsqueda de complejos/canchas.
- Detalle de cancha con servicios, rating y disponibilidad.
- Selección de slot de 1 hora.
- Confirmación de reserva sin pagos ni cantidad de participantes.
- Error de slot ya reservado.
- Estado de reserva futura/finalizada si se requiere para acceder a reseña.
- Notificación interna post-reserva.
- Creación de reseña desde notificación o reserva finalizada.

### Dueño

- Crear complejo deportivo.
- Crear cancha dentro del complejo.
- Configurar servicios de complejo/cancha.
- Configurar disponibilidad de cancha con días + rango horario.
- Visualizar reseñas recibidas por complejo/cancha.

## Reglas de negocio visuales

1. Los wireframes navegables deben representar reservas como slots exactos de 1 hora.
2. Los wireframes navegables no deben mostrar pagos ni cantidad de participantes.
3. Los wireframes navegables no deben mostrar QR/código para habilitar reseñas.
4. La reseña debe nacer de reserva finalizada o notificación post-reserva.
5. El panel del dueño debe partir de `Complejo` y luego `Cancha`.
6. Las pantallas post-MVP pueden aparecer sólo como nota de trazabilidad, no como requeridas.

## Casos alternos/validaciones visibles

- Slot ya reservado por otra persona.
- No hay slots disponibles para el día seleccionado.
- Usuario no autenticado intenta reservar.
- Reserva aún no finalizada intenta abrir reseña.
- Reserva finalizada ya tiene reseña.
- Reserva cancelada/no válida no habilita reseña.
- Dueño intenta guardar disponibilidad inválida.

## Criterios de aceptación

1. Dado el flujo del `mejenguero`, cuando se navega el wireframe navegable, entonces se entiende cómo pasa de búsqueda a detalle, reserva, notificación y reseña.
2. Dado el flujo del `dueño`, cuando se navega el wireframe navegable, entonces se entiende cómo crea complejo, cancha, servicios y disponibilidad reservable.
3. Dado un slot reservado, cuando otro usuario intenta reservarlo, entonces existe representación o anotación del error.
4. Dada una reserva finalizada sin reseña, cuando se revisa el flujo, entonces existe una notificación o entrada para crear reseña.
5. Dada una reserva no finalizada, cancelada o ya reseñada, cuando se revisa el flujo, entonces existe estado o anotación que impide reseñar.
6. Dado el alcance semana 10, cuando se revisan los wireframes navegables, entonces no se requieren landing pages, social login, favoritos, perfil extendido, panel admin global ni QR/código.
7. Dado un issue cubierto por una pantalla, cuando se revisa la trazabilidad, entonces puede identificarse el issue relacionado.
8. Dado el cierre del issue, cuando se entregue el resultado, entonces existe un enlace o referencia clara al archivo, dashboard o prototipo.

## Definition of Done

- Existe un conjunto de wireframes navegables de baja fidelidad para el MVP semana 10.
- Existe navegación básica entre búsqueda, detalle, reserva, notificación y reseña.
- Existe navegación básica para gestión owner de complejo/canchas.
- Los estados vacíos, errores y restricciones relevantes están representados o anotados.
- Existe trazabilidad con los issues funcionales cubiertos.
- El entregable no agrega funcionalidades post-MVP como requeridas.

---
Current issue: TheMonstersP4/mejengueros-app#45
