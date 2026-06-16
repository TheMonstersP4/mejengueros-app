# Issue #28: Subir y almacenar imágenes en la nube


## Título

Subir y almacenar imágenes en la nube.

## Objetivo

Permitir que usuarios autorizados suban imágenes a S3 para perfiles, establecimientos, canchas o reseñas sin cargar archivos directamente en el servidor de API, incluyendo imágenes obligatorias cuando una reseña de `1 estrella` lo requiera.

## Relación con issues coordinados

- `#28` forma parte de la gestión técnica de datos y multimedia.
- `#29` debe registrar metadata relacional de las imágenes.
- `#5` aporta autenticación para saber quién sube la imagen.

## Historia de usuario

Como usuario autorizado,
quiero subir imágenes a la nube,
para asociarlas a mi perfil, establecimientos, canchas o reseñas de forma segura, incluyendo evidencia de reseñas de `1 estrella` cuando aplique.

## Alcance

- Usar S3 como almacenamiento de imágenes.
- Mantener bucket privado.
- Generar URLs firmadas para subida o descarga.
- Validar tipo de archivo.
- Validar tamaño máximo.
- Registrar metadata cuando exista base de datos.

## Fuera de alcance

- Hacer públicos los objetos de S3 sin control.
- Implementar procesamiento avanzado de imágenes.
- Crear CDN o transformaciones automáticas.
- Definir UI final de carga de imágenes.

## Reglas de negocio

1. Solo usuarios autenticados pueden solicitar carga.
2. El bucket debe mantenerse privado.
3. La API debe controlar el tipo de recurso asociado.
4. No se deben aceptar tipos MIME no permitidos.
5. La metadata debe vincularse al recurso correspondiente cuando exista persistencia.

## Lifecycle conceptual

1. Usuario solicita subir una imagen.
2. API valida usuario, tipo de recurso y metadata.
3. API genera URL firmada.
4. Cliente sube la imagen a S3.
5. API registra metadata o confirma asociación.
6. El recurso muestra la imagen asociada.

## Flujo principal

1. Usuario autenticado elige una imagen.
2. Cliente solicita URL firmada a la API.
3. API valida permisos y metadata.
4. API responde URL firmada.
5. Cliente sube archivo a S3.
6. Sistema asocia la imagen al recurso.

## Casos alternos/validaciones

- Si el usuario no está autenticado, la API rechaza la solicitud.
- Si el archivo supera el tamaño máximo, la API rechaza la solicitud.
- Si el tipo MIME no está permitido, la API rechaza la solicitud.
- Si el usuario no tiene permiso sobre el recurso, la API rechaza la solicitud.

## Datos de entrada

- Archivo de imagen.
- Tipo de uso: perfil, establecimiento, cancha o reseña.
- Contexto funcional de obligatoriedad cuando la imagen corresponde a una reseña de `1 estrella`.
- Identificador del recurso asociado.
- Nombre original.
- Tipo MIME.
- Tamaño.

## Datos de salida

- URL firmada.
- Object key en S3.
- Registro de metadata cuando aplique.
- Confirmación de carga o error.

## Dependencias

- Bucket S3 creado por Terraform.
- Permisos IAM para la API.
- Autenticación con Cognito.
- `#29` para registrar metadata relacional.

## Criterios de aceptación

1. Dado un usuario autenticado, cuando solicita subir una imagen válida, entonces la API devuelve una URL firmada.
2. Dada una imagen con tipo no permitido, cuando solicita carga, entonces la API rechaza la operación.
3. Dada una imagen que supera el tamaño máximo, cuando solicita carga, entonces la API rechaza la operación.
4. Dada una carga exitosa a S3, cuando se consulta el recurso asociado, entonces la imagen aparece vinculada.
5. Dado un usuario sin permisos sobre un recurso, cuando intenta subir una imagen a ese recurso, entonces la API rechaza la operación.

## Definition of Done

- S3 queda como almacenamiento privado.
- La API genera URLs firmadas.
- Existen validaciones de tipo y tamaño.
- La metadata queda preparada para persistencia.
- La historia queda alineada con autenticación y modelo de datos.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/27
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#27
Current issue: TheMonstersP4/mejengueros-app#28