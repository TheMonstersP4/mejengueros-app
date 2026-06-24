# Mejengueros API

API backend principal de Mejengueros. Es una API NestJS con Fastify, Pino, Cognito, Prisma y handlers Lambda para API Gateway WebSocket.

La API HTTP expone endpoints versionados bajo `/v1`. Cognito gestiona el login social con Google y Microsoft; esta API valida los tokens emitidos por Cognito. Los handlers WebSocket viven en el mismo paquete pero se despliegan como Lambdas pequenas para que cada evento WebSocket no necesite inicializar la app HTTP completa.

Todas las respuestas JSON usan el envelope estandar `success`, `data`, `errors` y `meta` documentado en `docs/api-response-contract.md`.

## Requisitos

- Node.js 22
- npm
- Valores reales de Cognito para probar rutas protegidas

## Entorno local

Copiá el ejemplo y ajustá los valores:

```powershell
Copy-Item .env.example .env
```

Variables validadas al iniciar la API:

| Variable | Requerida | Uso |
| --- | --- | --- |
| `NODE_ENV` | No | Entorno de ejecucion: `development`, `test` o `production`. |
| `PORT` | No | Puerto HTTP local. Default: `3000`. |
| `LOG_LEVEL` | No | Nivel de Pino: `trace`, `debug`, `info`, `warn`, `error` o `fatal`. |
| `ERROR_DOCUMENTATION_BASE_URL` | No | URL base para links de documentacion de errores. Puede quedar vacia en local. |
| `APP_CORS_ALLOWED_ORIGINS` | No | Origenes de browser permitidos para llamar a la API, separados por coma. |
| `DATABASE_URL` | No | URL de PostgreSQL usada por Prisma. Incluir `schema=mejengueros_dev` al usar la base de datos compartida de Azure. |
| `DATABASE_SECRET_ARN` | No | ARN de AWS Secrets Manager usado por Lambda para cargar `DATABASE_URL` al iniciar. |
| `AWS_REGION` | Si | Region de AWS donde esta desplegado Cognito. |
| `APP_S3_BUCKET_NAME` | Si | Bucket S3 privado de la aplicacion para subida de imagenes. |
| `APP_S3_REGION` | No | Region del bucket S3. Default: `AWS_REGION`. |
| `APP_S3_KEY_PREFIX` | No | Prefijo para las object keys generadas en S3. Default: `uploads`. |
| `APP_S3_UPLOAD_URL_TTL_SECONDS` | No | TTL para formularios de subida prefirmados. Default: `300`. |
| `APP_S3_PROFILE_IMAGE_MAX_BYTES` | No | Tamano maximo de imagen de perfil. Default: `5242880`. |
| `APP_S3_ALLOWED_IMAGE_MIME_TYPES` | No | Tipos MIME de imagen permitidos, separados por coma. |
| `COGNITO_USER_POOL_ID` | Si | ID del User Pool de Cognito. |
| `COGNITO_CLIENT_ID` | Si | ID del App Client de Cognito. |
| `COGNITO_TOKEN_USE` | No | Tipo de token aceptado por la API: `id` o `access`. Default: `id`. |
| `DEMO_OWNER_SUBS` | No | Lista de Cognito subjects separados por coma que reciben el rol `OWNER` durante la reconciliacion del usuario autenticado en entornos demo/MVP, incluyendo `POST /v1/complexes` y `GET /v1/users/me`. |
| `DEMO_OWNER_EMAILS` | No | Lista de emails separados por coma como fallback para otorgar el rol `OWNER` solo cuando Cognito reporta `email_verified=true`. |
| `WEBSOCKET_CONNECTIONS_TABLE_NAME` | Si | Tabla DynamoDB para conexiones WebSocket. |
| `WEBSOCKET_CONNECTION_TTL_SECONDS` | No | TTL para conexiones WebSocket inactivas. Default: `86400`. |

Los endpoints respaldados por Prisma quedan deshabilitados hasta que `DATABASE_URL` este disponible directamente o a traves de `DATABASE_SECRET_ARN`.

Ejemplo local minimo:

