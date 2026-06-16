# Issue #36: Editar información de perfil (Apodo, posición favorita)

## Estado de alcance

`alcance-descartado` / `post-MVP` para semana 10.

## Motivo

La profesora indicó que la información del usuario debe simplificarse para el MVP. Campos como `apodo` y `posición favorita` no aportan al flujo crítico de negocio: búsqueda, reserva, reseña post-reserva ni gestión de complejo/cancha.

## Trazabilidad

- `#36` se conserva para no perder la historia original de personalización de perfil.
- Para semana 10, el usuario sólo necesita datos mínimos para autenticación, rol, propiedad de reservas y autoría de reseñas.
- Si en una versión futura el producto necesita identidad social o perfil deportivo, esta historia puede reabrirse o rediseñarse.

## Alcance descartado para MVP semana 10

- Editar apodo.
- Editar posición favorita.
- Validar formato de apodo.
- Mantener catálogo de posiciones favoritas.
- Usar estos campos como parte del flujo de reserva o reseña.

## Modelo mínimo recomendado para usuario MVP

- Identificador de autenticación.
- Email.
- Nombre visible básico si el registro manual ya lo captura.
- Rol: `mejenguero` o `dueño`.
- Estado operativo de cuenta si ya existe en autenticación.
- Relaciones con reservas y reseñas.

## Fuera de alcance

- Biografía.
- Número de teléfono, salvo que el equipo lo justifique como dato operativo mínimo.
- Redes sociales.
- Foto de perfil.
- Apodo único global.
- Posición favorita.

## Definition of Done

- La issue queda marcada como alcance descartado/post-MVP.
- Las specs vigentes no dependen de apodo ni posición favorita.
- La trazabilidad queda preservada para una versión futura.

---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/35
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#35
Current issue: TheMonstersP4/mejengueros-app#36
