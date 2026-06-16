# Issue #27: Iniciar sesión de forma rápida mediante Social Login con Outlook (Microsoft)


## Título

Iniciar sesión de forma rápida mediante Social Login con Outlook (Microsoft).


## Nota de priorización MVP semana 10

Social login queda post-MVP por directriz de semana 10; mantener trazabilidad, pero priorizar autenticación manual suficiente para reservas.

## Objetivo

Permitir que un usuario ingrese a Mejengueros usando una cuenta Outlook, Hotmail o Microsoft personal mediante Cognito Hosted UI.

## Relación con issues coordinados

- `#27` forma parte de `#5 Seguridad y Autenticación`.
- `#24` y `#25` deben estar listos.
- `#29` permitirá sincronizar datos locales del usuario.

## Historia de usuario

Como visitante,
quiero iniciar sesión con mi cuenta Outlook, Hotmail o Microsoft,
para entrar a Mejengueros sin crear una contraseña nueva.

## Alcance

- Configurar Microsoft como identity provider en Cognito.
- Permitir cuentas personales de Microsoft cuando aplique.
- Redirigir al usuario a Cognito Hosted UI.
- Procesar callback exitoso.
- Usar token emitido por Cognito para consumir la API.

## Fuera de alcance

- Validar tokens directos de Microsoft en la API.
- Administrar contraseñas de Outlook o Hotmail.
- Crear recuperación de acceso de Microsoft desde Mejengueros.
- Definir permisos avanzados de Microsoft Graph.

## Reglas de negocio

1. Microsoft debe operar como proveedor federado dentro de Cognito.
2. La API solo acepta tokens emitidos por Cognito.
3. El App Registration debe permitir el tipo de cuenta acordado.
4. Las cuentas Outlook/Hotmail personales deben funcionar si el proyecto las requiere.
5. El callback debe ser el de Cognito, no uno arbitrario del cliente.

## Lifecycle conceptual

1. Usuario selecciona Microsoft.
2. Cliente redirige a Cognito.
3. Cognito delega autenticación a Microsoft.
4. Microsoft autentica.
5. Cognito emite token.
6. Cliente entra a pantalla protegida.

## Flujo principal

1. Usuario presiona `Inicio de sesión with Microsoft`.
2. Sistema redirige a Cognito Hosted UI.
3. Usuario se autentica con Microsoft.
4. Cognito procesa la identidad.
5. Cognito redirige al callback.
6. Cliente guarda sesión.
7. API responde `/v1/autenticación/me`.

## Casos alternos/validaciones

- Si Microsoft rechaza el inicio de sesión, el usuario vuelve sin sesión.
- Si el tenant/audience no permite cuentas personales, el inicio de sesión con Hotmail/Outlook falla.
- Si la API recibe token directo de Microsoft, lo rechaza.
- Si falta client secret, el proveedor no debe considerarse listo.

## Datos de entrada

- Acción de inicio de sesión con Microsoft.
- Microsoft client ID.
- Microsoft client secret.
- Tenant o audiencia configurada.
- Callback autorizado.
- Dominio de Cognito.

## Datos de salida

- Token de Cognito.
- Identidad autenticada.
- Redirección a pantalla protegida.
- Respuesta válida de `/v1/autenticación/me`.

## Dependencias

- Microsoft Entra App Registration creada.
- Redirect URI de Cognito registrado en Microsoft.
- `#24` configurado.
- `#25` configurado.

## Criterios de aceptación

1. Dado un visitante, cuando presiona `Inicio de sesión with Microsoft`, entonces se redirige al Hosted UI de Cognito.
2. Dada una cuenta Outlook o Hotmail válida, cuando completa el inicio de sesión, entonces Cognito emite una sesión válida.
3. Dado un callback exitoso, cuando la aplicación procesa la respuesta, entonces muestra la pantalla protegida.
4. Dado un token de Microsoft directo, cuando se envía a la API, entonces la API lo rechaza.
5. Dado un token de Cognito válido, cuando se consulta `/v1/autenticación/me`, entonces la API devuelve la identidad autenticada.

## Definition of Done

- Microsoft queda configurado como provider de Cognito.
- El flujo de inicio de sesión con Outlook/Hotmail funciona en dev.
- La API valida token de Cognito.
- La API rechaza tokens directos de Microsoft.
- La historia queda alineada con `#5`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/26
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#26
Current issue: TheMonstersP4/mejengueros-app#27