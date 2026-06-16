# Issue #35: Visualizar lista de canchas favoritas

## Título

Visualizar la lista de canchas favoritas del `mejenguero`.


## Nota de priorización MVP semana 10

Favoritos queda post-MVP salvo que sobre capacidad; no es parte del flujo crítico búsqueda -> reserva -> reseña.

## Objetivo

Definir la capacidad MVP para que un `mejenguero` autenticado pueda visualizar la lista de canchas que ha marcado como favoritas y, desde esa misma lista, navegar al detalle de una cancha. Si la vista ofrece la opción de quitar una cancha de favoritos, esa opción debe reutilizar la acción de desmarcar definida en `#38`, sin redefinir una mutación distinta.

## Relación con #38, #15, #16, #29, #34, #36, #37 y #9

- `#38` es el dueño de la acción de marcar o desmarcar una cancha como favorita. `#35` consume las canchas favoritas resultantes para visualizarlas como lista. Si `#35` permite quitar una cancha desde la lista, debe hacerlo como punto de acceso alternativo a la misma acción definida en `#38`, no como una mutación independiente.
- `#15` define la regla de visibilidad de canchas dentro del catálogo: sólo canchas con estado de publicación `publicada` y estado administrativo `activo`. `#35` hereda ese criterio para la experiencia de la lista.
- `#16` define el detalle de una cancha individual. Desde `#35`, el `mejenguero` debe poder navegar al detalle de cualquier cancha de su lista favorita; ese destino está resuelto por `#16`.
- `#29` debe proveer la persistencia de la relación usuario/cancha favorita y la restricción que evita favoritos duplicados.
- `#34` organiza el historial personal de reseñas realizadas por el `mejenguero`. `#35` organiza su lista de canchas favoritas. Pertenecen al mismo bloque de perfil en `#9` pero no tienen dependencia directa entre sí.
- `#36` y `#37` resuelven edición de perfil y foto de perfil respectivamente. Pertenecen a `#9` pero son independientes de `#35`.
- `#9` es la épica contenedora que agrupa las capacidades de perfil y fidelización del usuario.

## Historia de usuario

Como `mejenguero`,
quiero ver la lista de canchas que he marcado como favoritas,
para encontrarlas fácilmente y navegar a su detalle cuando quiera revisarlas de nuevo.

## Alcance

- Mostrar al `mejenguero` autenticado la lista de canchas que ha marcado como favoritas mediante el flujo de `#38`.
- Incluir en cada entrada de la lista información suficiente para identificar la cancha: nombre, imagen o placeholder, y ubicación básica.
- Permitir que el `mejenguero` navegue al detalle de una cancha favorita desde la lista, resolviendo esa experiencia en `#16`.
- Opcionalmente, permitir que el `mejenguero` quite una cancha de su lista desde esta vista reutilizando la acción de desmarcar definida en `#38`, sin duplicar la lógica de dominio.
- Contemplar el estado vacío cuando el `mejenguero` todavía no tiene ninguna cancha marcada como favorita.
- Restringir la consulta al usuario autenticado; cada usuario sólo puede ver sus propios favoritos.
- Mantener el scope en nivel de producto MVP, sin bajar a decisiones técnicas de implementación.

## Fuera de alcance

- Marcar nuevas canchas como favoritas desde esta vista; esa acción pertenece a `#38` y se origina desde la visualización de una cancha individual.
- Crear carpetas, colecciones, etiquetas o categorías dentro de los favoritos.
- Ordenar la lista por relevancia, fecha, distancia, frecuencia de visita o cualquier otro criterio personalizado.
- Compartir la lista de favoritos con otros usuarios.
- Incorporar filtros avanzados, búsqueda dentro de favoritos o paginación real.
- Recomendar canchas basadas en los favoritos del usuario.
- Comprometer contratos técnicos de API, persistencia, indexación ni sincronización offline.
- Resolver la experiencia completa de detalle de cancha; eso pertenece a `#16`.
- Definir el catálogo ni la regla de visibilidad de canchas; eso pertenece a `#15`.

## Reglas de negocio

1. Sólo un `mejenguero` autenticado puede consultar su propia lista de canchas favoritas.
2. La lista muestra únicamente las canchas que ese usuario específico ha marcado como favoritas a través del flujo definido en `#38`.
3. Cada `mejenguero` sólo puede ver sus propios favoritos; no puede ver ni modificar los favoritos de otro usuario.
4. Si el `mejenguero` no tiene ninguna cancha marcada como favorita, el sistema debe mostrar un estado vacío claro en lugar de una lista incompleta o un error.
5. Desde la lista, el `mejenguero` debe poder navegar al detalle de cualquier cancha favorita, accediendo a la experiencia resuelta por `#16`.
6. Si la lista ofrece quitar una cancha de favoritos, esa opción debe invocar o reutilizar la acción de desmarcar definida en `#38`.
7. Al quitar una cancha desde `#35`, la relación usuario/cancha favorita debe quedar eliminada con la misma regla de negocio de `#38`; `#35` no define una mutación alternativa.
8. `#35` no redefine ni duplica la acción de marcar/desmarcar favorito que pertenece a `#38`; sólo puede ofrecer un acceso visual desde la lista a esa misma capacidad.
9. Si una cancha favorita ya no tiene estado de publicación `publicada` o estado administrativo `activo`, `#35` debe manejarlo sin mostrar una experiencia inconsistente.
10. `#35` no define qué es un favorito ni cómo se crea la relación usuario/cancha; eso pertenece a `#38`.

