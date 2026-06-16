# Issue #26: Iniciar sesión de forma rápida mediante Social Login con Google


## Título

Iniciar sesión de forma rápida mediante Social Login con Google.


## Nota de priorización MVP semana 10

Social login queda post-MVP por directriz de semana 10; mantener trazabilidad, pero priorizar autenticación manual suficiente para reservas.

## Objetivo

Permitir que un usuario ingrese a Mejengueros usando su cuenta de Google mediante Cognito Hosted UI.

## Relación con issues coordinados

- `#26` forma parte de `#5 Seguridad y Autenticación`.
- `#24` y `#25` deben estar listos.
- `#29` permitirá sincronizar datos locales del usuario.

## Historia de usuario

Como visitante,
quiero iniciar sesión con mi cuenta de Google,
para entrar a Mejengueros sin crear una contraseña nueva.

## Alcance

- Configurar Google como identity provider en Cognito.
- Redirigir al usuario a Cognito Hosted UI.
- Procesar callback exitoso.
- Usar token emitido por Cognito para consumir la API.
- Mostrar la pantalla protegida después del inicio de sesión.

## Fuera de alcance

- Validar tokens directos de Google en la API.
- Administrar contraseñas de Google.
- Crear recuperación de acceso de Google desde Mejengueros.
- Sincronizar perfil local completo si `#29` no está listo.

## Reglas de negocio

1. Google debe operar como proveedor federado dentro de Cognito.
2. La API solo acepta tokens emitidos por Cognito.
3. El inicio de sesión debe usar callbacks registrados.
4. La sesión debe poder cerrarse correctamente.

## Lifecycle conceptual

1. Usuario selecciona Google.
2. Cliente redirige a Cognito.
3. Cognito delega autenticación a Google.
4. Google autentica.
5. Cognito emite token.
6. Cliente entra a pantalla protegida.

## Flujo principal

1. Usuario presiona `Inicio de sesión with Google`.
2. Sistema redirige a Cognito Hosted UI.
3. Usuario se autentica con Google.
4. Cognito procesa la identidad.
5. Cognito redirige al callback.
6. Cliente guarda sesión.
7. API responde `/v1/autenticación/me`.

## Casos alternos/validaciones

- Si Google rechaza el inicio de sesión, el usuario vuelve sin sesión.
- Si el callback no está permitido, Cognito bloquea la redirección.
- Si la API recibe token directo de Google, lo rechaza.
- Si falta Google client secret, el proveedor no debe considerarse listo.

## Datos de entrada

- Acción de inicio de sesión con Google.
- Google OAuth client ID.
- Google OAuth client secret.
- Callback autorizado.
- Dominio de Cognito.

## Datos de salida

- Token de Cognito.
- Identidad autenticada.
- Redirección a pantalla protegida.
- Respuesta válida de `/v1/autenticación/me`.

## Dependencias

- OAuth client creado en Google Cloud.
- Redirect URI de Cognito registrado en Google Cloud.
- `#24` configurado.
- `#25` configurado.

## Criterios de aceptación

1. Dado un visitante, cuando presiona `Inicio de sesión with Google`, entonces se redirige al Hosted UI de Cognito.
2. Dada una cuenta Google válida, cuando completa el inicio de sesión, entonces Cognito redirige al callback configurado.
3. Dado un callback exitoso, cuando la aplicación procesa la respuesta, entonces muestra la pantalla protegida.
4. Dado un token de Google directo, cuando se envía a la API, entonces la API lo rechaza.
5. Dado un token de Cognito válido, cuando se consulta `/v1/autenticación/me`, entonces la API devuelve la identidad autenticada.

## Definition of Done

- Google queda configurado como provider de Cognito.
- El flujo de inicio de sesión con Google funciona en dev.
- La API valida token de Cognito.
- La API rechaza tokens directos de Google.
- La historia queda alineada con `#5`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/25
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#25
Current issue: TheMonstersP4/mejengueros-app#26