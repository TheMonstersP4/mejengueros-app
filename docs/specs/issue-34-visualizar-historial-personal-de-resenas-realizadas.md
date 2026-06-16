# Issue #34: Visualizar historial personal de reseñas realizadas

## Título

Consultar el historial personal de reseñas que el `mejenguero` ha realizado.

## Objetivo

Definir la capacidad MVP para que un `mejenguero` autenticado pueda consultar la lista de reseñas que personalmente ha escrito sobre canchas, permitiéndole revisar su actividad de reviewer dentro del producto de manera simple y directa.

## Relación con #17, #18, #19, #20, #21, #22, #35, #38 y #9

- `#17` define la capacidad padre de creación de reseñas de canchas. `#34` consume las reseñas ya publicadas mediante ese flujo.
- `#18` define el comportamiento del rating en estrellas dentro de una reseña; `#34` muestra ese dato como parte de cada entrada en el historial.
- `#19` define la validación especial para reseñas de `1 estrella`; esa regla ya fue resuelta al momento de crear la reseña en `#17/#19`, y `#34` sólo muestra el resultado.
- `#20` define el cuestionario breve obligatorio de experiencia; `#34` puede mostrar las respuestas como parte del detalle de cada reseña si el producto decide exponerlas, pero no redefine esa lógica.
- `#21` resuelve la perspectiva del `dueño` de cancha para ver reseñas recibidas; `#34` resuelve la perspectiva del `mejenguero` para ver reseñas que él mismo escribió. Son capacidades distintas con usuarios y objetivos distintos.
- `#22` define el significado medible de las respuestas estructuradas de experiencia. `#34` puede mostrar respuestas como contenido de una reseña, pero no interpreta ni agrega esas respuestas como métricas.
- `#35` organiza la lista de canchas favoritas del `mejenguero`; `#34` organiza su historial de reseñas realizadas. Pueden convivir como secciones dentro del perfil pero no dependen entre sí.
- `#38` permite al `mejenguero` marcar o desmarcar una cancha como favorita; `#34` permite al mismo usuario ver su historial de reseñas. Pertenecen al mismo bloque de actividad personal del usuario en `#9`, pero no tienen dependencia directa entre sí.
- `#9` es la épica contenedora que agrupa las capacidades de perfil y fidelización del usuario.

## Historia de usuario

Como `mejenguero`,
quiero ver la lista de reseñas que yo mismo he realizado sobre canchas,
para poder revisar mi actividad de reviewer, recordar qué dije sobre cada cancha y tener control sobre mis contribuciones al ecosistema de reseñas.

## Alcance

- Mostrar al `mejenguero` autenticado la lista de reseñas que él mismo ha escrito.
- Incluir en cada entrada del historial la información esencial para identificar la reseña: cancha reseñada, rating general, comentario y referencia temporal básica.
- Contemplar el estado vacío cuando el `mejenguero` todavía no ha realizado ninguna reseña.
- Restringir la consulta al usuario autenticado; cada usuario sólo puede ver su propio historial.
- Mantener el scope en nivel de producto MVP, sin bajar a decisiones técnicas de implementación.

## Fuera de alcance

- Crear nuevas reseñas desde esta vista; eso pertenece a `#17` y se origina desde el detalle de cancha de `#16`.
- Editar o eliminar reseñas ya realizadas.
- Visualizar reseñas recibidas desde la perspectiva del dueño de la cancha; eso pertenece a `#21`.
- Incorporar filtros avanzados, búsqueda por cancha, ordenación personalizada, paginación real o explotación analítica sobre el historial.
- Mostrar métricas propias del usuario como "promedio de mis calificaciones" o "total de reseñas escritas" como KPIs visuales.
- Comprometer contratos técnicos de API, persistencia, indexación ni sincronización offline.
- Moderar, denunciar, destacar ni validar reseñas desde esta vista.

## Reglas de negocio

