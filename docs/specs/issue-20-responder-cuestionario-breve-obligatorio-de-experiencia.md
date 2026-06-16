# Issue #20: Responder cuestionario breve obligatorio de experiencia

## Título

Definir el cuestionario breve de experiencia obligatorio dentro del flujo MVP de reseñas de cancha, usando preguntas y respuestas cerradas, predefinidas y no configurables por administración.

## Relación con #17, #18, #19, #22

- `#20` refina el bloque de cuestionario dentro del flujo de creación de reseña definido en `#17`.
- `#17` define la capacidad padre y ya establece que la reseña MVP requiere `rating` + `comentario` + `cuestionario breve obligatorio`, con `fotos` opcionales.
- `#18` define la calificación general en estrellas y permanece separado del cuestionario estructurado.
- `#19` define la validación especial de justificación e imagen obligatoria cuando el `rating` es exactamente `1 estrella`, y no reemplaza el cuestionario de `#20`.
- `#22` profundiza la interpretación estructurada de las respuestas del cuestionario definido en `#20`, sin redefinir el cuestionario base ni su obligatoriedad.
- La separación debe ser explícita: `#18` resuelve `cómo califico con estrellas`; `#19` resuelve `qué validación adicional ocurre con 1 estrella`; `#20` resuelve `qué bloque breve y estructurado de experiencia debe completar siempre el usuario`; `#22` resuelve `qué significado medible tienen esas respuestas una vez capturadas`.

## Objetivo

Definir un bloque breve y obligatorio de preguntas estructuradas dentro de la reseña MVP, para que el producto capture señales comparables sobre la experiencia vivida en la cancha sin depender sólo del texto libre ni introducir complejidad innecesaria, configuraciones administrativas o analítica avanzada fuera del alcance inicial.

## Historia de usuario

Como `mejenguero`,
quiero completar un cuestionario breve y simple al dejar mi reseña,
para aportar señales concretas y comparables sobre mi experiencia además del rating y el comentario.

## Alcance

- Definir que el cuestionario breve de experiencia forma parte obligatoria del flujo de reseña MVP de `#17`.
- Definir que el cuestionario se compone de una cantidad acotada de preguntas breves, estructuradas y cerradas.
- Establecer que las preguntas del MVP provienen de un catálogo predefinido por producto.
- Establecer que las respuestas posibles también son cerradas, medibles y predefinidas para cada pregunta.
- Definir que la reseña no puede enviarse si falta responder el cuestionario obligatorio.
- Mantener el spec orientado a comportamiento de producto y simplicidad MVP, sin absorber la definición del significado estructurado que corresponde a `#22`.

## Fuera de alcance

- Definir el flujo padre completo de creación de reseña; eso pertenece a `#17`.
- Definir el selector general de `rating` en estrellas; eso pertenece a `#18`.
- Definir la justificación obligatoria para reseñas de `1 estrella`; eso pertenece a `#19`.
- Definir el modelo detallado de KPIs, scentralcards, agregaciones, reporting o explotación analítica de las respuestas; eso pertenece a `#22`.
- Habilitar preguntas abiertas dentro del cuestionario breve MVP.
- Habilitar que usuarios, admins o dueños creen, editen, activen, desactiven o reordenen preguntas y respuestas desde una UI de mantenimiento en este MVP.
- Diseñar un motor dinámico de formularios, versionado complejo de cuestionarios, segmentación por tipo de cancha o experimentación avanzada.
- Definir contratos técnicos de persistencia, API, eventos o analytics de implementación.

## Reglas de negocio

1. El cuestionario breve de experiencia es obligatorio dentro del flujo MVP de reseña definido en `#17`.
2. Para este MVP, una reseña no se considera completa si falta responder el cuestionario breve requerido.
3. El cuestionario de `#20` complementa al `rating` y al `comentario`; no los reemplaza.
4. Las preguntas del cuestionario deben pertenecer a un catálogo cerrado y predefinido por producto para el MVP.
5. Las respuestas posibles de cada pregunta deben ser cerradas, medibles y predefinidas para el MVP.
6. En este MVP, el cuestionario no es configurable por usuario ni por administrador desde una UI de mantenimiento.
7. El cuestionario debe ser breve: su diseño debe priorizar baja fricción y rápida completitud dentro del flujo de reseña.
8. `#20` define el bloque funcional de preguntas y respuestas estructuradas, pero no debe tragarse el significado métrico detallado que corresponde a `#22`.
9. Si alguna respuesta obligatoria del cuestionario falta o es inválida respecto del catálogo cerrado vigente, el sistema debe bloquear el envío de la reseña.
10. El lenguaje del spec debe mantenerse en nivel producto/comportamiento MVP, sin inventar capacidades operativas o administrativas no acordadas.

