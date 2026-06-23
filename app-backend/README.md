# Mejengueros Backend

Backend e infraestructura de Mejengueros.

El subproyecto concentra la API, la infraestructura cloud y un POC web de soporte para autenticación social y pruebas de WebSocket.

## Qué incluye

- `api/`: API NestJS con Fastify, Prisma, Cognito y handlers Lambda para WebSocket.
- `infra/`: Terraform para AWS, Azure AD, Cloudflare y composición de ambientes.
- `poc/`: cliente web estático para validar login Hosted UI y flujo de chat.
- `docs/`: estándares técnicos y documentación de arquitectura del backend.

## Estructura

```text
.
|-- api/      API NestJS, Prisma, tests y WebSocket Lambda handlers
|-- infra/    Terraform por módulos y composición de ambientes
|-- poc/      Cliente web estático para probar login Cognito + WebSocket
|-- docs/     Estándares técnicos y decisiones de arquitectura
`-- ../.github/  Workflows y scripts de deploy en la raíz del repositorio
```

## Prerrequisitos

- Node.js 22 y npm para `api/`.
- Terraform para `infra/`.
- Valores reales de Cognito y AWS cuando se prueban rutas protegidas o despliegues.

## API

La API está en `api/`.

Incluye:

- NestJS con Fastify.
- Pino para logs HTTP.
- Cognito como broker de identidad para Google y Microsoft.
- Prisma 7 para persistencia.
- Estructura DDD por módulo.
- Tests unitarios fuera de `src`.
- Handlers Lambda para API Gateway WebSocket.

Guía local:

```text
api/README.md
```

Comandos principales:

```powershell
cd api
npm install
npm run start:dev
npm run lint
npm test -- --runInBand
npm run test:cov -- --runInBand
npm run build
```

La API local expone rutas bajo:

```text
http://localhost:3000/v1
```

## Infraestructura

Terraform está en `infra/`.

Incluye:

- Cognito User Pool con Google y Microsoft como identity providers.
- Microsoft Entra app registration automatizable con Terraform.
- VPC privada sin NAT gateway por defecto.
- S3 para archivos de aplicación.
- ECR para imágenes Docker.
- API Gateway WebSocket con DynamoDB para conexiones por room.
- Lambdas para rutas `$connect`, `$disconnect` y `$default`.
- CloudWatch log groups con retención definida.
- POC site en S3 + Cloudflare Worker.
- GitHub Actions OIDC deploy role.

Estructura:

```text
infra/
|-- env/      tfvars y backend examples por ambiente
|-- modules/  módulos reutilizables
`-- root/     composición del ambiente
```

Antes de ejecutar Terraform, crea tus archivos locales reales a partir de los ejemplos versionados:

```powershell
Copy-Item 'infra\env\dev.tfvars.example' 'infra\env\dev.tfvars'
Copy-Item 'infra\env\dev.backend.hcl.example' 'infra\env\dev.backend.hcl'
```

Comandos desde `app-backend/`:

```powershell
terraform -chdir=infra/root init
terraform -chdir=infra/root plan -var-file '..\env\dev.tfvars'
terraform -chdir=infra/root apply -var-file '..\env\dev.tfvars'
```

Si usas backend remoto:

```powershell
terraform -chdir=infra/root init -backend-config '..\env\dev.backend.hcl'
```

## POC Web

El POC está en `poc/web-chat`.

Sirve para probar:

- Login con Cognito Hosted UI.
- Callback OAuth con PKCE.
- Página protegida `/chat/`.
- Conexión WebSocket.
- Mensajes y usuarios conectados por room.

Local:

```powershell
cd poc/web-chat
python -m http.server 3000
```

Abrir:

```text
http://localhost:3000
```

## Documentación

`docs/` es la fuente canónica de estándares del proyecto.

Documentos principales:

- `docs/nestjs-ddd-structure.md`
- `docs/ddd-solid-standards.md`
- `docs/error-handling-standards.md`
- `docs/prisma-standards.md`
- `docs/terraform-standards.md`
- `docs/tsdoc-standards.md`
- `docs/unit-test-standards.md`
- `docs/websocket-architecture.md`

## Dónde profundizar

- [`api/README.md`](api/README.md): variables de entorno, endpoints, calidad y arquitectura de la API.
- [`infra/README.md`](infra/README.md): módulos Terraform, entradas/salidas y despliegue de infraestructura.
- [`poc/web-chat/README.md`](poc/web-chat/README.md): flujo del POC web para autenticación y chat.
- [`docs/`](docs/): estándares y decisiones técnicas del backend.

## Deploy

GitHub Actions está en la carpeta `.github/` de la raíz del repositorio, no dentro de `app-backend/`.

El deploy es script-driven:

- La workflow detecta cambios.
- Usa OIDC para asumir rol en AWS.
- Corre quality gate antes de publicar API y WebSocket.
- Los scripts viven en la ruta repo-root `.github/scripts`.

Secrets requeridos para ambiente `dev`:

```text
AWS_ROLE_ARN_DEV
DEPLOY_DEV_CONFIG
```

Se obtienen desde Terraform:

```powershell
terraform -chdir=infra/root output -raw github_actions_iam_role_arn
terraform -chdir=infra/root output -json deploy_config
```

## Seguridad

No se deben commitear:

- `.env`
- `.tfvars` reales
- backend configs reales
- secretos OAuth
- passwords de base de datos
- Terraform state
- paquetes Lambda generados
- Prisma generated client
- coverage o build output

Los ejemplos viven como `*.example` y los valores reales se manejan localmente o por secrets del ambiente.
