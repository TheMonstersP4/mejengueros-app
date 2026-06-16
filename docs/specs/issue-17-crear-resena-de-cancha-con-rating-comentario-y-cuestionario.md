# Issue #17: Crear reseña de cancha desde reserva finalizada

## Título

Crear reseña de cancha desde una reserva finalizada.

## Nota de re-alcance MVP

Por directriz de la profesora, la validación por código temporal o QR queda obsoleta. La confianza de la reseña se basa en una reserva real que ya concluyó. Para semana 10, la reseña activa se limita a rating y comentario básico; cuestionarios, imágenes, métricas estructuradas y reglas especiales de `1 estrella` quedan post-MVP salvo decisión explícita posterior.

## Objetivo

Definir el flujo para que un `mejenguero` autenticado pueda crear una reseña básica de una cancha únicamente cuando tiene una reserva finalizada asociada a esa cancha.

## Historia de usuario

Como `mejenguero`,
quiero dejar una reseña después de que termina mi reserva,
para compartir mi experiencia real en la cancha que utilicé.

## Alcance

- Crear reseña desde una reserva finalizada.
- Permitir iniciar la reseña desde una notificación post-reserva definida en `#51`.
- Asociar la reseña a la reserva, usuario y cancha.
- Registrar rating obligatorio según `#18`.
- Registrar comentario básico opcional u obligatorio según la decisión de implementación mínima.
- Evitar más de una reseña por la misma reserva.

## Fuera de alcance

- Validación por QR o código de 6 dígitos.
- Crear o gestionar reservas; eso pertenece a `#50`.
- Pagos, pasarela o cantidad de participantes.
- Cuestionario breve obligatorio, métricas estructuradas o señales avanzadas; eso queda post-MVP en `#20` y `#22`.
- Imagen/evidencia obligatoria o regla especial de `1 estrella`; eso queda post-MVP en `#19` y `#28`.
- Moderación humana, edición posterior, respuestas del dueño o votos útiles.
- Reseñas de usuarios sin reserva finalizada.

## Reglas de negocio

1. El usuario debe estar autenticado.
2. Debe existir una reserva asociada al usuario y a la cancha.
3. La reserva debe haber finalizado; una reserva futura o en curso no habilita reseña.
4. Una reserva permite como máximo una reseña.
5. La reseña debe quedar vinculada a `reservationId`, `userId` y `canchaId`.
6. Si la reserva fue cancelada o no está vigente para reseña, el sistema debe rechazar la creación.
7. La notificación de `#51` facilita el acceso, pero la validación real está en la reserva finalizada.
8. Para semana 10, la reseña debe mantenerse simple: rating visible y comentario básico.
9. El rating se define en `#18` y es obligatorio para cerrar la reseña.

## Flujo principal

1. El sistema detecta que una reserva del usuario ya finalizó.
2. El sistema crea o muestra una notificación de reseña según `#51`.
3. El usuario presiona la notificación o accede desde sus reservas finalizadas.
4. El sistema valida que la reserva pertenece al usuario, ya finalizó y no tiene reseña previa.
5. El usuario selecciona rating y completa el comentario básico si aplica.
6. El sistema guarda la reseña asociada a la reserva y actualiza el rating visible de la cancha.

## Criterios de aceptación

1. Dado un usuario con una reserva finalizada sin reseña, cuando abre el flujo de reseña, entonces puede registrar una reseña para esa cancha.
2. Dado un usuario sin reserva finalizada para esa cancha, cuando intenta crear una reseña, entonces el sistema bloquea la operación.
3. Dada una reserva futura o en curso, cuando el usuario intenta reseñar, entonces el sistema indica que la reseña todavía no está disponible.
4. Dada una reserva que ya tiene reseña, cuando el usuario intenta crear otra reseña desde la misma reserva, entonces el sistema bloquea el duplicado.
5. Dada una notificación post-reserva, cuando el usuario la presiona, entonces el sistema abre el flujo de reseña asociado a esa reserva.
6. Dado un rating válido de `1` a `5`, cuando el usuario envía la reseña, entonces el sistema guarda el rating según `#18`.
7. Dado el re-alcance MVP, cuando se revisa este issue, entonces no queda dependencia funcional de QR, código temporal, cuestionario, imagen, métricas estructuradas ni regla especial de `1 estrella`.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- La reseña se valida por reserva finalizada.
- La reseña se vincula a reserva, usuario y cancha.
- La duplicidad se controla por reserva.
- El flujo puede abrirse desde notificación post-reserva.
- Rating obligatorio integrado con `#18`.
- QR/código, cuestionario, imágenes y métricas avanzadas quedan explícitamente fuera del alcance activo.

## Notas de partición

- `#17` responde: `cómo el mejenguero crea una reseña básica después de una reserva finalizada`.
- `#18` define el rating en estrellas.
- `#19`, `#20`, `#22` y `#28` quedan como extensiones post-MVP salvo decisión explícita posterior.
- `#21` responde: `cómo el dueño consulta reseñas recibidas`.
- `#51` responde: `cómo se notifica al usuario que puede reseñar`.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/16
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#16
Current issue: TheMonstersP4/mejengueros-app#17
