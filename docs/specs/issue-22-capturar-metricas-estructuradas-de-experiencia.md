# Issue #22: Capturar métricas estructuradas de experiencia

## Título

Definir cómo las respuestas cerradas del cuestionario breve obligatorio de experiencia se interpretan y tratan como señales estructuradas y medibles dentro de la reseña MVP, para habilitar lectura básica de la experiencia reportada sin redefinir el cuestionario ni agregar capacidades de analítica avanzada.

## Relación con #17, #20

- `#22` profundiza la interpretación de respuestas del cuestionario definido en `#20` y forma parte funcional de la experiencia de reseña definida en `#17`.
- `#17` define la capacidad padre de crear una reseña MVP de cancha y establece que la reseña incluye `rating`, `comentario`, `cuestionario breve obligatorio` y `fotos` opcionales.
- `#20` define el bloque obligatorio de preguntas y respuestas cerradas del cuestionario breve de experiencia.
- `#22` no redefine ese cuestionario: toma las respuestas predefinidas de `#20` y les asigna significado medible, comparable y explotable como métricas básicas del producto.
- La separación debe quedar explícita: `#20` responde `qué preguntas y respuestas cerradas se completan`; `#22` responde `qué significado estructurado tienen esas respuestas para interpretar la experiencia en el MVP`.
- `#22` ya no representa FAQ, no corresponde a contenido informativo ni debe reinterpretarse como un flujo de respuestas cargadas por dueño o administración.

## Objetivo

Definir la capa MVP que da significado medible a las respuestas estructuradas del cuestionario obligatorio de `#20`, para que el producto pueda contar con evidencia básica sobre la experiencia reportada por los usuarios y responder de forma trazable si la aplicación está logrando una experiencia satisfactoria, sin agregar dashboards, BI, configuración administrativa ni analítica avanzada.

## Historia de usuario

Como equipo de producto,
quiero que las respuestas cerradas del cuestionario obligatorio de la reseña queden interpretadas como métricas estructuradas de experiencia,
para poder interpretar de forma medible cómo fue la experiencia reportada por los usuarios más allá del comentario libre y del rating general.

## Alcance

- Definir que las respuestas del cuestionario obligatorio de `#20` deben conservarse con estructura suficiente para ser interpretadas como señales medibles de experiencia.
- Establecer que cada respuesta válida del cuestionario representa un dato estructurado y comparable, no sólo una selección visual de formulario.
- Definir que la reseña publicada deja como salida tanto el contenido de la reseña como las señales estructuradas derivadas del cuestionario.
- Aclarar que estas métricas del MVP son de lectura básica, orientadas a medir experiencia reportada, no a construir una plataforma analítica.
- Mantener el issue en nivel producto/comportamiento, sin bajar a contratos técnicos detallados de implementación.

## Fuera de alcance

- Redefinir el cuestionario breve, sus preguntas, sus opciones o su obligatoriedad; eso pertenece a `#20`.
- Redefinir el flujo padre de crear una reseña; eso pertenece a `#17`.
- Crear dashboards, scentralcards visuales, paneles administrativos, reportes BI o vistas avanzadas de explotación.
- Crear UI de configuración para alta, baja, edición, versionado o mantenimiento de métricas, preguntas o respuestas.
- Agregar analítica predictiva, segmentación avanzada, benchmarking, tendencias complejas o cruces estadísticos sofisticados.
- Convertir este issue en FAQ, base de conocimiento o contenido informativo.
- Hacer que las métricas sean cargadas manualmente por dueño, admin u otro actor distinto del usuario que completa la reseña.
- Definir fórmulas exhaustivas de scoring compuesto si no son necesarias para sostener la medición MVP.

## Reglas de negocio

