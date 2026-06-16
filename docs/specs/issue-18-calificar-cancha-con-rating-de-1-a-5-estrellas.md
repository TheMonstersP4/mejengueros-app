# Issue #18: Calificar cancha con rating de 1 a 5 estrellas

## Título

Definir el rating visible de 1 a 5 estrellas dentro de la reseña MVP.

## Nota de re-alcance MVP

Para el roadmap activo de Sprint 5, `#18` se limita al rating obligatorio y visible de `1` a `5` estrellas dentro de una reseña creada desde una reserva finalizada. La regla especial de `1 estrella` con justificación e imagen queda post-MVP en `#19`, porque `#19` y la carga de imágenes no forman parte de la capacidad activa del demo semana 10.

## Relación con issues coordinados

- `#17` define la creación de reseña desde una reserva finalizada.
- `#18` define la calificación general visible en estrellas.
- `#21` permite que el dueño consulte reseñas y ratings recibidos.
- `#19`, `#20` y `#22` quedan como extensiones post-MVP salvo decisión explícita de traerlas al alcance activo.

## Objetivo

Permitir que el `mejenguero` seleccione un rating entero de `1` a `5` estrellas al crear una reseña post-reserva, y que ese rating pueda mostrarse como señal visible de la experiencia en la cancha.

## Historia de usuario

Como `mejenguero`,
quiero calificar una cancha con estrellas después de mi reserva,
para expresar de forma simple mi evaluación general de la experiencia.

## Alcance

- Mostrar escala de `1` a `5` estrellas.
- Permitir seleccionar exactamente un valor entero de la escala.
- Permitir modificar la selección antes de enviar la reseña.
- Exigir rating para completar la reseña de `#17`.
- Guardar o exponer el rating como parte de la reseña.
- Permitir que el rating sea visible en detalle/listado o en reseñas recibidas según `#16`, `#17` y `#21`.

## Fuera de alcance

- Justificación obligatoria para `1 estrella`; queda en `#19` como post-MVP.
- Imagen/evidencia fotográfica obligatoria para `1 estrella`; queda fuera del demo activo.
- Cuestionario breve obligatorio de experiencia; queda en `#20` como post-MVP salvo decisión contraria.
- Métricas estructuradas derivadas; quedan en `#22`.
- Medias estrellas, decimales, sliders, emojis o escalas alternativas.
- Ranking avanzado, analítica, dashboards o moderación.

## Reglas de negocio

1. El rating pertenece a una reseña creada desde `#17`.
2. La escala válida es cerrada y entera: `1`, `2`, `3`, `4` o `5` estrellas.
3. No existen medias estrellas ni valores fuera del rango.
4. El usuario debe seleccionar un rating para enviar la reseña.
5. El usuario puede cambiar la selección antes del envío.
6. Para el MVP semana 10, seleccionar `1 estrella` no activa por sí mismo una dependencia obligatoria con imagen o justificación; esa regla queda post-MVP en `#19`.
7. El rating debe poder mostrarse como señal visible en la cancha o en el listado de reseñas.

## Flujo principal

1. El `mejenguero` abre la reseña desde una reserva finalizada según `#17`.
2. El sistema muestra una escala de `1` a `5` estrellas.
3. El usuario selecciona un valor.
4. El sistema refleja visualmente la selección.
5. El usuario puede cambiar la selección antes de enviar.
6. Al enviar la reseña, el sistema valida que el rating sea obligatorio y esté dentro del rango permitido.
7. El rating queda asociado a la reseña y puede mostrarse en vistas de cancha o reseñas.

## Casos alternos/validaciones

- Si el usuario no selecciona rating, el sistema bloquea el envío de la reseña.
- Si el valor seleccionado está fuera de `1` a `5`, el sistema rechaza la reseña.
- Si el usuario cambia de opinión antes de enviar, el sistema actualiza la selección.
- Si el flujo de reseña se interrumpe antes del envío, la selección parcial no cuenta como reseña publicada.

## Datos de entrada

- Contexto de reseña asociado a una reserva finalizada.
- Usuario autenticado que crea la reseña.
- Valor entero de rating entre `1` y `5`.

## Datos de salida

- Rating válido asociado a la reseña.
- Señal visual de rating completado.
- Rating disponible para visualización en el detalle/listado o en reseñas recibidas.

## Dependencias

- `#17` para crear la reseña desde una reserva finalizada.
- `#21` para que el dueño consulte reseñas recibidas y pueda ver rating.
- `#16` si el detalle de cancha muestra rating visible.

## Criterios de aceptación

1. Dado un `mejenguero` creando una reseña desde `#17`, cuando visualiza el bloque de calificación, entonces el sistema presenta una escala clara de `1` a `5` estrellas.
2. Dado un `mejenguero` que selecciona una cantidad válida de estrellas, cuando interactúa con el componente, entonces el sistema refleja visualmente la selección actual.
3. Dado un `mejenguero` que no seleccionó rating, cuando intenta publicar la reseña, entonces el sistema bloquea la acción por falta de calificación.
4. Dado un `mejenguero` que cambia su selección antes del envío, cuando confirma la reseña, entonces se guarda el último valor seleccionado.
5. Dado un valor fuera del rango `1` a `5`, cuando se intenta persistir, entonces el sistema lo rechaza.
6. Dado el alcance de Sprint 5, cuando se revisa `#18`, entonces no depende de `#19`, imágenes ni evidencia fotográfica obligatoria.
7. Dada una reseña publicada con rating, cuando se visualizan reseñas o información de cancha, entonces el rating puede mostrarse como señal visible.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define escala obligatoria de `1` a `5` estrellas enteras.
- El spec define que el rating pertenece a una reseña post-reserva.
- El spec permite modificar la selección antes de enviar.
- El spec no depende de `#19` ni de imágenes para el Sprint 5 activo.
- El rating queda disponible para visualización en el flujo MVP.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/17
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#17
Current issue: TheMonstersP4/mejengueros-app#18
