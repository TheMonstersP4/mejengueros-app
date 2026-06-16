# Issue #46: Ver y regenerar código QR de validación para reseñas de cancha

## Estado de alcance

`alcance-descartado` / `QR-obsoleto`.

## Motivo

Esta historia queda obsoleta por el re-alcance MVP definido después de la retroalimentación de la profesora. La validación para publicar reseñas ya no se hará mediante QR ni código de 6 dígitos, sino mediante una reserva finalizada asociada al usuario y a la cancha.

## Trazabilidad

- `#46` se conserva para no perder la decisión histórica sobre validación por QR/código.
- El flujo vigente de reseña debe resolverse en `#17` mediante reserva finalizada.
- La notificación post-reserva debe resolverse en `#51`.
- La persistencia vigente debe resolverse en `#29` con reservas, notificaciones y reseñas asociadas a reserva.
- No se debe implementar gestión owner-facing de código/QR para el MVP semana 10.

## Alcance descartado

- Mostrar código vigente de 6 dígitos al dueño.
- Mostrar QR equivalente al código.
- Regenerar código/QR.
- Invalidar códigos anteriores.
- Usar código/QR como validación previa para reseñar.

## Reemplazo funcional

El reemplazo funcional es:

1. El `mejenguero` reserva un slot de cancha según `#50`.
2. La reserva concluye.
3. El sistema genera una notificación según `#51`.
4. El usuario abre la notificación y crea una reseña según `#17`.
5. El servidor valida que la reserva pertenece al usuario, ya finalizó y no tiene reseña previa.

## Definition of Done

- La issue conserva trazabilidad histórica.
- La issue queda marcada como descartada/obsoleta.
- Las historias vigentes no dependen de QR ni código.

---
Current issue: TheMonstersP4/mejengueros-app#46
