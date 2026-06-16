# Issue #19: Justificar obligatoriamente reseña de 1 estrella

## Título

Definir la regla MVP por la cual una reseña con `1 estrella` debe incluir una justificación obligatoria y al menos una imagen antes de poder enviarse.

## Relación con #17, #18, #20, #22

- `#19` refina una regla específica dentro del flujo de creación de reseña definido en `#17`.
- `#18` define la calificación general de `1` a `5` estrellas y es dependencia directa de `#19`.
- `#19` existe únicamente para la rama especial en la que el `rating` seleccionado es exactamente `1 estrella`.
- `#20` permanece separado porque define el cuestionario breve de experiencia y no la justificación puntual de una reseña de `1 estrella`.
- `#22` permanece separado porque profundiza KPIs o métricas estructuradas derivadas del cuestionario, no esta validación.
- La separación debe ser explícita: `#18` resuelve `cómo se selecciona el rating`; `#19` resuelve `qué requisito extra aparece sólo con 1 estrella`; `#20` y `#22` cubren señales estructuradas distintas.

## Objetivo

Definir un comportamiento MVP claro y revisable para exigir una justificación y una imagen cuando un `mejenguero` asigna la calificación más baja posible a una cancha, de modo que el producto capture contexto cualitativo y evidencia mínima sin agregar moderación compleja, análisis semántico ni lógica avanzada fuera del alcance inicial.

## Historia de usuario

Como `mejenguero`,
quiero que una reseña de `1 estrella` me pida explicar brevemente el motivo y adjuntar una imagen,
para dejar contexto útil y evidencia mínima sobre una experiencia claramente negativa antes de publicar esa reseña.

## Alcance

- Definir que la justificación adicional y la imagen son obligatorias sólo cuando el `rating` seleccionado es exactamente `1 estrella`.
- Definir que la justificación y la imagen forman parte del flujo de reseña de `#17`, pero únicamente dentro de la rama activada por `#18` al seleccionar `1 estrella`.
- Establecer que la reseña no puede enviarse mientras falte esa justificación obligatoria o la imagen requerida.
- Establecer que la justificación se captura como texto libre breve y entendible para este MVP.
- Definir el comportamiento esperado si el usuario cambia el `rating` después de haber activado esta regla.

## Fuera de alcance

- Definir el selector general de estrellas, la escala `1` a `5` o la interacción base del `rating`; eso pertenece a `#18`.
- Definir reglas equivalentes para `2`, `3`, `4` o `5` estrellas.
- Convertir esta justificación en cuestionario estructurado, lista de verificación o captura de KPIs; eso pertenece a `#20` y `#22`.
- Diseñar moderación avanzada, NLP, clasificación automática, detección de toxicidad, resúmenes automáticos o scoring semántico.
- Definir flujos de apelación, denuncias, edición posterior, auditoría o revisión manual por parte de admins.
- Comprometer contratos técnicos de servidor, storage, analytics o validaciones de implementación específicas.

## Reglas de negocio

1. La justificación obligatoria aplica sólo si el `rating` vigente de la reseña es exactamente `1 estrella`.
2. Si el `rating` es `2`, `3`, `4` o `5` estrellas, esta regla no debe activarse.
3. La reseña no debe poder enviarse si tiene `1 estrella` y la justificación requerida o la imagen obligatoria están ausentes.
4. Para este MVP, justificar significa completar un texto libre breve con motivo suficiente y adjuntar al menos una imagen para acompañar la calificación de `1 estrella`.
5. `#19` no redefine cómo se selecciona el `rating`; consume el resultado de `#18`.
6. Si el usuario cambia el `rating` de `1 estrella` a un valor mayor antes del envío, la obligatoriedad de la justificación deja de aplicar en ese momento.
7. Si el usuario vuelve a seleccionar `1 estrella` antes del envío, la obligatoriedad debe reactivarse.
8. Esta regla debe expresarse como validación funcional de producto, no como una política de moderación o de análisis automático del contenido.

## Qué significa justificar una reseña de 1 estrella en este MVP

- Significa que el usuario debe escribir una explicación breve en texto libre y adjuntar una imagen antes de publicar.
- Significa que el producto pide contexto mínimo y evidencia visual básica sobre por qué la experiencia fue calificada con la nota más baja.
- Significa que la explicación y la imagen acompañan a la reseña sólo en el caso de `1 estrella`.
- No significa validar la veracidad del contenido, su calidad editorial o su tono con reglas inteligentes.
- No significa transformar la explicación en categorías estructuradas, KPIs o insights automáticos.
- No significa exigir evidencia adicional, documentación extra ni fotos para calificaciones de `2` a `5` estrellas.

## Flujo principal

1. El `mejenguero` inicia la creación de una reseña dentro del flujo definido en `#17`.
2. El usuario selecciona el `rating` general mediante el comportamiento resuelto en `#18`.
3. Si la selección actual es exactamente `1 estrella`, el sistema activa la regla de justificación obligatoria.
4. El sistema solicita al usuario una justificación textual breve y al menos una imagen dentro del flujo de reseña.
5. El usuario completa la justificación requerida y adjunta la imagen obligatoria.
6. El sistema valida que la reseña con `1 estrella` ya incluye esa justificación y esa imagen.
7. Si el resto del flujo requerido también está completo, la reseña puede enviarse.