1. Sólo un `mejenguero` autenticado puede consultar su propio historial de reseñas realizadas.
2. El historial muestra únicamente las reseñas escritas por ese usuario específico, no las reseñas de otros usuarios sobre canchas que él administre.
3. Las reseñas visibles deben provenir del flujo de creación de `#17` y sus sub-issues.
4. Si el `mejenguero` no ha realizado ninguna reseña, el sistema debe mostrar un estado vacío claro en lugar de una lista incompleta o un error.
5. Cada entrada del historial debe incluir información mínima para identificar la cancha reseñada, el rating asignado y el comentario escrito.
6. `#34` no debe requerir ni mostrar el proceso de creación de reseña; sólo consume resultados ya publicados.
7. `#34` no define qué es una reseña ni cómo se compone; eso pertenece a `#17`, `#18`, `#19`, `#20` y `#22`.
8. El acceso a esta vista debe ser posible desde el contexto de perfil del usuario definido en `#9`, sin necesidad de entrar al detalle de una cancha específica.

## Qué significa historial personal de reseñas en este MVP

- Significa que el `mejenguero` puede consultar en un solo lugar todas las reseñas que ha escrito hasta el momento.
- Significa que cada entrada del historial corresponde a una reseña concreta sobre una cancha específica.
- Significa que la información disponible por reseña incluye al menos la cancha, el rating, el comentario y una referencia de cuándo fue escrita.
- No significa editar ni borrar reseñas pasadas.
- No significa analítica avanzada sobre el patrón de reseñas del usuario.
- No significa que el historial muestre reseñas de otros usuarios sobre canchas del propio usuario.

## Flujo principal

1. El `mejenguero` accede a su perfil o a la sección de actividad personal dentro del contexto de `#9`.
2. El usuario identifica y accede a la sección de historial de reseñas realizadas.
3. El sistema recupera las reseñas que el `mejenguero` autenticado ha escrito.
4. El sistema muestra la lista de reseñas con la información esencial de cada una.
5. El `mejenguero` recorre la lista y consulta su actividad de reviewer.

## Casos alternos/validaciones

- Si el `mejenguero` no ha escrito ninguna reseña, el sistema debe mostrar un estado vacío claro indicando que todavía no tiene reseñas en su historial.
- Si el usuario no está autenticado, el sistema no debe mostrar historial de reseñas de ningún usuario.
- Si una reseña existe pero alguno de sus componentes opcionales no está presente, la entrada del historial igualmente debe poder mostrarse con la información disponible.
- Si ocurre una falla al cargar el historial, el sistema debe comunicar que no fue posible obtener la información en lugar de simular una lista vacía o exitosa.
- Si el producto decide no exponer las respuestas del cuestionario breve de `#20` en esta vista, el historial debe seguir siendo válido mostrando al menos rating, cancha y comentario disponible.

## Datos de entrada

- Identificador o contexto del `mejenguero` autenticado.
- Conjunto de reseñas publicadas y asociadas a ese `mejenguero` como autor.
- Componentes funcionales de cada reseña disponible: cancha reseñada, `rating`, `comentario`, referencia temporal, y opcionalmente respuestas del cuestionario breve si se decide exponerlas.

Notas:

- Este issue define inputs funcionales de producto, no el contrato técnico exacto de consulta.
- La existencia y composición de las reseñas provienen del flujo de `#17`, `#18`, `#19` y `#20`.

## Datos de salida

- Lista de reseñas realizadas por el `mejenguero` autenticado.
- Información esencial de cada reseña para identificar la cancha, la valoración y el contexto del retroalimentación escrito.
- Estado vacío cuando el `mejenguero` no ha realizado ninguna reseña.
- Error comunicado claramente si el sistema no puede recuperar el historial.

## Dependencias

- `#17` para la existencia del flujo padre que crea reseñas de cancha y produce los datos que `#34` consulta.
- `#18` para la existencia del `rating` general en estrellas dentro de cada reseña del historial.
- `#19` para la posible justificación asociada a reseñas de `1 estrella`, ya resuelta al momento de creación.
- `#20` para la existencia del cuestionario breve obligatorio como parte de la reseña MVP, cuyas respuestas pueden o no exponerse en `#34` según decisión de producto.
- `#22` para el significado estructurado de esas respuestas cuando se usen como métricas; `#34` no produce KPIs ni analítica.
- Autenticación vigente del usuario para poder asociar el historial al `mejenguero` correcto.
- Persistencia o disponibilidad de las reseñas ya publicadas y su relación con el autor.

Notas:

- `#35`, `#36`, `#37` y `#38` pertenecen al mismo bloque de perfil en `#9` pero no son prerequisitos para que `#34` exista.
- `#21` y `#34` resuelven perspectivas distintas del mismo ecosistema de reseñas y no dependen entre sí.

