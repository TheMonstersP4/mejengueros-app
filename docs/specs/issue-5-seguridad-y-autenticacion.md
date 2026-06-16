# Issue #5: Seguridad y Autenticación

## Tipo

Épica contenedora.

## Objetivo

Agrupar las capacidades de autenticación y seguridad del MVP, usando Amazon Cognito como proveedor central de identidad y coordinando registro manual, configuración base, callbacks y social login.

## Alcance como contenedor

Esta épica organiza las historias implementables de autenticación. No representa por sí misma una tarea técnica única ni debe duplicar la estimación de sus subissues.

## Subissues esperadas

- `#23` Registrar cuenta de usuario manualmente con correo y contraseña.
- `#24` Configuración base de AWS y Cognito.
- `#25` Callbacks, dominios y variables de autenticación.
- `#26` Iniciar sesión de forma rápida mediante Social Login con Google.
- `#27` Iniciar sesión de forma rápida mediante Social Login con Outlook/Microsoft.

## Decisiones de coordinación

- Cognito es el proveedor central de identidad del sistema.
- Google y Microsoft funcionan como proveedores federados dentro de Cognito.
- La API debe validar tokens emitidos por Cognito, no tokens directos de Google o Microsoft.
- Secretos, credenciales OAuth y configuración sensible deben mantenerse fuera del repositorio.
- La sincronización completa del perfil local se coordina con `#29`.

## Criterio de cierre

La épica se considera completa cuando sus subissues implementables estén completadas o explícitamente descartadas del alcance MVP.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/4
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#4
Current issue: TheMonstersP4/mejengueros-app#5