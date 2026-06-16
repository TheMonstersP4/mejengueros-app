# Issue #25: Callbacks, dominios y variables de autenticación

## Título

Configurar callbacks, dominios y variables de autenticación.

## Objetivo

Conectar cliente, Cognito y API mediante URLs y variables consistentes para que el inicio de sesión, callback y logout funcionen en local y dev.

## Relación con issues coordinados

- `#25` forma parte de `#5 Seguridad y Autenticación`.
- `#24` crea la base de Cognito que esta historia configura operativamente.
- `#26` y `#27` dependen de callbacks correctos.
- La API depende de estas variables para validar tokens de Cognito.

## Historia de usuario

Como equipo de desarrollo,
queremos configurar callbacks, dominios y variables de autenticación,
para que cliente, Cognito y API compartan el mismo flujo seguro de inicio y cierre de sesión.

## Alcance

- Registrar callback URLs para local y dev.
- Registrar logout URLs para local y dev.
- Documentar variables requeridas por cliente.
- Documentar variables requeridas por API.
- Validar que el callback no quede congelado ni redirija al inicio de sesión por falta de estado.
- Validar logout.

## Fuera de alcance

- Crear nuevos proveedores sociales.
- Rediseñar UI de inicio de sesión.
- Implementar persistencia completa del usuario local.
- Definir autorización por roles.

## Reglas de negocio

1. Cognito solo debe redirigir a URLs permitidas.
2. El cliente debe conocer el dominio de Cognito y App Client ID.
3. La API debe conocer User Pool ID, App Client ID y región AWS.
4. Logout debe limpiar sesión local.
5. Las variables deben diferenciar local y dev.

## Lifecycle conceptual

1. Se definen URLs locales y públicas.
2. Se registran en Cognito.
3. Se configuran variables de cliente.
4. Se configuran variables de API.
5. Se prueba inicio de sesión, callback, token y logout.

## Flujo principal

1. El usuario inicia sesión desde cliente.
2. Cliente redirige a Cognito.
3. Cognito autentica.
4. Cognito vuelve al callback registrado.
5. Cliente procesa token y estado.
6. Cliente llama a la API.
7. API valida token con Cognito.

## Casos alternos/validaciones

- Si la URL de callback no está registrada, Cognito rechaza el flujo.
- Si falta App Client ID, el cliente no debe iniciar el inicio de sesión.
- Si falta User Pool ID o región, la API no debe aceptar tokens.
- Si el estado del callback es inválido, el cliente debe mostrar error seguro.

## Datos de entrada

- URL local del cliente.
- URL pública del cliente.
- Callback de Cognito.
- Logout URLs.
- Cognito User Pool ID.
- Cognito App Client ID.
- AWS región.

## Datos de salida

- Callback URLs registradas.
- Logout URLs registradas.
- Variables de cliente documentadas.
- Variables de API documentadas.
- Inicio de sesión/callback/logout probado.

## Dependencias

- `#24` completado.
- Cliente o POC web disponible.
- API con guard de Cognito disponible.
- Dominio público definido para dev.

## Criterios de aceptación

1. Dado el frontend local, cuando se inicia sesión, entonces Cognito permite volver al callback local configurado.
2. Dado el cliente publicado, cuando se inicia sesión, entonces Cognito permite volver al callback público configurado.
3. Dada una sesión válida, cuando se consulta `/v1/autenticación/me`, entonces la API reconoce el token de Cognito.
4. Dado un usuario que cierra sesión, cuando termina el logout, entonces vuelve a una URL permitida.
5. Dada una URL no registrada, cuando se intenta usar como callback, entonces Cognito la rechaza.

## Definition of Done

- Local y dev tienen callbacks definidos.
- Local y dev tienen logout URLs definidos.
- API y cliente tienen variables documentadas.
- El callback funciona sin quedarse congelado.
- Logout limpia sesión y redirige correctamente.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/24
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#24
Current issue: TheMonstersP4/mejengueros-app#25