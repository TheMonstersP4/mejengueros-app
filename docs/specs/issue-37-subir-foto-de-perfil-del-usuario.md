# Issue #37: Subir foto de perfil del usuario

## Título

Permitir que el `mejenguero` suba o reemplace su foto de perfil personal.


## Nota de priorización MVP semana 10

Foto de perfil queda post-MVP salvo que se justifique para identidad visual; no es necesaria para reserva básica.

## Objetivo

Definir la capacidad MVP para que un `mejenguero` autenticado pueda subir una imagen como foto de perfil, usando la infraestructura de almacenamiento en la nube definida en `#28`, de modo que su identidad visual dentro del producto quede representada por una imagen elegida por él mismo.

## Relación con #28, #29, #23, #36, #34, #35, #38 y #9

- `#28` define la capacidad técnica de subir y almacenar imágenes en S3 usando URLs firmadas, con validación de tipo MIME, tamaño y permisos. `#37` consume directamente ese mecanismo: la subida de la foto de perfil debe seguir el flujo de URL firmada establecido en `#28`.
- `#29` provee el esquema relacional que permite asociar la referencia de la imagen al perfil del usuario. `#37` depende de que exista un modelo de datos capaz de guardar y actualizar esa referencia.
- `#23` establece la cuenta de usuario que origina el perfil. La foto de perfil de `#37` complementa esa identidad base con una representación visual.
- `#36` resuelve la edición de datos textuales del perfil (apodo y posición favorita). `#36` y `#37` son capacidades complementarias del mismo perfil pero sin dependencia directa entre sí.
- `#34`, `#35` y `#38` pertenecen al mismo bloque de perfil en `#9` pero son independientes de `#37`.
- `#9` es la épica contenedora que agrupa las capacidades de perfil y fidelización del usuario.

## Historia de usuario

Como `mejenguero`,
quiero subir una foto de perfil que me represente dentro del producto,
para tener una identidad visual personalizada que los demás usuarios puedan asociar a mi nombre dentro del ecosistema.

## Alcance

- Permitir al `mejenguero` autenticado seleccionar y subir una imagen como foto de perfil.
- Usar el flujo de URL firmada de `#28` para cargar la imagen a S3 de forma segura.
- Validar que el archivo seleccionado cumpla el tipo y tamaño permitidos, conforme a las reglas de `#28`.
- Asociar la imagen subida al perfil del usuario autenticado.
- Reemplazar la foto anterior si ya existía una, sin acumular versiones.
- Mostrar la foto de perfil actual antes de iniciar el cambio, para que el usuario tenga contexto visual.
- Mostrar un avatar o placeholder cuando el usuario no tiene foto de perfil cargada.
- Confirmar al usuario cuando la foto se subió y asoció correctamente.

## Fuera de alcance

- Recortar, editar, rotar o aplicar filtros a la imagen dentro del producto; `#28` tampoco lo contempla.
- Subir múltiples fotos de perfil o gestionar un álbum de fotos personales.
- Usar la foto de perfil para canchas, reseñas u otros recursos; esos flujos de imagen pertenecen a sus propios issues.
- Implementar una galería de avatares predefinidos.
- Comprometer contratos técnicos de API, política exacta de tipos MIME permitidos ni tamaño máximo en bytes; esas reglas pertenecen a `#28`.
- Eliminar la foto de perfil sin reemplazarla (volver a estado sin foto); no es parte del MVP.
- Diseñar la moderación o revisión editorial de fotos de perfil.

## Reglas de negocio

