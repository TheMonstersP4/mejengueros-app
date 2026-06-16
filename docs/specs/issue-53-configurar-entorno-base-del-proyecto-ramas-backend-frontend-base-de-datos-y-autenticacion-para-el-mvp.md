# Issue #53: Configurar entorno base del proyecto, ramas, backend, frontend, base de datos y autenticación para el MVP

## Título

Configurar entorno base del proyecto, ramas, backend, frontend, base de datos y autenticación para el MVP.

## Objetivo

Preparar la base técnica necesaria para que el desarrollo formal del Sprint 3 pueda iniciar sin bloqueos de ambiente, ramas, ejecución local, base de datos, migraciones, seed baseline ni autenticación mínima.

## Historia de usuario

Como equipo de desarrollo,
queremos tener servidor, cliente, base de datos, ramas y configuración de autenticación funcionando localmente,
para iniciar el desarrollo del MVP con una base común y reproducible.

## Alcance

- Verificar que el servidor pueda ejecutarse localmente desde un checkout limpio.
- Verificar que el cliente pueda ejecutarse localmente desde un checkout limpio.
- Configurar o documentar conexión local/desarrollo a base de datos.
- Definir `.env.example` o equivalente sin secretos reales.
- Definir comandos de instalación, ejecución, migración y seed/reinicio inicial.
- Definir branch workflow para el equipo.
- Dejar lista la base para migraciones del modelo MVP de `#29`.
- Cubrir configuración mínima equivalente a `#24` y `#25`: Cognito/autenticación base, callbacks, dominios/variables o su alternativa acordada para el demo.
- Documentar la configuración en README o documento local equivalente.

## Fuera de alcance

- Implementar endpoints funcionales de negocio.
- Implementar pantallas del MVP.
- Implementar el esquema relacional completo más allá del baseline necesario para migraciones.
- Despliegue productivo.
- Social login Google/Microsoft.

## Reglas de negocio / operación

1. Sprint 2 es una excepción de puntaje por directriz de los profesores.
2. Todos los miembros del equipo deben quedar asignados a esta issue para reflejar participación en configuración.
3. La configuración debe permitir que `#23`, `#29`, `#48`, `#49` y demás historias central inicien en Sprint 3 sin bloqueos de ambiente.
4. No se deben versionar secretos reales.
5. La semilla inicial puede ser mínima, pero debe dejar preparado el camino para `#54` y `#52`.

## Criterios de aceptación

1. Dado un integrante del equipo con un checkout limpio, cuando sigue la documentación de configuración, entonces puede levantar servidor y frontend localmente.
2. Dada la configuración de ambiente, cuando se revisan los archivos ejemplo, entonces no contienen secretos reales.
3. Dada la Base de datos local/dev, cuando se ejecutan comandos documentados, entonces migraciones y seed baseline pueden correr o queda documentado el paso pendiente explícito.
4. Dada la estrategia de ramas, cuando un miembro inicia una historia de Sprint 3, entonces sabe qué rama crear y contra qué rama integrar.
5. Dada la autenticación mínima, cuando se inicia `#23`, entonces existen variables/callbacks/configuración base suficientes para implementar registro/inicio de sesión manual.

## Definition of Done

- Backend local documentado y verificable.
- Frontend local documentado y verificable.
- Base de datos local/dev documentada y verificable.
- `.env.example` o equivalente sin secretos.
- Branch workflow acordado.
- Comandos de migración, seed y reset documentados.
- Configuración mínima de autenticación lista para Sprint 3.

---
Current issue: TheMonstersP4/mejengueros-app#53
