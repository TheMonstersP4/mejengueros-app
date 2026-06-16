# Mejengueros Backend

Backend e infraestructura de Mejengueros.

El proyecto contiene una API NestJS con Fastify, autenticacion con Cognito, handlers Lambda para WebSocket, Terraform para AWS/AzureAD/Cloudflare, un POC web para login social y chat, documentacion tecnica y skills para mantener estandares del repositorio.

## Estructura

```text
.
|-- api/      API NestJS, Prisma, tests y WebSocket Lambda handlers
|-- infra/    Terraform por modulos y composicion de ambientes
|-- poc/      Cliente web estatico para probar login Cognito + WebSocket
|-- docs/     Estandares tecnicos y decisiones de arquitectura
|-- skills/   Instrucciones operativas para agentes del proyecto
`-- .github/  Workflows y scripts de deploy
```

## API

La API esta en `api/`.

Incluye:

- NestJS con Fastify.
- Pino para logs HTTP.
- Cognito como broker de identidad para Google y Microsoft.
- Prisma 7 para persistencia.
- Estructura DDD por modulo.
- Tests unitarios fuera de `src`.
- Handlers Lambda para API Gateway WebSocket.

Guia local:

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

Terraform esta en `infra/`.

Incluye:

- Cognito User Pool con Google y Microsoft como identity providers.
- Microsoft Entra app registration automatizable con Terraform.
- VPC privada sin NAT gateway por defecto.
- S3 para archivos de aplicacion.
- ECR para imagenes Docker.
- API Gateway WebSocket con DynamoDB para conexiones por room.
- Lambdas para rutas `$connect`, `$disconnect` y `$default`.
- CloudWatch log groups con retencion definida.
- POC site en S3 + Cloudflare Worker.
- GitHub Actions OIDC deploy role.

Estructura:

```text
infra/
|-- env/      tfvars y backend examples por ambiente
|-- modules/  modulos reutilizables
`-- root/     composicion del ambiente
```

Comandos desde la raiz del repo:

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

El POC esta en `poc/web-chat`.

Sirve para probar:

- Login con Cognito Hosted UI.
- Callback OAuth con PKCE.
- Pagina protegida `/chat/`.
- Conexion WebSocket.
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

## Documentacion

`docs/` es la fuente canonica de estandares del proyecto.

Documentos principales:

- `docs/nestjs-ddd-structure.md`
- `docs/ddd-solid-standards.md`
- `docs/error-handling-standards.md`
- `docs/prisma-standards.md`
- `docs/terraform-standards.md`
- `docs/tsdoc-standards.md`
- `docs/unit-test-standards.md`
- `docs/websocket-architecture.md`

Los `skills/` deben apuntar a estos documentos cuando exista un estandar canonico.

## Skills

`skills/` contiene reglas operativas para agentes del proyecto.

Skills actuales:

- `conventional-commits`
- `nestjs-ddd-solid`
- `repo-documentation`
- `unit-testing`
- `aws-serverless-terraform`
- `github-actions-deploy`
- `security-review`

Regla general:

```text
docs/*.md                 estandar canonico
skills/*/SKILL.md         workflow corto
skills/*/references/*.md  checklist breve
```

## Deploy

GitHub Actions esta en `.github/`.

El deploy es script-driven:

- La workflow detecta cambios.
- Usa OIDC para asumir rol en AWS.
- Corre quality gate antes de publicar API y WebSocket.
- Los scripts viven en `.github/scripts`.

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