1. `#22` depende funcionalmente de que exista el cuestionario obligatorio de `#20` ya definido con preguntas y respuestas cerradas.
2. En este MVP, las métricas estructuradas de experiencia se derivan exclusivamente de respuestas predefinidas seleccionadas por el usuario dentro del cuestionario obligatorio.
3. Ninguna métrica de `#22` puede exigir preguntas nuevas, pasos extra ni campos manuales por fuera del cuestionario base de `#20`.
4. Cada respuesta válida del cuestionario debe poder tratarse como una señal estructurada y comparable entre reseñas.
5. La interpretación estructurada debe quedar asociada a la reseña concreta en la que el usuario completó el cuestionario.
6. Si una respuesta del cuestionario es inválida o no pertenece al catálogo cerrado definido por `#20`, no debe considerarse una señal métrica válida.
7. El rating general en estrellas de `#17/#18` sigue siendo una señal separada de estas métricas derivadas; no las reemplaza ni queda reemplazado por ellas.
8. El comentario libre sigue siendo contexto cualitativo complementario; no reemplaza la interpretación estructurada de `#22`.
9. En este MVP, interpretar métricas estructuradas significa dar significado medible a respuestas ya capturadas; no implica exponerlas obligatoriamente en una UI de reporting.
10. El lenguaje del spec debe mantenerse en alcance MVP y orientado a comportamiento de producto, sin inventar operaciones administrativas o analítica fuera de lo acordado.

## Qué significa interpretar métricas estructuradas en este MVP

- Significa que las respuestas cerradas del cuestionario de `#20` no se guardan sólo como texto incidental, sino como datos con estructura interpretable.
- Significa que cada respuesta puede leerse como una señal de experiencia sobre una dimensión ya definida por producto.
- Significa que esas señales pueden compararse entre reseñas porque provienen de preguntas y opciones predefinidas.
- Significa que el producto puede usar esas respuestas para sostener medición básica sobre experiencia reportada y éxito percibido.
- Significa que la salida de la reseña incluye información útil para conteos, agrupaciones o lecturas simples posteriores, aunque este issue no defina la UI que las mostraría.
- No significa inventar un dashboard, una consola de analítica ni una herramienta BI.
- No significa agregar un formulario más largo ni nuevas preguntas para mejorar la medición.
- No significa permitir configuración dinámica por administración ni captura manual por dueño.

## Flujo principal

1. El `mejenguero` inicia y completa la reseña dentro del flujo definido por `#17`.
2. Dentro de ese flujo, el usuario responde el cuestionario breve obligatorio definido por `#20` usando opciones cerradas y válidas.
3. El sistema valida que las respuestas del cuestionario sean completas y pertenezcan al catálogo vigente de `#20`.
4. Al registrarse la reseña, el sistema conserva esas respuestas no sólo como parte del formulario respondido, sino también como señales estructuradas de experiencia asociadas a la reseña.
5. La reseña resultante queda compuesta por `rating`, `comentario`, `respuestas estructuradas del cuestionario` y `fotos` opcionales.
6. El producto dispone entonces de una base medible mínima para interpretar experiencia reportada de manera más estructurada que sólo con texto libre.

## Casos alternos/validaciones

- Si el usuario no completa el cuestionario obligatorio, la reseña no puede enviarse conforme a `#20`; por lo tanto, tampoco puede existir interpretación métrica válida en `#22`.
- Si una respuesta seleccionada no pertenece a las opciones cerradas definidas por `#20`, el sistema debe tratarla como inválida y no debe convertirla en una señal métrica válida.
- Si la reseña falla al guardarse, el sistema no debe asumir que las métricas quedaron capturadas correctamente sólo porque el usuario había respondido el cuestionario.
- Si el cuestionario cambia en el futuro, este spec no asume motor dinámico ni gobierno avanzado de versiones; sólo exige que las respuestas vigentes del MVP puedan tratarse como datos estructurados.
- Si una reseña tiene `1 estrella`, siguen aplicando las reglas de `#19`, pero esa validación no altera la responsabilidad de `#22` sobre el significado métrico derivado del cuestionario.
- Si el usuario agrega `fotos`, eso no cambia la definición ni la obligatoriedad de la captura estructurada de experiencia.

## Datos de entrada

- Contexto de reseña MVP definido por `#17`.
- Cuestionario breve obligatorio definido por `#20`.
- Catálogo vigente de preguntas cerradas y respuestas válidas del cuestionario.
- Respuestas efectivamente seleccionadas por el usuario en cada pregunta obligatoria del cuestionario.
- Contexto de la reseña a la que esas respuestas quedan asociadas.

Notas:

- Este issue define inputs funcionales y semánticos de producto, no un contrato técnico específico de API, eventos o persistencia.
- El origen de las métricas es exclusivamente el cuestionario estructurado ya definido en `#20`.

## Datos de salida