1. Sólo un `mejenguero` autenticado puede subir o cambiar su propia foto de perfil.
2. La subida debe seguir el flujo de URL firmada de `#28`: el cliente solicita la URL a la API, la API valida permisos y retorna la URL firmada, el cliente sube directamente a S3.
3. El tipo de archivo y el tamaño deben cumplir las reglas definidas en `#28`; si no cumplen, la operación debe rechazarse con un mensaje claro.
4. La nueva foto reemplaza la foto anterior del usuario; no se acumulan versiones dentro del MVP.
5. Una vez subida y confirmada la asociación, el perfil del usuario debe mostrar la nueva foto.
6. Si el usuario no tiene foto de perfil, el sistema debe mostrar un avatar o placeholder por defecto en lugar de un espacio vacío.
7. Si la subida falla en cualquier punto del flujo, el sistema debe comunicarlo sin dejar la foto en un estado visual inconsistente.
8. `#37` no define qué tipos MIME están permitidos ni el tamaño máximo exacto; esas reglas pertenecen a `#28` y `#37` las consume.

## Qué significa subir foto de perfil en este MVP

- Significa que el `mejenguero` puede tener una imagen visual asociada a su identidad dentro del producto.
- Significa que esa imagen se sube de forma segura a través del mecanismo de `#28`, sin cargar archivos directamente al servidor de API.
- Significa que una nueva foto reemplaza la anterior.
- No significa editar ni recortar la imagen dentro del producto.
- No significa gestionar múltiples fotos o un historial de fotos de perfil.
- No significa usar esa foto como imagen de cancha o de reseña; esos son flujos de imagen independientes.

## Flujo principal

1. El `mejenguero` accede a su perfil dentro del contexto de `#9`.
2. El usuario identifica la sección de foto de perfil y elige subir o cambiar su foto.
3. El sistema muestra la foto actual o el placeholder si no tiene ninguna.
4. El usuario selecciona una imagen desde su dispositivo.
5. El cliente solicita una URL firmada a la API, enviando el tipo de recurso y los metadatos necesarios.
6. La API valida que el usuario está autenticado, que el tipo MIME y el tamaño están dentro de los límites de `#28`, y retorna la URL firmada.
7. El cliente sube la imagen directamente a S3 usando esa URL firmada.
8. El sistema asocia la imagen subida al perfil del `mejenguero`.
9. El sistema confirma al usuario que la foto se actualizó correctamente.
10. El perfil muestra la nueva foto.

## Casos alternos/validaciones

- Si el archivo seleccionado no es de un tipo MIME permitido, el sistema debe rechazar la operación y comunicarlo antes de intentar la subida.
- Si el archivo supera el tamaño máximo definido en `#28`, el sistema debe rechazar la operación y comunicarlo.
- Si la solicitud de URL firmada falla, el sistema debe comunicar que no fue posible iniciar la subida.
- Si la subida a S3 falla después de obtener la URL firmada, el sistema debe comunicarlo sin actualizar la foto del perfil.
- Si la asociación de la imagen al perfil falla tras la subida exitosa, el sistema debe comunicarlo y no mostrar la nueva foto hasta que la asociación esté confirmada.
- Si el usuario cancela antes de completar el flujo, la foto anterior debe mantenerse sin cambios.
- Si el usuario no tiene foto de perfil, el sistema debe mostrar un avatar o placeholder por defecto hasta que suba una.

## Datos de entrada

- Identificador o contexto del `mejenguero` autenticado.
- Archivo de imagen seleccionado por el usuario.
- Tipo de uso: perfil de usuario (según los tipos de recurso definidos en `#28`).
- Tipo MIME y tamaño del archivo para validación previa.

Notas:

- Este issue define inputs funcionales de producto, no el contrato técnico exacto del flujo de URL firmada.
- Las reglas de validación de tipo y tamaño están definidas en `#28`; `#37` las consume.

## Datos de salida

- Imagen subida a S3 y asociada al perfil del `mejenguero`.
- Foto de perfil actualizada y visible dentro del producto.
- Confirmación visible de que la foto se cargó correctamente.
- Mensaje de error claro si algún paso del flujo no pudo completarse.
- Avatar o placeholder cuando el usuario no tiene foto de perfil.

## Dependencias

- `#28` para el mecanismo de URL firmada, validación de tipo MIME, tamaño y subida segura a S3. Es la dependencia técnica directa y funcional de `#37`.
- `#29` para la persistencia de la referencia de la imagen asociada al perfil del usuario.
- Autenticación vigente del usuario, requerida tanto por `#37` como por `#28`.
- Perfil de usuario existente derivado del registro en `#23`.

