# Issue #23: Registrar cuenta de usuario manualmente con correo y contraseña


## Título

Registrar cuenta de usuario manualmente con correo y contraseña.

## Objetivo

Permitir que un visitante cree una cuenta en Mejengueros usando correo y contraseña, apoyándose en Cognito para no implementar almacenamiento propio de credenciales.

## Relación con issues coordinados

- `#23` forma parte de `#5 Seguridad y Autenticación`.
- `#24` debe dejar Cognito configurado.
- `#25` debe dejar callbacks y variables listas.
- `#29` permitirá sincronizar el perfil local del usuario cuando exista base de datos.

## Historia de usuario

Como visitante,
quiero registrarme con correo y contraseña,
para acceder a Mejengueros aunque no use Google o Microsoft.

## Alcance

- Permitir registro manual desde Cognito.
- Solicitar correo y contraseña.
- Validar formato de correo.
- Validar política mínima de contraseña.
- Confirmar o informar el estado del registro.
- Permitir que la API reconozca al usuario mediante token de Cognito.

## Fuera de alcance

- Guardar contraseñas en la base de datos propia.
- Crear un sistema de autenticación manual fuera de Cognito.
- Sincronizar todos los datos del usuario local si `#29` no está implementado.
- Definir roles administrativos.

## Reglas de negocio

1. El registro manual debe ser administrado por Cognito.
2. La contraseña nunca debe llegar a persistencia propia de Mejengueros.
3. El correo debe ser único dentro del mecanismo de identidad configurado.
4. La API debe validar la sesión usando tokens de Cognito.
5. Los errores deben mostrarse de forma segura y comprensible.

## Lifecycle conceptual

1. El visitante abre la pantalla de registro.
2. Ingresa correo y contraseña.
3. Cognito valida y crea la cuenta.
4. El usuario confirma la cuenta si la configuración lo requiere.
5. El usuario inicia sesión.
6. La API reconoce su identidad mediante Cognito.

## Flujo principal

1. El usuario selecciona registro manual.
2. El sistema solicita correo, contraseña y confirmación.
3. El usuario envía los datos.
4. Cognito crea la cuenta.
5. El sistema muestra confirmación o paso de verificación.
6. El usuario inicia sesión.
7. El sistema permite entrar a la pantalla protegida.

## Casos alternos/validaciones

- Si el correo no tiene formato válido, el sistema rechaza el registro.
- Si la contraseña no cumple la política, el sistema informa la regla incumplida.
- Si el correo ya existe, el sistema muestra un error seguro.
- Si Cognito requiere confirmación, el usuario no debe considerarse activo hasta completar ese paso.

## Datos de entrada

- Correo electrónico.
- Contraseña.
- Confirmación de contraseña.
- Nombre visible opcional.
- Aceptación de términos, si aplica.

## Datos de salida

- Usuario creado en Cognito.
- Estado de confirmación de cuenta.
- Mensaje de éxito o error.
- Token de Cognito después de inicio de sesión exitoso.

## Dependencias

- `#24` Cognito configurado.
- `#25` callbacks y variables configuradas.
- `#29` para persistir perfil local completo.

## Criterios de aceptación

1. Dado un visitante con correo válido y contraseña válida, cuando completa el registro, entonces Cognito crea la cuenta.
2. Dado un correo ya registrado, cuando intenta registrarse de nuevo, entonces se muestra un error claro sin exponer información sensible.
3. Dada una contraseña que no cumple la política, cuando intenta registrarse, entonces se informa la regla incumplida.
4. Dado un usuario registrado, cuando confirma su cuenta si aplica, entonces puede iniciar sesión.
5. Dado un usuario autenticado, cuando la API recibe su token de Cognito, entonces responde su identidad en `/v1/autenticación/me`.

## Definition of Done

- El registro manual queda delegado a Cognito.
- El sistema no almacena contraseñas propias.
- La API reconoce tokens de usuarios registrados manualmente.
- Los errores principales están contemplados.
- La historia queda alineada con `#5`.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/22
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#22
Current issue: TheMonstersP4/mejengueros-app#23