```env
NODE_ENV=development
PORT=3000
LOG_LEVEL=debug
ERROR_DOCUMENTATION_BASE_URL=
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
DATABASE_URL=
DATABASE_SECRET_ARN=

AWS_REGION=us-east-2
APP_S3_BUCKET_NAME=mejengueros-dev-app-example
APP_S3_REGION=us-east-2
APP_S3_KEY_PREFIX=dev/uploads
APP_S3_UPLOAD_URL_TTL_SECONDS=300
APP_S3_PROFILE_IMAGE_MAX_BYTES=5242880
APP_S3_ALLOWED_IMAGE_MIME_TYPES=image/jpeg,image/png,image/webp

COGNITO_USER_POOL_ID=us-east-2_example
COGNITO_CLIENT_ID=example-client-id
COGNITO_TOKEN_USE=id
DEMO_OWNER_SUBS=owner-sub-from-cognito
DEMO_OWNER_EMAILS=

WEBSOCKET_CONNECTIONS_TABLE_NAME=mejengueros-dev-ws-connections
WEBSOCKET_CONNECTION_TTL_SECONDS=86400
```

## Correr localmente

Instalar dependencias:

```powershell
npm install
```

Generar Prisma:

```powershell
npm run prisma:generate
```

Iniciar la API:

```powershell
npm run start:dev
```

La API queda disponible en:

```text
http://localhost:3000/v1
```

## Probar la API localmente

Health check:

```powershell
curl http://localhost:3000/v1/health
```

Respuesta esperada:

```json
{
  "status": "ok",
  "timestamp": "2026-05-25T00:00:00.000Z"
}
```

Rutas protegidas:

```powershell
curl http://localhost:3000/v1/auth/me -H "Authorization: Bearer <id_token>"
curl http://localhost:3000/v1/users/me -H "Authorization: Bearer <id_token>"
curl -X POST http://localhost:3000/v1/complexes -H "Authorization: Bearer <id_token>" -H "Content-Type: application/json" -d '{"complex":{"name":"North Sports Center","address":"123 Main Street"},"firstCourt":{"name":"Court A"}}'
```

El `<id_token>` debe venir de Cognito Hosted UI. No enviar tokens de Google o Microsoft directamente a esta API.

## Endpoints actuales

| Metodo | Ruta | Auth | Descripcion |
| --- | --- | --- | --- |
| `GET` | `/v1/health` | No | Estado basico del proceso. |
| `GET` | `/v1/auth/me` | Si | Devuelve el usuario autenticado del token de Cognito. |
| `POST` | `/v1/files/uploads` | Si | Crea un formulario S3 POST prefirmado para imagenes de perfil. |
| `POST` | `/v1/files/uploads/confirm` | Si | Confirma una subida directa a S3 y valida propiedad, metadata y firma de bytes. |
| `GET` | `/v1/users/me` | Si | Sincroniza y devuelve el perfil local del usuario autenticado. Disponible una vez habilitado PostgreSQL. |
| `POST` | `/v1/complexes` | Si | Crea un complejo y su primera cancha para usuarios autenticados cuyo acceso `OWNER` se reconcilia dentro del mismo request. |

> Limitacion: el schema actual de `UserRole` no almacena el origen del rol. La reconciliacion del rol `OWNER` de demo es por lo tanto solo de alta: puede agregar `OWNER` para identidades en la lista de permitidos, pero no revoca filas `OWNER` existentes. Se requieren metadatos de origen antes de poder revocar roles gestionados por demo de forma independiente de asignaciones manuales o futuras de admin.

## Lambdas WebSocket

Los handlers viven en:

```text
src/functions/websocket
```

Rutas:

```text
$connect    -> functions/websocket/connect.handler
$disconnect -> functions/websocket/disconnect.handler
$default    -> functions/websocket/default.handler
```

Generar el zip usado por Terraform y GitHub Actions:

```powershell
npm run build
npm run lambda:package:websocket
```

El paquete se genera en:

```text
api/.lambda/websocket.zip
```

## Prisma

El proyecto usa Prisma 7:

```text
prisma/schema.prisma
prisma.config.ts
src/generated/prisma
```

Las tablas de la aplicacion viven en el schema de PostgreSQL `mejengueros_dev`. No usar el schema `public` de la base de datos compartida para este proyecto.

Comandos utiles:

```powershell
npm run prisma:generate
npm run prisma:validate
npx prisma migrate deploy
```

El cliente generado vive en `src/generated/prisma` y no debe ser importado desde codigo de dominio o aplicacion.

### Catalogos de ubicacion y servicios para el wizard de complejo