## Casos alternos/validaciones

- Si el usuario selecciona `1 estrella` y no completa la justificación o no adjunta una imagen, el sistema debe bloquear el envío y comunicar que ambos elementos son obligatorios.
- Si el usuario selecciona `2`, `3`, `4` o `5` estrellas, el sistema no debe exigir la justificación ni la imagen definida en `#19`.
- Si el usuario primero selecciona `1 estrella`, escribe una justificación y luego cambia el `rating` a un valor mayor, la reseña puede continuar sin depender de esa validación especial.
- Si el usuario cambia desde un valor mayor a `1 estrella`, el sistema debe activar la exigencia de justificación antes del envío.
- Si existe texto en la justificación pero no alcanza el mínimo funcional definido por producto, el sistema debe tratarla como incompleta.
- Si falla el guardado final de la reseña, el sistema debe informar que la publicación no se completó; no debe asumir éxito por el solo hecho de haber escrito la justificación.

## Datos de entrada

- Contexto de creación de reseña definido en `#17`.
- Valor de `rating` seleccionado en `#18`.
- Texto de justificación ingresado por el usuario cuando el `rating` vigente es `1 estrella`.
- Imagen adjunta obligatoria cuando el `rating` vigente es `1 estrella`.

Notas:

- Este issue define inputs funcionales, no contratos técnicos de API o persistencia.
- El contenido del cuestionario breve y de métricas derivadas permanece fuera de este issue y se define en `#20` y `#22`.

## Datos de salida

- Señal funcional de que una reseña con `1 estrella` quedó correctamente justificada y evidenciada para este MVP.
- Bloqueo de envío cuando una reseña con `1 estrella` no cumple la justificación o la imagen requerida.
- Texto de justificación e imagen asociados a la reseña sólo cuando aplica esta regla especial.

## Dependencias

- `#17` para el flujo padre de creación de reseña.
- `#18` para la selección y validez del `rating` general en estrellas.
- `#20` como capacidad separada de cuestionario breve de experiencia, con la que no debe mezclarse esta justificación.
- `#22` como capacidad separada de KPIs o métricas estructuradas derivadas del cuestionario.
- `#28` para adjuntar la imagen obligatoria cuando el `rating` es `1 estrella`.
- Disponibilidad del contexto de cancha y del `mejenguero` dentro del flujo de reseña.

## Criterios de aceptación

1. Dado un `mejenguero` que está creando una reseña dentro de `#17`, cuando el `rating` seleccionado en `#18` es exactamente `1 estrella`, entonces el sistema exige una justificación y una imagen antes de permitir el envío.
2. Dado un `mejenguero` con una reseña de `1 estrella`, cuando intenta enviarla sin justificación válida o sin imagen, entonces el sistema bloquea la acción y comunica la validación.
3. Dado un `mejenguero` con una reseña de `1 estrella`, cuando completa la justificación requerida, adjunta una imagen y el resto del flujo obligatorio está completo, entonces la reseña puede continuar hacia su envío.
4. Dado un `mejenguero` que selecciona `2`, `3`, `4` o `5` estrellas, cuando avanza en el flujo, entonces el sistema no exige la justificación ni la imagen definida en `#19`.
5. Dado un `mejenguero` que había activado la regla de `1 estrella`, cuando cambia el `rating` a un valor mayor antes del envío, entonces la obligatoriedad de la justificación deja de aplicar.
6. Dado el alcance MVP de este issue, cuando se revisa el spec, entonces queda claro que la justificación se resuelve como texto libre obligatorio y no como moderación avanzada, NLP ni cuestionario estructurado.
7. Dada la relación entre issues, cuando se revisa `#19`, entonces queda claro que `#18` sigue siendo dueño del comportamiento general del selector de estrellas y que `#20` y `#22` siguen siendo dueños de cuestionario y KPIs respectivamente.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad que `#19` refina el flujo de `#17` y depende directamente de `#18`.
- Queda explícito que la justificación y la imagen obligatoria aplican sólo para `1 estrella` exacta.
- Queda explícito que `2` a `5` estrellas no activan esta regla.
- Queda claro que el MVP resuelve la justificación como texto libre obligatorio y simple, acompañado por una imagen obligatoria sólo en esta rama.
- Queda claro que el envío se bloquea si falta la justificación o la imagen requerida en una reseña de `1 estrella`.
- Queda documentado qué pasa si el usuario cambia el `rating` antes de enviar.
- Queda explícita la separación respecto de `#18`, `#20` y `#22`.
- No se agregan comportamientos de moderación, NLP, scoring ni complejidad fuera del MVP.

## Notas de partición con #18

- `#18` responde: `cómo se selecciona, valida e interpreta la calificación general de 1 a 5 estrellas`.
- `#19` responde: `qué requisito adicional de justificación e imagen aparece cuando esa calificación es exactamente 1 estrella`.
- Si la decisión pertenece al selector, escala, obligatoriedad general del rating o edición de estrellas, cae en `#18`.
- Si la decisión pertenece al texto obligatorio, imagen obligatoria, bloqueo específico o activación/desactivación de la regla especial de `1 estrella`, cae en `#19`.
- `#19` no debe absorber comportamiento general del selector de estrellas; sólo debe colgarse de la rama especial ya disparada por `#18`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/18
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#18
Current issue: TheMonstersP4/mejengueros-app#19