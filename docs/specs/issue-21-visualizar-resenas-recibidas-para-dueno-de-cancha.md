# Issue #21: Visualizar reseñas recibidas para dueño de cancha

## Título

Permitir que el `dueño` vea reseñas básicas recibidas por sus canchas.

## Nota de re-alcance MVP

Para semana 10, `#21` se limita a lectura simple de reseñas ya publicadas desde reservas finalizadas. No incluye fotos, cuestionarios, métricas estructuradas, dashboards, moderación ni respuestas del dueño.

## Relación con issues coordinados

- `#17` crea reseñas básicas desde reservas finalizadas.
- `#18` define el rating visible de `1` a `5` estrellas.
- `#51` facilita que el usuario llegue a la reseña mediante notificación post-reserva.
- `#19`, `#20`, `#22` y `#28` quedan post-MVP para esta vista salvo decisión explícita posterior.

## Objetivo

Definir la capacidad MVP para que un `dueño` pueda consultar reseñas básicas recibidas por sus canchas, leyendo rating, comentario y contexto mínimo sin introducir herramientas de gestión o analítica avanzada.

## Historia de usuario

Como `dueño` de una cancha,
quiero ver las reseñas que recibió mi cancha,
para entender de forma simple qué experiencia reportan los usuarios.

## Alcance

- Consultar lista de reseñas ya recibidas para una cancha bajo propiedad o gestión del `dueño`.
- Mostrar rating general de cada reseña.
- Mostrar comentario básico si existe.
- Mostrar fecha o referencia temporal básica.
- Mostrar contexto mínimo de cancha/reserva si ayuda a identificar la experiencia.
- Mostrar estado vacío cuando no existan reseñas.

## Fuera de alcance

- Crear reseñas; eso pertenece a `#17`.
- Definir el selector de estrellas; eso pertenece a `#18`.
- Mostrar o gestionar fotos/evidencia fotográfica.
- Mostrar respuestas de cuestionario o métricas estructuradas.
- Responder, moderar, ocultar, deshabilitar, denunciar o editar reseñas.
- Dashboards, tendencias, comparativas, analytics avanzados o scentralcards.
- Panel administrativo global o revisión manual de contenido.

## Reglas de negocio

1. `#21` representa una capacidad de consulta desde la perspectiva del `dueño`, no una capacidad de creación.
2. El `dueño` sólo debe ver reseñas asociadas a una cancha que le pertenece o administra.
3. Las reseñas visibles deben provenir del flujo `#17`.
4. La experiencia MVP debe priorizar lectura simple de retroalimentación, no gestión posterior.
5. Cada reseña listada debe mostrar al menos rating y contexto básico disponible.
6. Si una cancha todavía no tiene reseñas recibidas, el sistema debe comunicar un estado vacío claro.
7. El `dueño` no puede ocultar, apagar ni deshabilitar reseñas de mejengueros.
8. La confianza de la reseña se protege antes de publicar mediante reserva finalizada, no mediante control posterior del dueño.

## Qué debe mostrar la lista de reseñas recibidas en este MVP

- Cancha a la que pertenece la reseña.
- Rating general de `1` a `5` estrellas.
- Comentario básico si existe.
- Fecha o referencia temporal básica de publicación.
- Identidad mínima o nombre visible del autor sólo si el producto ya lo tiene disponible y no agrega campos de perfil post-MVP.

## Flujo principal

1. El `dueño` accede a la experiencia de su complejo/cancha o a una sección de retroalimentación recibido.
2. El sistema identifica las canchas que pertenecen o son gestionadas por el `dueño`.
3. El sistema busca las reseñas ya publicadas y asociadas a esas canchas.
4. El sistema muestra la lista con rating, comentario y contexto básico.
5. Si no hay reseñas, muestra estado vacío claro.

## Casos alternos/validaciones

- Si la cancha del `dueño` no tiene reseñas recibidas, el sistema muestra un estado vacío claro.
- Si el `dueño` intenta consultar una cancha que no le pertenece o no administra, el sistema no debe exponer reseñas de esa cancha.
- Si una reseña no tiene comentario, puede mostrarse con rating y contexto básico.
- Si ocurre una falla al cargar reseñas, el sistema debe comunicar que no fue posible obtener la información.

## Datos de entrada

- Identificador o contexto de la cancha o complejo del `dueño`.
- Identificador o contexto del `dueño` autenticado.
- Reseñas ya publicadas asociadas a canchas del dueño.

## Datos de salida

- Lista de reseñas recibidas.
- Rating y comentario básico por reseña.
- Estado vacío cuando no existan reseñas.
- Error claro si no se pueden cargar las reseñas.

## Dependencias

- `#17` para la existencia de reseñas post-reserva.
- `#18` para rating general en estrellas.
- Relación clara entre `dueño`, complejo y cancha.
- Persistencia de reseñas asociadas a reserva/cancha.

## Criterios de aceptación

1. Dado un `dueño` de una cancha, cuando accede a la vista de reseñas recibidas, entonces el sistema muestra reseñas asociadas a sus canchas.
2. Dada una reseña recibida, cuando se muestra en la lista, entonces incluye rating y comentario básico si existe.
3. Dado un `dueño` cuya cancha no tiene reseñas, cuando entra a la vista, entonces el sistema muestra un estado vacío claro.
4. Dado un `dueño` que intenta consultar reseñas de una cancha fuera de su dominio, cuando el sistema valida el acceso, entonces no expone esa información.
5. Dado el alcance de Sprint 5, cuando se revisa `#21`, entonces no depende de fotos, cuestionarios, métricas estructuradas, moderación ni dashboards.
6. Dada una falla de carga de reseñas, cuando el sistema no puede recuperar información, entonces comunica el error sin mostrar un éxito engañoso.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define lectura simple de reseñas recibidas por el dueño.
- El spec muestra rating y comentario básico como información mínima.
- El spec contempla estado vacío y acceso restringido a canchas propias.
- El spec excluye moderación, respuestas, fotos, cuestionarios, métricas avanzadas y dashboards.

## Notas de partición

- `#17` responde: `cómo el mejenguero crea una reseña básica desde reserva finalizada`.
- `#18` responde: `cómo se define y guarda el rating`.
- `#21` responde: `cómo el dueño lee reseñas ya existentes`.
- Si la decisión pertenece a crear, validar o publicar una reseña, cae en `#17/#18`.
- Si la decisión pertenece a consultar reseñas ya recibidas desde la perspectiva del dueño, cae en `#21`.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/20
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#20
Current issue: TheMonstersP4/mejengueros-app#21