## Criterios de aceptación

1. Dado un `mejenguero` autenticado que ha escrito al menos una reseña, cuando accede a su historial personal de reseñas, entonces el sistema muestra una lista con las reseñas que ese usuario específicamente ha realizado.
2. Dado el alcance de `#34`, cuando se revisa el spec, entonces queda explícito que esta capacidad es orientada al `mejenguero` como autor de reseñas y es distinta de `#21`, que está orientada al `dueño` como receptor de reseñas.
3. Dado un `mejenguero` que visualiza su historial, cuando lee la lista, entonces puede identificar al menos la cancha reseñada, el rating asignado y el comentario o contexto disponible de esa reseña.
4. Dado que el historial contiene reseñas con componentes opcionales como `fotos`, cuando esos componentes no están presentes en alguna entrada, entonces la reseña igual se muestra con los datos disponibles sin romper la experiencia.
5. Dado un `mejenguero` que todavía no ha escrito ninguna reseña, cuando accede a su historial, entonces el sistema muestra un estado vacío claro indicando que no tiene reseñas realizadas.
6. Dado un usuario no autenticado, cuando intenta acceder al historial de reseñas, entonces el sistema no expone información de reseñas de ningún usuario.
7. Dado el enfoque MVP, cuando se revisa este issue, entonces queda claro que no incluye editar reseñas, eliminarlas, filtrarlas por cancha, ordenarlas, ni generar analítica sobre el patrón de reviews del usuario.
8. Dada la relación con `#17`, `#18`, `#19` y `#20`, cuando se revisa `#34`, entonces queda claro que esos issues definen cómo se crea y compone la reseña, mientras `#34` sólo define cómo el `mejenguero` las consulta una vez existentes.
9. Dada una falla de carga del historial, cuando el sistema no puede recuperar la información, entonces comunica el error en lugar de mostrar una lista vacía engañosa.
10. Dado el vínculo con `#9`, cuando se revisa el spec, entonces queda claro que el historial de reseñas es una sección del perfil del usuario y no un flujo de creación de reseñas.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad que `#34` es un issue orientado al `mejenguero` como autor y separado de `#21` que es orientado al `dueño`.
- Queda explícito que `#34` depende de reseñas ya existentes (producidas por `#17`), pero no forma parte del flujo de creación.
- Queda claro qué información mínima debe mostrar cada entrada del historial en este MVP.
- Queda contemplado el estado vacío para el caso en que el usuario no haya escrito ninguna reseña.
- Queda contemplada la restricción funcional para que cada usuario sólo vea sus propias reseñas realizadas.
- Queda explícita la separación respecto de edición, eliminación, moderación, filtros avanzados y analítica.
- El documento mantiene enfoque de producto y MVP, sin bajar a decisiones técnicas innecesarias.

## Notas de partición

- `#17` responde: `cómo un mejenguero crea una reseña completa sobre una cancha`.
- `#18` responde: `cómo se define y selecciona el rating general en estrellas`.
- `#19` responde: `qué validación extra aplica cuando la reseña tiene 1 estrella`.
- `#20` responde: `qué cuestionario breve obligatorio acompaña la reseña`.
- `#22` responde: `qué significado medible tienen las respuestas estructuradas de experiencia`.
- `#21` responde: `cómo el dueño ve las reseñas recibidas por su cancha`.
- `#34` responde: `cómo el mejenguero consulta la lista de reseñas que él mismo ha escrito`.
- `#35` responde: `cómo el mejenguero consulta o gestiona la lista de canchas favoritas guardadas`.
- `#38` responde: `cómo el mejenguero guarda o quita una cancha como favorita desde su visualización`.
- Si la decisión pertenece a crear, validar, completar o publicar una reseña, cae en `#17/#18/#19/#20`.
- Si la decisión pertenece a consultar reseñas desde la perspectiva del dueño que las recibió, cae en `#21`.
- Si la decisión pertenece a consultar las reseñas escritas por el propio usuario, cae en `#34`.
- `#34` no debe absorber validaciones internas del formulario de reseña ni redefinir su composición; sólo consume el resultado ya publicado.
- Si `#34` muestra respuestas del cuestionario, las muestra como contenido de la reseña; la interpretación métrica o agregada pertenece a `#22`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/33
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#33
Current issue: TheMonstersP4/mejengueros-app#34