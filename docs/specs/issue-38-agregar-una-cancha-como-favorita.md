# Issue #38: Agregar una cancha como favorita

## Título

Marcar o desmarcar una cancha como favorita.


## Nota de priorización MVP semana 10

Agregar cancha favorita queda post-MVP salvo que sobre capacidad; no es parte del flujo crítico de reserva.

## Objetivo

Definir la capacidad MVP para que un `mejenguero` autenticado pueda marcar o desmarcar una cancha con estado de publicación `publicada` y estado administrativo `activo` como favorita desde la visualización de la cancha, usando un indicador visual de corazón que refleje claramente si esa cancha ya forma parte de sus favoritos.

## Relación con #15, #16, #29, #34, #35 y #9

- `#15` permite descubrir canchas publicadas y activas dentro del catálogo.
- `#16` permite abrir el detalle de una cancha seleccionada desde el catálogo.
- `#38` agrega la capacidad de marcar o desmarcar como favorita una cancha visible para el `mejenguero`.
- `#38` no redefine búsqueda, filtros ni listado del catálogo; eso pertenece a `#15`.
- `#38` no redefine el contenido informativo del detalle; eso pertenece a `#16`.
- `#29` debe proveer la persistencia de la relación usuario/cancha favorita y la restricción que evita duplicados.
- `#35` puede consumir las canchas favoritas resultantes para mostrarlas en una lista o sección de favoritos. Si `#35` ofrece quitar una cancha desde la lista, debe reutilizar esta misma acción de desmarcar y no definir una mutación paralela.
- `#34` pertenece al mismo bloque de actividad personal del usuario, pero se enfoca en reseñas realizadas, no en favoritos.
- `#38` y `#34` pueden convivir como accesos o secciones del perfil del usuario, pero no dependen directamente entre sí.
- `#9` agrupa capacidades relacionadas con perfil del usuario y fidelización.

## Historia de usuario

Como `mejenguero`,
quiero marcar una cancha como favorita desde su visualización,
para poder guardarla y encontrarla fácilmente más adelante.

## Alcance

- Mostrar un ícono de corazón asociado a una cancha visible para el `mejenguero`.
- Representar visualmente si la cancha ya está marcada como favorita por el usuario autenticado.
- Permitir que el usuario marque una cancha como favorita.
- Permitir que el usuario quite una cancha de favoritos desde el mismo control.
- Mantener sincronizado el estado visual del corazón con el estado funcional de favorito.
- Aplicar esta acción únicamente sobre canchas con estado de publicación `publicada` y estado administrativo `activo`.
- Evitar duplicados de favoritos para una misma combinación de usuario y cancha.
- Comunicar un error claro si la acción de marcar o desmarcar no puede completarse.

## Fuera de alcance

- Definir el catálogo completo de canchas; eso pertenece a `#15`.
- Definir el detalle completo de cancha; eso pertenece a `#16`.
- Definir la lista completa de canchas favoritas; eso pertenece a `#35`.
- Crear carpetas, colecciones, etiquetas o categorías de favoritos.
- Compartir favoritos con otros usuarios.
- Ordenar favoritos por relevancia, fecha o frecuencia.
- Recomendar canchas con base en favoritos.
- Permitir marcar como favorita una cancha en `borrador`, inactiva, desactivada, oculta, eliminada o no visible para el usuario.
- Definir contratos técnicos de API, persistencia o sincronización offline.

## Reglas de negocio