- `Province` y `Canton` son catalogos controlados para Costa Rica.
- `Canton` pertenece exactamente a una `Province`.
- `Complex.address` sigue siendo el texto de direccion visible para el usuario.
- `Complex.latitude` y `Complex.longitude` almacenan las coordenadas opcionales del pin en el mapa.
- `ServiceCatalog` es la unica fuente de verdad para servicios de complejo y cancha, incluyendo los tipos de cesped del MVP.
- La migracion garantiza que un `Complex.cantonId` persistido debe pertenecer al mismo `Complex.provinceId`.

Regla de transicion actual:

- `provinceId`, `cantonId`, `latitude` y `longitude` son nulos en el schema por ahora para que el contrato existente de `POST /v1/complexes` no cambie hasta que el issue de API siguiente expanda el cuerpo del request.

## Seed de demo

Un seed minimo que puebla la base de datos con datos de demo para el flujo MVP: catalogo, detalle, disponibilidad y reserva.

### Cargar o resetear

Requiere `ALLOW_DEMO_SEED=true`. No corre contra `NODE_ENV=production`.

```powershell
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
```

El seed es idempotente: borra los datos de demo existentes e inserta un conjunto nuevo en cada ejecucion.

### Que crea

| Entidad | Valor |
| --- | --- |
| Usuario dueno | `demo-owner@mejengueros.demo` - provider `demo`, subject `demo-owner-sub-00000001`, rol `OWNER` |
| Jugador 1 | `demo-player1@mejengueros.demo` - provider `demo`, subject `demo-player-sub-00000001`, rol `PLAYER` |
| Jugador 2 | `demo-player2@mejengueros.demo` - provider `demo`, subject `demo-player-sub-00000002`, rol `PLAYER` |
| Provincia | San Jose (codigo `SJ`) |
| Canton | San Jose (codigo `SJ-01`) |
| Complejo | "Complejo Demo Los Nogales" - Av. Central 1234, San Jose, Costa Rica |
| Servicios del complejo | Parqueo |
| Cancha | "Cancha 1 -- Demo" |
| Servicios de la cancha | Iluminacion, Sintetico, Natural, Hibrido |
| Disponibilidad | Lunes a sabado, 08:00 a 22:00 UTC |
| Reserva confirmada | Proximo sabado a las 10:00-11:00 UTC - en poder del Jugador 1 |
| Reserva completada | Hace 7 dias a las 10:00-11:00 UTC - en poder del Jugador 2, incluye resena de 5 estrellas |

### Escenarios de demo

- **Catalogo y detalle**: cualquier usuario puede ver el complejo y la cancha provenientes de la base de datos.
- **Slot disponible**: cualquier slot dentro del horario Lun-Sab 08:00-22:00 que no sea el sabado 10:00 UTC esta libre para reservar.
- **Error de doble reserva**: intentar reservar el slot del sabado 10:00 UTC activa el indice parcial unico en reservas confirmadas y devuelve el error de negocio.

### Teardown

Identifica datos demo por `UserIdentity.provider = 'demo'` y borra en orden seguro de FK: resenas, notificaciones, reservas (todas las de canchas demo), canchas, complejos y usuarios. Los catalogos (`Province`, `Canton`, `ServiceCatalog`) son datos compartidos y no se eliminan.

### Validacion local de idempotencia

Usar la base de datos descartable en `app-backend/api/docker/`:

```powershell
npm run docker:migration-db:up
# Configurar DATABASE_URL con la URL local (ver docker/migration-validation.env.example)
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
$env:ALLOW_DEMO_SEED="true"; npm run db:seed
npm run docker:migration-db:reset
```

Ambas ejecuciones deben completarse sin errores y dejar el conjunto demo esperado.

## Calidad

Comandos principales:

```powershell
npm run lint
npm test -- --runInBand
npm run test:integration -- --runInBand
npm run test:cov -- --runInBand
npm run build
```

Los tests unitarios viven en `test/unit`. No colocar archivos `*.spec.ts` dentro de `src`.

## Arquitectura

La API sigue DDD modular:

```text
src/modules/<feature>/
  domain/
  application/
  infrastructure/
  interfaces/
```

Reglas rapidas:

- Controllers delgados.
- Casos de uso en `application`.
- Entidades, value objects y errores de negocio en `domain`.
- Prisma, Cognito, AWS SDK y adaptadores externos en `infrastructure`.
- Controllers HTTP, guards, filters y DTOs en `interfaces`.
- Usar `shared` solo para primitivos tecnicas estables.