## Qué significa cuestionario breve de experiencia en este MVP

- Significa un bloque corto de preguntas estructuradas que el usuario debe completar como parte de la reseña.
- Significa que esas preguntas buscan capturar señales concretas y comparables de la experiencia.
- Significa que las respuestas no son texto libre, sino opciones cerradas y medibles.
- Significa que el contenido del cuestionario está previamente definido por producto para el MVP.
- Significa que el usuario no configura qué preguntas aparecen ni cómo responderlas más allá de elegir entre opciones válidas.
- Significa que no existe una UI administrativa para mantener este cuestionario durante el MVP.
- No significa crear un formulario largo, adaptable o altamente parametrizable.
- No significa resolver todavía la capa completa de KPIs, agregaciones o lectura analítica avanzada de esas respuestas; eso se profundiza en `#22`.

## Flujo principal

1. El `mejenguero` inicia la creación de una reseña dentro del flujo definido en `#17`.
2. El sistema presenta los bloques obligatorios de la reseña MVP: `rating`, `comentario` y `cuestionario breve de experiencia`, dejando `fotos` como opcionales salvo la regla de `1 estrella` definida en `#19`.
3. El sistema muestra el cuestionario breve usando preguntas predefinidas del catálogo MVP vigente.
4. Para cada pregunta, el usuario selecciona una respuesta válida dentro del conjunto cerrado disponible.
5. El sistema registra que el cuestionario quedó completo cuando todas las respuestas obligatorias requeridas fueron seleccionadas válidamente.
6. El usuario continúa con el resto del flujo de reseña.
7. Al intentar enviar la reseña, el sistema valida que el cuestionario obligatorio esté completo junto con el resto de los mínimos requeridos.
8. Si todo el flujo requerido está completo, la reseña puede enviarse.

## Casos alternos/validaciones

- Si el usuario intenta enviar la reseña sin completar una o más respuestas obligatorias del cuestionario, el sistema debe bloquear el envío y comunicar qué falta completar.
- Si una respuesta seleccionada no pertenece al conjunto cerrado válido para la pregunta correspondiente, el sistema debe tratarla como inválida.
- Si el usuario completa `rating` y `comentario` pero omite el cuestionario breve obligatorio, la reseña sigue estando incompleta para el MVP.
- Si el `rating` es `1 estrella`, además del cuestionario obligatorio de `#20`, debe seguir aplicando la justificación especial definida en `#19`.
- Si el usuario no adjunta `fotos`, la reseña igualmente puede enviarse siempre que `rating`, `comentario`, cuestionario obligatorio y demás validaciones requeridas estén completos, salvo cuando aplique la regla de `1 estrella` de `#19`.
- Si falla el guardado final de la reseña, el sistema no debe asumir éxito sólo porque el cuestionario haya sido respondido.
- Si en el futuro existieran nuevas variantes del cuestionario, este spec no asume personalización dinámica en el MVP actual.

## Datos de entrada

- Contexto de creación de reseña definido en `#17`.
- Identificador o contexto del `mejenguero` que está creando la reseña.
- Catálogo MVP vigente de preguntas predefinidas del cuestionario breve.
- Conjunto cerrado de respuestas válidas por cada pregunta del cuestionario.
- Selecciones realizadas por el usuario para cada respuesta obligatoria del cuestionario.

Notas:

- Este issue define inputs funcionales, no el contrato técnico exacto de API o persistencia.
- La interpretación métrica o estructurada de estas respuestas se refina en `#22`.

## Datos de salida

- Señal funcional de que el cuestionario breve obligatorio quedó completo dentro de la reseña.
- Respuestas estructuradas y cerradas asociadas a la reseña creada.
- Bloqueo de envío cuando faltan respuestas obligatorias o alguna respuesta no es válida respecto del catálogo cerrado definido.
- Base funcional para que `#22` pueda asignar significado estructurado o métrico a partir de estas respuestas.

## Dependencias

