# Issue #2: Estandarizar contrato global de respuestas del API

## Título

Estandarizar el contrato global de respuestas HTTP del API.

## Objetivo

Definir e implementar un formato único de respuesta para el API de Mejengueros, de forma que los clientes puedan consumir cualquier endpoint con una estructura consistente basada en `success`, `data`, `errors` y `meta`, sin tener que manejar formatos distintos por endpoint.

## Relación con issues coordinados

- `#2` forma parte de `#6 Infraestructura Técnica`.
- `#2` complementa el estándar actual de manejo de errores del servidor.
- `#2` debe aplicarse transversalmente a endpoints existentes y futuros.
- `#2` prepara una base consistente para las features funcionales de canchas, catálogo, reseñas, favoritos, usuarios y administración.
- `#2` debe incluir sus propias pruebas unitarias backend sobre el contrato de respuesta, porque la historia de pruebas unitarias de frontend del origen fue descartada como issue transversal y la validación mínima queda como DoD de cada issue implementable.

## Historia de usuario

Como equipo de desarrollo,
queremos que todas las respuestas HTTP del API tengan una estructura estándar,
para simplificar el consumo desde cliente/KMP, reducir casos especiales por endpoint y mantener una base técnica limpia durante el MVP.

## Alcance

- Definir un envelope estándar para respuestas exitosas y respuestas de error.
- Implementar un interceptor global de respuestas exitosas en NestJS.
- Adaptar el filtro global de errores para responder con el mismo envelope estándar.
- Mantener HTTP status codes correctos según el resultado real de cada operación.
- Preservar los códigos de error actuales del servidor, como `AUTH_INVALID_TOKEN`, `VALIDATION_FAILED`, `RESOURCE_NOT_FOUND` e `INTERNAL_SERVER_ERROR`.
- Incluir metadata común como `requestId`, `path` y `timestamp` cuando esté disponible.
- Incluir metadata de paginación para endpoints de listado cuando aplique.
- Actualizar pruebas unitarias existentes que validan respuestas HTTP.
- Documentar la convención en la documentación técnica del servidor.

## Fuera de alcance

- Rediseñar todos los DTOs de dominio o persistencia.
- Cambiar la semántica funcional de endpoints existentes.
- Definir OpenAPI completo para todos los endpoints.
- Implementar paginación real en todos los listados si cada feature todavía no la requiere.
- Cambiar la arquitectura de módulos del servidor.
- Modificar reglas de negocio de canchas, usuarios, reseñas, favoritos o autenticación.
- Exponer stack traces, detalles internos de infraestructura, SQL, tokens, cookies o payloads sensibles al cliente.

## Reglas de negocio

1. Toda respuesta HTTP del API debe poder interpretarse con la misma estructura base.
2. El campo `success` indica si la operación fue exitosa desde el punto de vista del API.
3. En respuestas exitosas, `success` debe ser `true`.
4. En respuestas exitosas, `data` contiene el recurso, colección, confirmación o payload propio del endpoint.
5. En respuestas exitosas, `errors` debe ser un arreglo vacío.
6. En respuestas de error, `success` debe ser `false`.
7. En respuestas de error, `data` debe ser `null`.
8. En respuestas de error, `errors` debe contener uno o más errores seguros para el cliente.
9. `meta` debe reservarse para información transversal, no para datos principales del negocio.
10. `meta.requestId`, `meta.path` y `meta.timestamp` deben incluirse cuando estén disponibles.
11. Para listados, la paginación debe representarse dentro de `meta.pagination` cuando aplique.
12. Los errores deben conservar códigos estables y machine-readable.
13. Los HTTP status codes deben seguir representando correctamente el resultado: `2xx` para éxito, `4xx` para errores del cliente, `5xx` para errores del servidor o servicios externos.
14. Los controllers no deben construir manualmente el envelope estándar en cada endpoint.
15. La transformación de respuestas debe resolverse en una capa global, para evitar duplicación y respuestas inconsistentes.
16. La validación de entrada debe seguir usando el mecanismo global actual del servidor, pero su salida HTTP debe adaptarse al envelope estándar.
17. El contrato debe ser simple para el MVP, pero suficientemente estable para que clientes futuros no tengan que manejar formatos incompatibles.

## Contrato estándar de respuesta

### Forma base

```json
{
  "success": true,
  "data": {},
  "errors": [],
  "meta": {}
}
```

### Campos

