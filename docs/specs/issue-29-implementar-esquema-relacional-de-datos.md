# Issue #29: Implementar esquema relacional mínimo MVP

## Título

Implementar esquema relacional mínimo para el MVP de reservas.

## Nota de re-alcance MVP

Para Sprint 3, `#29` se limita al esquema mínimo necesario para demostrar el flujo principal del MVP: usuarios, complejo, canchas, servicios, disponibilidad, reservas, notificaciones y reseñas/rating. Imágenes, favoritos, cuestionarios avanzados, métricas estructuradas y reglas especiales de `1 estrella` quedan post-MVP salvo decisión explícita posterior.

## Objetivo

Definir e implementar el modelo relacional mínimo usando Prisma y PostgreSQL para soportar el flujo DB-connected de semana 10: autenticación local, complejo/cancha, servicios, horarios/slots, reservas, notificaciones post-reserva, reseñas y rating visible.

## Relación con issues coordinados

- `#53` prepara ambiente, Base de datos local/dev, migraciones y seed baseline.
- `#23` aporta identidad de usuario mediante autenticación manual/Cognito.
- `#48` crea complejo y primera cancha.
- `#49` define disponibilidad y slots de 1 hora.
- `#50` crea reservas y exige no doble booking.
- `#51` crea notificaciones post-reserva.
- `#17` y `#18` crean reseñas/rating desde reserva finalizada.
- `#54` y `#52` consumen este modelo para seed/lista de verificación demo.

## Historia de usuario

Como equipo de desarrollo,
queremos implementar un esquema relacional mínimo y coherente,
para que el flujo central del MVP funcione con datos persistidos en base de datos.

## Alcance

- Definir modelos principales en `schema.prisma`.
- Crear migración inicial mínima.
- Generar Prisma Client.
- Mantener tipos de Prisma dentro de infraestructura.
- Definir relaciones, claves únicas e índices principales.
- Preparar repositorios o mappers iniciales sólo para el flujo central.

## Fuera de alcance

- Favoritos.
- Imágenes y metadata de imágenes.
- Imagen/evidencia obligatoria para reseña de `1 estrella`.
- Cuestionarios avanzados o métricas estructuradas.
- Social login, landing pages, perfil extendido o panel admin global.
- CRUD completo para todos los módulos.
- RDS productivo o infraestructura pagada no requerida para el demo.

## Reglas de negocio

1. `User` debe relacionarse con la identidad de autenticación mediante `cognitoSub` o identificador equivalente acordado.
2. Prisma no debe filtrarse a dominio ni aplicación.
3. Las entidades deben tener timestamps básicos.
4. `Complejo` es la entidad raíz de oferta deportiva.
5. Un `Complejo` contiene una o más `Canchas`.
6. Los servicios pueden asociarse a `Complejo` o a `Cancha` según su naturaleza.
7. Una `Cancha` tiene reglas de disponibilidad que generan slots exactos de 1 hora.
8. `Reserva` se asocia a usuario, cancha, `startsAt`, `endsAt` y estado básico.
9. El modelo debe impedir más de una reserva activa para la misma cancha y hora de inicio.
10. `Notification` debe permitir notificación post-reserva para reseñar.
11. `Review` debe vincularse a una reserva finalizada, usuario y cancha.
12. Una reserva no debe tener más de una reseña asociada.
13. El rating de la reseña debe permitir valores enteros de `1` a `5`.

## Flujo principal

1. Equipo define entidades y relaciones mínimas.
2. Se actualiza `schema.prisma`.
3. Se ejecuta validación/generación de Prisma.
4. Se crea migración inicial.
5. Se implementan mappers o repositorios iniciales necesarios para el flujo central.
6. Se agregan pruebas unitarias básicas donde corresponda.

## Casos alternos/validaciones

- Si no hay base de datos configurada, los comandos dependientes de Prisma deben fallar de forma clara o quedar documentados como bloqueo de ambiente.
- Si una relación requerida falta, la migración no debe considerarse completa.
- Si una restricción única de reserva falta, se debe agregar antes de cerrar la historia.
- Si un mapper expone tipos de Prisma al dominio, debe corregirse.

## Datos de entrada

- Identificador de usuario autenticado.
- Datos de complejo y cancha.
- Servicios de complejo/cancha.
- Reglas de disponibilidad y slots de 1 hora.
- Reservas, notificaciones y reseñas post-reserva.

## Datos de salida

- `schema.prisma` actualizado.
- Migración inicial mínima.
- Prisma Client generado.
- Modelos listos para repositorios/mappers del flujo central.
- Documentación breve de relaciones y restricciones críticas.

## Dependencias

- `#53` para ambiente, Base de datos local/dev y comandos base.
- PostgreSQL o base compatible disponible.
- Prisma configurado según estándar del proyecto.
- Variables de entorno para migraciones.
- `#23` para identidad de usuarios.

## Criterios de aceptación

1. Dado un entorno con base de datos configurada, cuando se ejecuta la migración, entonces se crean las tablas mínimas del MVP sin errores.
2. Dado un usuario autenticado, cuando se sincroniza su perfil local, entonces se crea o actualiza un registro en `User`.
3. Dado un dueño, cuando crea un complejo y una cancha, entonces el modelo permite asociar la cancha al complejo correcto.
4. Dado un complejo o cancha, cuando se registran servicios, entonces quedan asociados al alcance correcto.
5. Dada una cancha con disponibilidad, cuando se generan o consultan slots, entonces el modelo permite representar slots exactos de 1 hora.
6. Dada una reserva, cuando se guarda, entonces queda asociada a usuario, cancha, inicio y fin exactos de 1 hora.
7. Dado un slot ya reservado para una cancha, cuando otra persona intenta reservar el mismo inicio, entonces el modelo permite rechazar la duplicidad.
8. Dada una reserva finalizada, cuando el usuario crea una reseña, entonces la reseña queda vinculada a esa reserva y no puede duplicarse para la misma reserva.
9. Dada una reseña con rating, cuando se guarda, entonces el rating queda persistido como entero de `1` a `5`.
10. Dado el alcance de Sprint 3, cuando se revisa el esquema, entonces no incluye favoritos, imágenes, cuestionarios avanzados ni métricas estructuradas.

## Definition of Done

- El esquema compila con `prisma generate`.
- La migración puede ejecutarse en un entorno limpio.
- Las relaciones principales tienen claves foráneas, índices y restricciones únicas.
- `User` queda vinculado a la identidad de autenticación.
- La unicidad de reserva por cancha/hora queda protegida en el modelo.
- La unicidad de reseña por reserva queda protegida en el modelo.
- Prisma se mantiene en infraestructura.
- Existen pruebas unitarias para mappers o repositorios iniciales donde corresponda.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/28
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#28
Current issue: TheMonstersP4/mejengueros-app#29