Notas:

- `#34`, `#35`, `#36` y `#38` pertenecen al mismo bloque de perfil en `#9` pero no son prerequisitos para que `#37` exista.
- `#37` no debe implementar lógica de almacenamiento de imágenes propia; ese mecanismo pertenece íntegramente a `#28`.

## Criterios de aceptación

1. Dado un `mejenguero` autenticado que accede a su foto de perfil, cuando no tiene ninguna cargada, entonces el sistema muestra un avatar o placeholder por defecto.
2. Dado un `mejenguero` autenticado que selecciona una imagen válida en tipo y tamaño, cuando completa el flujo de subida, entonces el sistema asocia la imagen a su perfil y la muestra actualizada.
3. Dado que el flujo usa el mecanismo de `#28`, cuando se revisa el spec de `#37`, entonces queda claro que la subida ocurre mediante URL firmada hacia S3 y no mediante carga directa al servidor de API.
4. Dado un archivo con tipo MIME no permitido, cuando el usuario intenta subirlo, entonces el sistema rechaza la operación y comunica el motivo antes de intentar la subida.
5. Dado un archivo que supera el tamaño máximo definido en `#28`, cuando el usuario intenta subirlo, entonces el sistema rechaza la operación y comunica el motivo.
6. Dado que el usuario ya tiene una foto de perfil y sube una nueva, cuando el proceso finaliza correctamente, entonces la nueva foto reemplaza a la anterior.
7. Dada una falla en cualquier punto del flujo de subida o asociación, cuando el sistema no puede completar la operación, entonces comunica el error y no muestra una foto en estado inconsistente.
8. Dado un usuario no autenticado, cuando intenta subir o cambiar la foto de perfil, entonces el sistema no permite la operación.
9. Dado el alcance MVP de `#37`, cuando se revisa el spec, entonces queda claro que no incluye recorte, edición, filtros, múltiples fotos ni moderación editorial.
10. Dado el vínculo con `#9`, cuando se revisa el spec, entonces queda claro que la foto de perfil es una sección del perfil del usuario complementaria a los datos textuales de `#36`.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define con claridad que `#37` consume el mecanismo de `#28` y no implementa lógica propia de almacenamiento de imágenes.
- Queda explícita la dependencia directa con `#28` y la dependencia de persistencia con `#29`.
- Queda claro que la nueva foto reemplaza la anterior sin acumular versiones.
- Queda contemplado el estado de placeholder o avatar cuando no hay foto cargada.
- Queda contemplado el manejo de errores en cada punto del flujo: validación previa, solicitud de URL, subida a S3 y asociación al perfil.
- Queda explícita la restricción de que sólo el usuario autenticado puede modificar su propia foto.
- El documento mantiene enfoque de producto y MVP, sin bajar a decisiones técnicas propias de `#28`.

## Notas de partición

- `#28` responde: `cómo el sistema sube y almacena imágenes en S3 de forma segura mediante URLs firmadas`.
- `#29` responde: `cómo se registra y relaciona la metadata de imágenes en el esquema de datos`.
- `#36` responde: `cómo el mejenguero edita su apodo y posición favorita`.
- `#37` responde: `cómo el mejenguero sube o reemplaza su foto de perfil usando la infraestructura de #28`.
- `#34` responde: `cómo el mejenguero consulta su historial de reseñas realizadas`.
- `#35` responde: `cómo el mejenguero consulta y gestiona su lista de canchas favoritas`.
- Si la decisión pertenece al mecanismo técnico de subida a S3, tipos permitidos o tamaño máximo, cae en `#28`.
- Si la decisión pertenece a la persistencia relacional de la referencia de imagen, cae en `#29`.
- Si la decisión pertenece a la experiencia del usuario al gestionar su foto de perfil, cae en `#37`.
- `#37` no debe duplicar ni redefinir las reglas de `#28`; sólo las consume como dependencia.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/36
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#36
Current issue: TheMonstersP4/mejengueros-app#37