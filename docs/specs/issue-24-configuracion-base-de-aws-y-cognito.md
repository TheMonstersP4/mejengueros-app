# Issue #24: Configuración base de AWS y Cognito

## Título

Configurar cuenta AWS y Cognito para autenticación.

## Objetivo

Preparar la infraestructura base de autenticación en AWS para que Cognito pueda operar como proveedor central de identidad del proyecto.

## Relación con issues coordinados

- `#24` forma parte de `#5 Seguridad y Autenticación`.
- `#23`, `#26` y `#27` dependen de que Cognito exista.
- `#25` complementa esta configuración con callbacks, dominios y variables.

## Historia de usuario

Como equipo de desarrollo,
queremos configurar AWS y Amazon Cognito,
para que el registro manual y los proveedores sociales funcionen sobre una infraestructura segura y reproducible.

## Alcance

- Configurar el User Pool de Cognito.
- Configurar el App Client.
- Configurar dominio Hosted UI.
- Exponer outputs necesarios para API y cliente.
- Definir políticas básicas de contraseña.
- Mantener secretos fuera del repositorio.

## Fuera de alcance

- Crear el OAuth client de Google manualmente dentro de esta historia.
- Resolver el detalle funcional de inicio de sesión con Google; eso corresponde a `#26`.
- Resolver el detalle funcional de inicio de sesión con Microsoft; eso corresponde a `#27`.
- Definir modelo relacional de usuarios; eso corresponde a `#29`.

## Reglas de negocio

1. Cognito debe ser creado o actualizado mediante Terraform cuando aplique.
2. El dominio de Cognito debe ser determinístico por proyecto y ambiente.
3. La configuración sensible no debe versionarse.
4. El App Client debe permitir los flujos necesarios para Hosted UI.
5. Los outputs deben poder reutilizarse en API, cliente y GitHub Actions.

## Lifecycle conceptual

1. Se definen variables del ambiente.
2. Terraform crea o actualiza Cognito.
3. Terraform expone outputs de User Pool, App Client y dominio.
4. Cliente y API consumen esos valores mediante variables de entorno.

## Flujo principal

1. El equipo configura variables de Terraform.
2. Ejecuta plan y apply.
3. Terraform crea User Pool, App Client y dominio.
4. El equipo copia outputs necesarios a los entornos correspondientes.
5. Se valida que Hosted UI pueda abrirse.

## Casos alternos/validaciones

- Si falta una variable requerida, Terraform debe fallar antes de crear recursos incompletos.
- Si el dominio de Cognito ya está en uso, se debe ajustar el nombre del ambiente o proyecto.
- Si falta un secreto OAuth, el proveedor social correspondiente no debe considerarse listo.

## Datos de entrada

- Cuenta AWS.
- Region AWS.
- Nombre del proyecto.
- Ambiente.
- URLs iniciales de callback y logout.
- Variables de Terraform.

## Datos de salida

- Cognito User Pool ID.
- Cognito App Client ID.
- Cognito Hosted UI domain.
- Outputs para API y cliente.
- Recursos versionados en Terraform.

## Dependencias

- Acceso a AWS.
- Terraform inicializado.
- Servidor de estado o estado local disponible.
- Dominio o URL local/cliente definida para callbacks.

## Criterios de aceptación

1. Dado un ambiente configurado, cuando se ejecuta Terraform, entonces se crea o actualiza Cognito sin errores.
2. Dado el User Pool creado, cuando se consulta el output de Terraform, entonces se obtiene User Pool ID y App Client ID.
3. Dado el dominio Hosted UI, cuando se abre la URL de inicio de sesión, entonces Cognito muestra el flujo de autenticación.
4. Dado un callback permitido, cuando Cognito completa el inicio de sesión, entonces puede redirigir al cliente.
5. Dado un valor sensible, cuando se revisa el repositorio, entonces no aparece hardcodeado en archivos versionados.

## Definition of Done

- Cognito queda creado o documentado mediante Terraform.
- El App Client queda listo para Hosted UI.
- El dominio de Cognito queda disponible.
- Los outputs necesarios quedan identificados.
- No hay secretos versionados.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/23
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#23
Current issue: TheMonstersP4/mejengueros-app#24