## Qué significa visualizar la lista de favoritos en este MVP

- Significa que el `mejenguero` puede consultar en un solo lugar todas las canchas que ha guardado como favoritas.
- Significa que desde esa lista puede navegar directamente al detalle de una cancha sin tener que buscarla en el catálogo de nuevo.
- Puede significar que el usuario tenga un acceso para quitar una cancha desde la lista, siempre que ese acceso reutilice la acción de desmarcar de `#38`.
- No significa crear colecciones, carpetas ni subcategorías de favoritos.
- No significa ordenar, filtrar ni buscar dentro de sus favoritos.
- No significa compartir la lista ni comparar favoritos con otros usuarios.
- No significa marcar nuevas canchas como favoritas desde esta vista; esa acción vive en `#38`.

## Flujo principal

1. El `mejenguero` accede a su perfil o a la sección de actividad personal dentro del contexto de `#9`.
2. El usuario identifica y accede a la sección de canchas favoritas.
3. El sistema recupera las canchas marcadas como favoritas por ese `mejenguero` autenticado.
4. El sistema muestra la lista de canchas favoritas con la información esencial de cada una.
5. El `mejenguero` puede seleccionar una cancha para navegar a su detalle en `#16`.
6. Opcionalmente, el `mejenguero` puede quitar una cancha de su lista desde esta vista usando la misma acción de desmarcar definida en `#38`.
7. Si quita una cancha, el sistema actualiza la lista reflejando el cambio de estado.

## Casos alternos/validaciones

- Si el `mejenguero` no tiene ninguna cancha marcada como favorita, el sistema debe mostrar un estado vacío claro indicando que todavía no tiene favoritas.
- Si el usuario no está autenticado, el sistema no debe mostrar la lista de favoritos de ningún usuario.
- Si una cancha favorita ya no tiene estado de publicación `publicada` o estado administrativo `activo`, el sistema debe manejar esa situación sin mostrar información inconsistente ni romper la experiencia de la lista.
- Si ocurre una falla al cargar la lista, el sistema debe comunicar que no fue posible obtener la información en lugar de simular una lista vacía o exitosa.
- Si la acción de quitar una cancha de favoritos falla, el sistema debe comunicarlo sin dejar la lista en un estado visual engañoso.
- Si el usuario quita una cancha de favoritos y la lista queda vacía, el sistema debe mostrar el estado vacío correspondiente.
- Si una cancha favorita no tiene imagen registrada, la entrada de la lista debe mostrarse de forma válida usando un placeholder, de forma consistente con la regla de `#15`.

## Datos de entrada

- Identificador o contexto del `mejenguero` autenticado.
- Conjunto de canchas marcadas como favoritas y asociadas a ese `mejenguero`.
- Información básica de cada cancha favorita para su representación en la lista: nombre, imagen o placeholder, ubicación básica.
- Acción del usuario: navegar a detalle o, si la vista lo permite, quitar de favoritos mediante la acción definida en `#38`.

Notas:

- Este issue define inputs funcionales de producto, no el contrato técnico exacto de consulta.
- La existencia de la relación usuario/cancha favorita proviene del flujo de `#38`.

## Datos de salida

- Lista de canchas favoritas del `mejenguero` autenticado.
- Información esencial de cada cancha para identificarla dentro de la lista.
- Navegación hacia el detalle de una cancha favorita, resuelta por `#16`.
- Cancha removida de favoritos cuando el usuario así lo decide desde la lista mediante la acción definida en `#38`.
- Estado vacío cuando el `mejenguero` no tiene canchas favoritas o las ha quitado todas.
- Error comunicado claramente si el sistema no puede completar una acción.

## Dependencias

- `#38` para la existencia de la relación usuario/cancha favorita que `#35` consume y para la acción de marcar/desmarcar que la lista puede invocar.
- `#29` para la persistencia de favoritos y la restricción de unicidad usuario/cancha.
- `#16` como destino de navegación desde la lista hacia el detalle de una cancha favorita.
- `#15` para la regla de visibilidad de canchas (`publicada` + `activo`) que aplica sobre las canchas listadas.
- Autenticación vigente del usuario para asociar la lista al `mejenguero` correcto y proteger el acceso.
- Persistencia o disponibilidad de las canchas favoritas ya guardadas y su relación con el usuario.