1. Sólo un usuario autenticado puede marcar o desmarcar una cancha como favorita.
2. El favorito pertenece al usuario que ejecuta la acción.
3. Una misma cancha no debe duplicarse como favorita para el mismo usuario.
4. Si la cancha ya es favorita del usuario, el corazón debe mostrarse lleno en color rojo.
5. Si la cancha no es favorita del usuario, el corazón debe mostrarse sin relleno rojo.
6. Al tocar el corazón de una cancha no favorita, el sistema debe marcarla como favorita para ese usuario.
7. Al tocar el corazón de una cancha ya favorita, el sistema debe quitarla de favoritos para ese usuario.
8. La acción sólo debe aplicar sobre canchas con estado de publicación `publicada` y estado administrativo `activo`, porque son las únicas canchas visibles para el `mejenguero`.
9. Si una cancha deja de tener estado de publicación `publicada` o estado administrativo `activo`, no debe aparecer como opción visible ni accionable para marcarla como favorita.
10. Si la operación falla, el sistema debe comunicarlo y no dejar un estado visual engañoso.
11. `#38` no decide cómo se listan todos los favoritos; sólo resuelve la acción de marcar o desmarcar una cancha.

## Qué significa favorita en este MVP

- Significa que un `mejenguero` autenticado guardó una cancha visible para poder encontrarla más fácilmente después.
- Significa que existe una relación funcional entre usuario y cancha.
- Significa que el estado puede representarse con un corazón lleno rojo cuando la cancha está guardada.
- Significa que el estado puede representarse con un corazón sin relleno rojo cuando la cancha no está guardada.
- No significa crear colecciones avanzadas, listas compartidas, recomendaciones ni comportamiento social.

## Flujo principal

1. El `mejenguero` descubre una cancha desde el catálogo definido en `#15`.
2. El usuario abre la visualización o detalle de la cancha definido en `#16`.
3. El sistema valida que la cancha tenga estado de publicación `publicada` y estado administrativo `activo` para poder mostrarse.
4. El sistema muestra el ícono de corazón asociado a la cancha.
5. Si la cancha ya está en favoritos del usuario, el corazón aparece rojo y lleno.
6. Si la cancha no está en favoritos del usuario, el corazón aparece sin relleno rojo.
7. El usuario toca el corazón.
8. El sistema actualiza el estado de favorito de esa cancha para ese usuario.
9. El sistema actualiza la visualización del corazón según el nuevo estado.

## Casos alternos/validaciones

- Si el usuario no está autenticado, el sistema no debe permitir marcar o desmarcar una cancha como favorita.
- Si la cancha no existe, el sistema debe rechazar la acción.
- Si la cancha no tiene estado de publicación `publicada`, el sistema no debe permitir marcarla como favorita.
- Si la cancha no está activa, el sistema no debe permitir marcarla como favorita.
- Si el usuario intenta marcar como favorita una cancha que ya era favorita, el sistema no debe crear un duplicado.
- Si el usuario intenta quitar de favoritos una cancha que ya no estaba marcada como favorita, el sistema debe manejarlo sin romper la experiencia.
- Si falla el guardado del favorito, el corazón no debe quedar como lleno de forma definitiva.
- Si falla la eliminación del favorito, el corazón no debe quedar como vacío de forma definitiva.
- Si el estado inicial de favorito no puede determinarse, el sistema debe evitar mostrar una señal engañosa.

## Datos de entrada

- Identificador o contexto del usuario autenticado.
- Identificador de la cancha visible.
- Estado actual de la cancha: debe tener estado de publicación `publicada` y estado administrativo `activo`.
- Estado actual de favorito para la combinación usuario/cancha, si existe.

Notas:

- Este issue define entradas funcionales, no contrato técnico exacto de API o persistencia.
- La visibilidad de la cancha debe respetar la regla definida por `#15` y consumida por `#16`.

## Datos de salida

- Cancha marcada como favorita para el usuario autenticado.
- Cancha removida de favoritos para el usuario autenticado, cuando corresponda.
- Estado visual actualizado del corazón.
- Prevención de favoritos duplicados.
- Error claro cuando la acción no puede completarse.

## Dependencias

- `#15` para la regla de visibilidad de canchas dentro del catálogo: sólo canchas con estado de publicación `publicada` y estado administrativo `activo`.
- `#16` para la visualización de una cancha individual seleccionada desde el catálogo.
- Autenticación vigente del usuario.
- `#29` para la persistencia de la relación usuario/cancha favorita y la restricción de unicidad usuario/cancha.
- Persistencia o disponibilidad funcional de la relación usuario/cancha favorita.
- `#35` como consumidor posterior de las canchas favoritas guardadas.