- `#17` para el flujo padre de creación de reseña y la definición de obligatoriedad del bloque dentro de la reseña MVP.
- `#18` para la convivencia con el `rating` general en estrellas dentro del mismo flujo.
- `#19` para la validación adicional de justificación e imagen aplicable a reseñas con `1 estrella`.
- Disponibilidad de un catálogo MVP predefinido de preguntas y respuestas cerradas para el cuestionario.

Notas:

- `#22` depende de este cuestionario base para interpretar sus respuestas, pero esa relación es de downstream semántico y no cambia que `#20` sea dueño de la definición del cuestionario y de su completitud obligatoria.

## Criterios de aceptación

1. Dado un `mejenguero` que está creando una reseña dentro de `#17`, cuando avanza por el flujo MVP, entonces el sistema presenta un cuestionario breve de experiencia como bloque obligatorio junto al `rating` y al `comentario`.
2. Dado el alcance MVP de `#20`, cuando se revisa el spec, entonces queda explícito que el cuestionario es obligatorio y que la reseña no puede enviarse sin completarlo.
3. Dado el cuestionario breve de experiencia, cuando se revisa el spec, entonces queda explícito que las preguntas provienen de un catálogo predefinido y cerrado para este MVP.
4. Dado el cuestionario breve de experiencia, cuando se revisa el spec, entonces queda explícito que las respuestas posibles también son cerradas, medibles y predefinidas para este MVP.
5. Dado un `mejenguero` que intenta enviar una reseña con respuestas faltantes o inválidas en el cuestionario, cuando ejecuta la acción de publicar, entonces el sistema bloquea el envío y comunica la validación correspondiente.
6. Dado un `mejenguero` que completa `rating`, `comentario` y el cuestionario obligatorio con respuestas válidas, cuando el resto del flujo requerido también está completo, entonces la reseña puede continuar hacia su envío.
7. Dado el alcance MVP de este issue, cuando se revisa la spec, entonces queda claro que no existe UI administrativa para crear, editar o mantener preguntas y respuestas del cuestionario.
8. Dada la relación entre `#20` y `#22`, cuando se revisa este issue, entonces queda claro que `#20` define el cuestionario base y obligatorio, y que `#22` profundiza sólo el significado estructurado de sus respuestas sin absorber este bloque funcional.
9. Dado un `mejenguero` con una reseña de `1 estrella`, cuando revisa los requisitos del flujo, entonces queda claro que el cuestionario obligatorio de `#20` convive con la justificación especial definida en `#19`.
10. Dado el enfoque MVP, cuando se revisa `#20`, entonces queda claro que el cuestionario debe mantenerse breve, estructurado y simple, sin motor dinámico ni comportamiento administrativo avanzado.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad que `#20` refina una parte del flujo definido en `#17`.
- Queda explícito que el cuestionario breve de experiencia es obligatorio en el flujo MVP de reseña.
- Queda explícito que la reseña MVP exige `rating` + `comentario` + `cuestionario breve obligatorio`, con `fotos` opcionales salvo cuando `#19` las exige por `1 estrella`.
- Queda claro que las preguntas del cuestionario son predefinidas, cerradas y no configurables en este MVP.
- Queda claro que las respuestas también son cerradas, medibles y predefinidas en este MVP.
- Queda claro que no existe UI administrativa de mantenimiento del cuestionario en esta etapa.
- Queda clara la separación funcional con `#18` y `#19` dentro del flujo de reseña.
- Queda clara la partición con `#22`, que conserva la responsabilidad sobre el significado métrico o estructurado derivado de las respuestas.
- No se agregan comportamientos de formularios dinámicos, administración avanzada ni analítica fuera del MVP.

## Notas de partición con #22

- `#20` responde: `qué bloque breve, obligatorio y estructurado de preguntas debe completar el usuario dentro de la reseña MVP`.
- `#22` responde: `cómo se interpreta el significado métrico o estructurado de las respuestas de ese cuestionario`.
- Si la decisión pertenece a `qué preguntas hay`, `qué tipo de respuestas cerradas permite cada una` o `cuándo se exige completar el cuestionario`, cae en `#20`.
- Si la decisión pertenece a `cómo modelar, nombrar, normalizar, explotar o exponer el significado estructurado de esas respuestas`, cae en `#22`.
- `#20` no debe absorber dashboards, analítica, agregaciones ni definiciones exhaustivas de KPI.
- `#22` no debe redefinir la obligatoriedad del cuestionario ni convertirlo en un sistema configurable; parte del bloque base ya definido por `#20`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/19
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#19
Current issue: TheMonstersP4/mejengueros-app#20