- `success`: booleano obligatorio que indica éxito o error.
- `data`: payload principal de la respuesta. Puede ser objeto, arreglo, primitivo controlado o `null`.
- `errors`: arreglo obligatorio de errores. Vacío en respuestas exitosas.
- `meta`: objeto obligatorio para metadata transversal. Puede estar vacío si no hay metadata relevante.

## Respuestas exitosas esperadas

### Recurso simple

```json
{
  "success": true,
  "data": {
    "id": "user_123",
    "email": "user@example.com",
    "name": "Usuario Demo"
  },
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/users/me",
    "timestamp": "2026-06-04T00:00:00.000Z"
  }
}
```

### Colección o listado

```json
{
  "success": true,
  "data": [
    {
      "id": "court_123",
      "name": "Cancha La Mejenga"
    }
  ],
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/courts",
    "timestamp": "2026-06-04T00:00:00.000Z",
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 57,
      "totalPages": 3
    }
  }
}
```

### Estado vacío

```json
{
  "success": true,
  "data": [],
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/courts",
    "timestamp": "2026-06-04T00:00:00.000Z",
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 0,
      "totalPages": 0
    }
  }
}
```

### Creación o actualización

```json
{
  "success": true,
  "data": {
    "id": "court_123",
    "status": "draft",
    "message": "Cancha creada. Ahora completá la información restante."
  },
  "errors": [],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/courts",
    "timestamp": "2026-06-04T00:00:00.000Z"
  }
}
```

## Respuestas de error esperadas

### Error base

```json
{
  "success": false,
  "data": null,
  "errors": [
    {
      "code": "AUTH_INVALID_TOKEN",
      "message": "Autenticaciónentication token is invalid or expired.",
      "status": 401,
      "type": "urn:problem-type:servidor:autenticación-invalid-token"
    }
  ],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/autenticación/me",
    "timestamp": "2026-06-04T00:00:00.000Z"
  }
}
```

### Error de validación

```json
{
  "success": false,
  "data": null,
  "errors": [
    {
      "code": "VALIDATION_FAILED",
      "message": "El nombre de la cancha es obligatorio.",
      "status": 400,
      "field": "name"
    }
  ],
  "meta": {
    "requestId": "request-id",
    "path": "/v1/courts",
    "timestamp": "2026-06-04T00:00:00.000Z"
  }
}
```

## Forma conceptual de un error

Cada item dentro de `errors` debe incluir, como mínimo:

- `code`: código estable y machine-readable.
- `message`: mensaje seguro para cliente/usuario.
- `status`: HTTP status code asociado.

Puede incluir adicionalmente:

- `field`: campo afectado, especialmente en validaciones.
- `type`: identificador conceptual del problema.
- `details`: información segura adicional cuando ayude al cliente.

## Flujo principal

1. El cliente envía una solicitud HTTP al API.
2. El controller o caso de uso produce el resultado propio del endpoint.
3. El interceptor global envuelve el resultado exitoso en el contrato estándar.
4. Si ocurre un error esperado o inesperado, el filtro global de errores lo normaliza.
5. El API responde siempre con `success`, `data`, `errors` y `meta`.
6. El cliente procesa la respuesta sin necesitar lógica distinta por endpoint.

## Casos alternos/validaciones

- Si un controller devuelve un objeto simple, el interceptor debe colocarlo dentro de `data`.
- Si un controller devuelve un arreglo, el interceptor debe colocarlo dentro de `data`.
- Si un endpoint necesita metadata de paginación, debe existir una forma explícita de pasarla a `meta.pagination`.
- Si ocurre un error de dominio o aplicación, el filtro debe mapearlo a `success = false`, `data = null` y un item en `errors`.
- Si ocurre un error de validación de NestJS, el filtro debe mapear los mensajes a `errors` seguros para el cliente.
- Si ocurre un error desconocido, el filtro debe devolver `INTERNAL_SERVER_ERROR` sin exponer detalles internos.
- Si un endpoint no tiene contenido útil para devolver, debe definirse si responde con `data = null` dentro del envelope o si mantiene `204 No Content`; para el MVP se prefiere envelope consistente salvo decisión técnica explícita.
- Si una respuesta ya viene envuelta por una excepción justificada, no debe envolverse doblemente.

## Datos de entrada

- Resultado exitoso producido por controllers o casos de uso.
- Errores de dominio, aplicación, infraestructura o framework.
- Request HTTP actual para extraer `path`, `requestId` y timestamp.
- Metadata opcional de paginación o contexto transversal.