- Respuestas del cuestionario preservadas como datos estructurados asociados a la reseña.
- Señales medibles de experiencia derivadas de opciones cerradas, listas para interpretación básica posterior.
- Base funcional para responder preguntas como `qué experiencia están reportando los usuarios` o `si la aplicación logra experiencias satisfactorias`, sin depender sólo de comentario libre.
- Separación clara entre `rating` general, comentario cualitativo y métricas estructuradas derivadas del cuestionario.

## Dependencias

- `#17` para el flujo padre de creación de reseña y la existencia de la reseña como objeto funcional completo.
- `#20` para la definición del cuestionario breve obligatorio, sus preguntas y sus respuestas cerradas.
- `#18` para la coexistencia con el `rating` general como señal distinta.
- `#19` para la convivencia con la validación especial de reseñas de `1 estrella`, cuando aplique.
- Disponibilidad de un catálogo MVP predefinido y cerrado de preguntas y respuestas del cuestionario.

## Criterios de aceptación

1. Dado el cuestionario obligatorio definido en `#20`, cuando se revisa `#22`, entonces queda explícito que este issue no redefine preguntas ni respuestas, sino que interpreta esas respuestas para darles significado medible.
2. Dada una reseña MVP creada dentro de `#17`, cuando el usuario completa válidamente el cuestionario obligatorio, entonces el sistema puede considerar esas respuestas como señales estructuradas de experiencia asociadas a esa reseña.
3. Dado el alcance de `#22`, cuando se revisa el spec, entonces queda claro que las métricas del MVP provienen sólo de respuestas cerradas, predefinidas y seleccionadas por el usuario.
4. Dado el enfoque MVP, cuando se revisa este issue, entonces queda claro que no forman parte del alcance dashboards, BI, paneles administrativos, configuración de métricas ni analítica avanzada.
5. Dada la separación con `#17` y `#20`, cuando se revisa `#22`, entonces queda claro que `#17` sigue siendo dueño del flujo de reseña y `#20` sigue siendo dueño del cuestionario.
6. Dada una respuesta inválida o fuera del catálogo cerrado, cuando se evalúa su captura, entonces el sistema no debe tratarla como una métrica estructurada válida.
7. Dado el objetivo de medición de experiencia, cuando se revisa la salida esperada de este issue, entonces queda claro que el producto obtiene una base medible simple para interpretar experiencia reportada más allá del comentario libre.
8. Dado el contexto acordado del proyecto, cuando se revisa este spec, entonces queda explícito que `#22` ya no representa FAQ ni contenido cargado por dueño.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec deja explícito que `#22` profundiza el cuestionario de `#20` y forma parte funcional del flujo de reseña de `#17`.
- Queda claro que `#22` no redefine el cuestionario, sino que consume sus respuestas cerradas y predefinidas.
- Queda explícito que las respuestas del cuestionario se consideran señales estructuradas y medibles de experiencia en este MVP.
- Queda claro que el objetivo es habilitar insight básico sobre experiencia reportada y percepción de éxito del producto.
- Queda claro que no hay alcance para dashboards, BI, admin UI, configuración de métricas ni analítica avanzada.
- Queda explícito que la interpretación métrica proviene del usuario que responde la reseña y no de un dueño ni de una carga manual posterior.
- Se mantiene una separación nítida entre `rating`, `comentario`, cuestionario base y métricas derivadas.
- El documento se mantiene en lenguaje de producto/comportamiento MVP y no baja a contratos técnicos innecesarios.

## Notas de partición con #20

- `#20` responde: `qué bloque breve, obligatorio y estructurado de preguntas debe completar el usuario dentro de la reseña MVP`.
- `#22` responde: `cómo esas respuestas cerradas se interpretan como métricas estructuradas de experiencia`.
- Si la decisión pertenece a `qué preguntas hay`, `qué opciones cerradas tiene cada una` o `cuándo se exige completar el cuestionario`, cae en `#20`.
- Si la decisión pertenece a `cómo esas respuestas se leen como señales medibles`, `cómo quedan interpretadas como salida funcional` o `cómo sostienen una lectura básica de experiencia`, cae en `#22`.
- `#20` no debe absorber la capa de significado métrico de las respuestas.
- `#22` no debe redefinir el formulario base, cambiar su obligatoriedad ni convertirlo en un sistema configurable.
- Ni `#20` ni `#22` deben expandirse en este MVP hacia dashboards, BI, admin UIs o analítica sofisticada.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/21
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#21
Current issue: TheMonstersP4/mejengueros-app#22