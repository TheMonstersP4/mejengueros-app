# Issue #14: Especificar servicios del complejo y de la cancha

## Título

Registrar servicios aplicables al complejo deportivo o a una cancha específica.

## Nota de re-alcance MVP

Por directriz de la profesora, los servicios ya no deben asumirse únicamente como atributos de una cancha. Algunos servicios pertenecen al `Complejo` completo y otros a una `Cancha` específica.

## Decisión de catálogo para Semana 10

Para Semana 10, el MVP usa un catálogo de servicios cerrado y global.

- El dueño selecciona servicios predefinidos existentes.
- Los servicios se registran según su alcance, `Complejo` o `Cancha`.
- El dueño no puede crear servicios personalizados en el MVP.
- Los servicios personalizados se difieren para post-MVP.

## Objetivo

Permitir que el `dueño` especifique servicios relevantes para el MVP, clasificándolos según apliquen al complejo completo o a una cancha individual.

## Relación con issues coordinados

- `#48` crea el complejo y la primera cancha.
- `#12` define identidad y ubicación del complejo.
- `#13` / `#49` definen disponibilidad por cancha.
- `#15` y `#16` muestran servicios al mejenguero.
- `#29` debe representar servicios con alcance `Complejo` o `Cancha`.

## Historia de usuario

Como `dueño`,
quiero especificar los servicios de mi complejo y sus canchas,
para que el mejenguero entienda qué ofrece el lugar antes de reservar.

## Alcance

- Registrar servicios aplicables al complejo completo.
- Registrar servicios aplicables a una cancha específica.
- Mostrar servicios relevantes en catálogo/detalle.
- Mantener un catálogo cerrado simple de servicios para el MVP.

## Fuera de alcance

- Pagos, promociones o inventario operativo avanzado.
- Administración global del catálogo de servicios.
- Servicios personalizados ilimitados.
- Estado `borrador` u incorporación por bloques.

## Reglas de negocio

1. Cafetería, parqueo general u otros servicios comunes pueden asociarse al `Complejo`.
2. Tipo de césped, iluminación específica u otros atributos particulares pueden asociarse a la `Cancha`.
3. Un servicio debe indicar claramente su alcance: `Complejo` o `Cancha`.
4. Los servicios deben ayudar al mejenguero a decidir búsqueda, visualización y reserva.
5. El sistema no debe exigir servicios que no aporten valor al MVP de semana 10.
6. El dueño no puede crear servicios personalizados durante el MVP.

## Criterios de aceptación

1. Dado un complejo, cuando el dueño registra un servicio común, entonces el servicio queda asociado al complejo usando un servicio predefinido del catálogo global.
2. Dada una cancha, cuando el dueño registra un servicio específico de esa cancha, entonces el servicio queda asociado a la cancha usando un servicio predefinido del catálogo global.
3. Dado un mejenguero viendo el detalle, cuando consulta servicios, entonces puede distinguir servicios generales del complejo y servicios de la cancha si aplica.
4. Dado el alcance MVP, cuando se revisa el spec, entonces queda claro que servicios no introducen pagos ni admin global avanzada.
5. Dado el alcance de Semana 10, cuando el dueño intenta crear un servicio nuevo, entonces esa capacidad queda fuera del MVP y diferida para post-MVP.

## Definition of Done

- Existe al menos una prueba unitaria asociada al comportamiento principal del issue, en la capa que corresponda según la implementación.
- El spec define alcance de servicios por complejo/cancha.
- El spec queda alineado con el modelo `Complejo` -> `Cancha`.
- El spec elimina dependencia de `borrador`.
- El spec deja explícito que el catálogo es cerrado y global para Semana 10.
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/13
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#13
Current issue: TheMonstersP4/mejengueros-app#14