## Datos de salida

- Respuesta exitosa con `success = true`, `data`, `errors = []` y `meta`.
- Respuesta de error con `success = false`, `data = null`, `errors` y `meta`.
- Códigos HTTP correctos.
- Documentación técnica actualizada del contrato.
- Pruebas unitarias actualizadas para success interceptor y error filter.

## Dependencias

- Servidor NestJS existente en `api/`.
- Filtro global actual de errores del servidor.
- Validación global actual del servidor.
- Códigos de error existentes en `APP_ERROR_CODES`.
- Pruebas unitarias de backend.
- Decisión del equipo de adoptar envelope global para el MVP.

## Criterios de aceptación

1. Dado un endpoint exitoso que devuelve un objeto, cuando el cliente recibe la respuesta, entonces la respuesta contiene `success = true`, el objeto dentro de `data`, `errors = []` y `meta`.
2. Dado un endpoint exitoso que devuelve un arreglo, cuando el cliente recibe la respuesta, entonces la respuesta contiene el arreglo dentro de `data`.
3. Dado un endpoint de listado con paginación, cuando el cliente recibe la respuesta, entonces la paginación aparece dentro de `meta.pagination`.
4. Dado un endpoint sin resultados, cuando el cliente recibe la respuesta, entonces `success = true`, `data` representa el estado vacío y `errors = []`.
5. Dado un error de autenticación, cuando el cliente recibe la respuesta, entonces `success = false`, `data = null`, `errors` contiene un código estable de autenticación y el HTTP status es `401`.
6. Dado un error de validación, cuando el cliente recibe la respuesta, entonces `success = false`, `data = null`, `errors` contiene los campos o mensajes afectados y el HTTP status es `400`.
7. Dado un error desconocido, cuando el cliente recibe la respuesta, entonces no se exponen detalles internos y el código público es `INTERNAL_SERVER_ERROR`.
8. Dado cualquier respuesta HTTP JSON del API, cuando se revisa su estructura, entonces usa los campos base `success`, `data`, `errors` y `meta`.
9. Dado el servidor existente, cuando se implementa el estándar, entonces los controllers no duplican manualmente el envelope.
10. Dado el estándar nuevo, cuando se ejecutan las pruebas unitarias relevantes, entonces cubren respuestas exitosas, errores de dominio, errores de validación y errores desconocidos.
11. Dado el estándar nuevo, cuando se revisa la documentación técnica, entonces queda claro cómo deben responder endpoints simples, listados, estados vacíos, creaciones, actualizaciones y errores.

## Definition of Done

- Existe un tipo o UI central para el envelope de respuesta del API.
- Existe un interceptor global para respuestas exitosas.
- El filtro global de errores responde usando el mismo envelope estándar.
- Se conservan HTTP status codes correctos.
- Se conservan códigos de error públicos estables.
- Se actualiza la documentación técnica del servidor.
- Se actualizan o agregan pruebas unitarias del interceptor y del filtro de errores.
- Los endpoints actuales relevantes quedan alineados con el contrato estándar.
- No se expone información sensible en respuestas de error.
- El equipo puede consumir cualquier endpoint esperando `success`, `data`, `errors` y `meta`.

## Notas de implementación sugeridas

- Implementar el estándar en una capa global de NestJS, no manualmente en cada controller.
- Usar un `APP_INTERCEPTOR` para respuestas exitosas.
- Reutilizar el filtro global actual de errores como base, adaptando su salida al nuevo envelope.
- Mantener los códigos de error actuales como parte de `errors[].code`.
- Mantener `type` como dato opcional dentro de cada error si ayuda a preservar trazabilidad del estándar actual.
- Evitar doble wrapping de respuestas.
- Documentar una estrategia simple para endpoints que requieran `meta.pagination`.

## Notas de partición

- `#2` define el contrato transversal de respuesta HTTP.
- Los issues funcionales siguen definiendo comportamiento, datos funcionales y criterios de aceptación.
- Los DTOs específicos de cada endpoint siguen perteneciendo a sus features.
- La paginación concreta de cada listado pertenece a la feature correspondiente, pero su representación dentro de `meta.pagination` queda definida por `#2`.
- La semántica de errores específicos pertenece a cada módulo, pero su forma HTTP queda definida por `#2`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/44
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#44
Current issue: TheMonstersP4/mejengueros-app#2