Notas:

- `#34`, `#36` y `#37` pertenecen al mismo bloque de perfil en `#9` pero no son prerequisitos para que `#35` exista.
- `#35` no depende de las issues de creación de reseñas (`#17`–`#22`); son bloques independientes del perfil del usuario.

## Criterios de aceptación

1. Dado un `mejenguero` autenticado que tiene al menos una cancha marcada como favorita, cuando accede a su lista de favoritas, entonces el sistema muestra las canchas que ese usuario específicamente ha guardado.
2. Dado el alcance de `#35`, cuando se revisa el spec, entonces queda explícito que esta capacidad es de visualización de favoritos ya existentes, y que la acción de marcar nuevas canchas como favoritas pertenece a `#38`.
3. Dado un `mejenguero` que visualiza su lista de favoritas, cuando revisa cada entrada, entonces puede identificar al menos el nombre y la ubicación básica de la cancha, con imagen o placeholder cuando corresponda.
4. Dado un `mejenguero` que quiere ver más información sobre una cancha favorita, cuando selecciona esa cancha desde la lista, entonces el sistema navega al detalle de esa cancha resuelto por `#16`.
5. Dado un `mejenguero` que quiere quitar una cancha de su lista, cuando ejecuta esa acción desde `#35`, entonces la cancha deja de aparecer en su lista de favoritas y el sistema refleja el cambio.
6. Dado que el `mejenguero` quita todas sus canchas favoritas, cuando la lista queda vacía, entonces el sistema muestra el estado vacío correspondiente.
7. Dado un `mejenguero` que todavía no ha marcado ninguna cancha como favorita, cuando accede a su lista, entonces el sistema muestra un estado vacío claro indicando que no tiene canchas favoritas.
8. Dado un usuario no autenticado, cuando intenta acceder a la lista de favoritas, entonces el sistema no expone información de favoritas de ningún usuario.
9. Dada una falla al cargar la lista o al quitar una cancha, cuando el sistema no puede completar la operación, entonces comunica el error en lugar de mostrar un estado engañoso.
10. Dado el enfoque MVP, cuando se revisa este issue, entonces queda claro que no incluye ordenar, filtrar, buscar dentro de favoritos, crear colecciones ni compartir la lista.
11. Dado el vínculo con `#38`, cuando se revisa el spec, entonces queda claro que `#38` resuelve la acción de marcar/desmarcar y `#35` sólo puede invocarla desde la lista como punto de acceso alternativo, sin redefinirla.
12. Dado el vínculo con `#9`, cuando se revisa el spec, entonces queda claro que la lista de favoritas es una sección del perfil del usuario y no una extensión del catálogo de canchas.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad que `#35` es una capacidad de visualización de favoritos ya existentes, dependiente de `#38` para la creación y remoción de esa relación.
- Queda explícito que el `mejenguero` puede navegar al detalle de una cancha favorita desde la lista.
- Queda explícito que el `mejenguero` puede quitar una cancha de favoritos desde la lista, sin necesidad de ir a la vista individual.
- Queda claro qué información mínima debe mostrar cada entrada de la lista en este MVP.
- Queda contemplado el estado vacío para el caso en que el usuario no tenga canchas favoritas.
- Queda contemplada la restricción funcional para que cada usuario sólo vea sus propios favoritos.
- Queda explícita la separación respecto de ordenamiento avanzado, colecciones, filtros y alcance social.
- El documento mantiene enfoque de producto y MVP, sin bajar a decisiones técnicas innecesarias.

## Notas de partición

- `#15` responde: `qué canchas puede descubrir y ver el mejenguero en el catálogo`.
- `#16` responde: `qué información ve el mejenguero cuando abre una cancha específica`.
- `#34` responde: `cómo el usuario consulta su historial personal de reseñas realizadas`.
- `#38` responde: `cómo el mejenguero marca o quita una cancha como favorita desde su visualización individual`.
- `#35` responde: `cómo el mejenguero visualiza la lista completa de canchas que ha guardado como favoritas`.
- Si la decisión pertenece a descubrir o filtrar canchas en el catálogo, cae en `#15`.
- Si la decisión pertenece al contenido de detalle de una cancha individual, cae en `#16`.
- Si la decisión pertenece a la acción puntual de marcar o desmarcar una cancha como favorita desde su vista, cae en `#38`.
- Si la decisión pertenece a visualizar la lista completa de favoritos del usuario, cae en `#35`.
- `#35` no debe redefinir la acción de marcar/desmarcar favorito ni absorber alcance del catálogo; sólo visualiza la lista resultante y, si ofrece quitar, invoca la acción de `#38`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/34
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#34
Current issue: TheMonstersP4/mejengueros-app#35