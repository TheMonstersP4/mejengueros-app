# Issue #49: Configurar disponibilidad reservable de cancha

## Título

Configurar días y rango horario de una cancha para generar slots reservables de 1 hora.

## Objetivo

Definir la disponibilidad operativa de una cancha mediante selección de días y un rango horario único, para que el sistema genere slots de reserva exactos de 1 hora.

## Historia de usuario

Como `dueño`,
quiero seleccionar los días y el rango horario en que una cancha está disponible,
para que el sistema genere automáticamente los slots que los mejengueros pueden reservar.

## Alcance

- Seleccionar uno o más días de la semana.
- Definir un rango horario de inicio y fin para la cancha.
- Convertir el rango en slots exactos de 1 hora.
- Interpretar días no seleccionados como cerrados/no disponibles.
- Usar la disponibilidad como base para reservas.

## Fuera de alcance

- Intervalos distintos de 1 hora.
- Horarios diferentes por día en el MVP.
- Excepciones manuales de días cerrados.
- Manejo de feriados por la aplicación.
- Precios, pagos o promociones por horario.

## Reglas de negocio

1. El dueño no puede definir slots con duración distinta a 1 hora.
2. La configuración se expresa como selección de días + rango horario.
3. El sistema genera slots `[inicio, inicio + 1 hora)` hasta antes del fin del rango.
4. Si un día no está seleccionado, el sistema lo interpreta como cerrado.
5. La aplicación no gestiona feriados; son responsabilidad operativa del dueño.
6. Los slots se generan por cancha, no por complejo completo.
7. Un slot sólo puede reservarse si pertenece a la disponibilidad vigente de la cancha.

## Criterios de aceptación

1. Dado un dueño que selecciona lunes, martes y miércoles de 06:00 a 20:00, cuando guarda la disponibilidad, entonces el sistema puede generar slots de 1 hora para esos días y ese rango.
2. Dado un día no seleccionado, cuando un mejenguero consulta disponibilidad, entonces no existen slots reservables para ese día.
3. Dado un intento de configurar duración distinta de 1 hora, cuando el dueño guarda, entonces el sistema rechaza la configuración.
4. Dado el alcance MVP, cuando se revisa el spec, entonces queda claro que no existen feriados ni excepciones especiales administradas por la aplicación.

## Definition of Done

- La disponibilidad deja de ser sólo informativa.
- El spec define slots exactos de 1 hora.
- El spec conecta horarios con reservas.
- Queda explícito que días no seleccionados equivalen a cerrado.

---
Current issue: TheMonstersP4/mejengueros-app#49