## Criterios de aceptación

1. Dado un `mejenguero` autenticado que visualiza una cancha con estado de publicación `publicada` y estado administrativo `activo` que no es favorita, cuando entra a la vista, entonces el corazón aparece sin relleno rojo.
2. Dado un `mejenguero` autenticado que visualiza una cancha con estado de publicación `publicada` y estado administrativo `activo` que ya es favorita, cuando entra a la vista, entonces el corazón aparece rojo y lleno.
3. Dada una cancha visible que no es favorita, cuando el usuario toca el corazón, entonces la cancha queda marcada como favorita para ese usuario.
4. Dada una cancha visible que ya es favorita, cuando el usuario toca el corazón, entonces la cancha deja de estar marcada como favorita para ese usuario.
5. Dado un usuario y una cancha, cuando el usuario marca la cancha como favorita más de una vez, entonces no se crean favoritos duplicados.
6. Dada una cancha que no tiene estado de publicación `publicada`, cuando se intenta marcarla como favorita, entonces el sistema rechaza la acción o evita que sea accionable para el `mejenguero`.
7. Dada una cancha que no está activa, cuando se intenta marcarla como favorita, entonces el sistema rechaza la acción o evita que sea accionable para el `mejenguero`.
8. Dada una falla al guardar o quitar favorito, cuando el sistema no puede completar la operación, entonces comunica el error y no muestra un estado final engañoso.
9. Dado el alcance de `#38`, cuando se revisa la spec, entonces queda claro que la lista de favoritos pertenece a `#35`.
10. Dado el vínculo con `#15`, cuando se revisa la spec, entonces queda claro que favoritos no redefine catálogo, búsqueda ni filtros.
11. Dado el vínculo con `#16`, cuando se revisa la spec, entonces queda claro que la acción se presenta sobre una cancha que el usuario ya está visualizando.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad la acción de marcar y desmarcar una cancha como favorita.
- Queda explícito que sólo usuarios autenticados pueden ejecutar la acción.
- Queda explícito que sólo aplica sobre canchas con estado de publicación `publicada` y estado administrativo `activo`.
- Queda definido el comportamiento visual del corazón lleno rojo y sin relleno rojo.
- Queda explícito que no deben existir favoritos duplicados para el mismo usuario y cancha.
- Queda clara la relación indirecta con `#15` mediante la regla de visibilidad de canchas.
- Queda clara la relación directa con `#16` como contexto de visualización de la cancha.
- Queda claro que `#35` consume favoritos y puede invocar la remoción desde la lista, pero no redefine esta acción.
- No se agregan capacidades avanzadas de colecciones, recomendaciones, socialización ni ranking.

## Notas de partición

- `#15` responde: `qué canchas puede descubrir y ver el mejenguero en el catálogo`.
- `#16` responde: `qué información ve el mejenguero cuando abre una cancha específica`.
- `#34` responde: `cómo el usuario consulta su historial personal de reseñas realizadas`.
- `#38` responde: `cómo el mejenguero guarda o quita esa cancha como favorita desde su visualización`.
- `#35` responde: `cómo el mejenguero visualiza la lista de canchas favoritas guardadas`.
- Si la decisión pertenece a búsqueda, filtros o listado del catálogo, cae en `#15`.
- Si la decisión pertenece al contenido de detalle de una cancha, cae en `#16`.
- Si la decisión pertenece al historial personal de reseñas realizadas, cae en `#34`.
- Si la decisión pertenece al estado de favorito de una cancha visible para un usuario, cae en `#38`.
- Si la decisión pertenece a visualizar la lista completa de favoritos del usuario, cae en `#35`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/37
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#37
Current issue: TheMonstersP4/mejengueros-app#38