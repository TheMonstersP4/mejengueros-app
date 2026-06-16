# Issue #33: Implementar las pruebas unitarias de frontend

## Estado

Cerrada como `not planned`.

## Decisión de alcance

Esta issue no se implementará como una historia transversal separada.

Las pruebas unitarias de frontend quedan absorbidas por el `Definition of Done` de cada issue implementable que toque Kotlin Multiplatform, UI state, ViewModel, validación frontend, navegación, estados vacíos, errores de UI o lógica compartida relevante.

## Rationale

Mantener una issue global de pruebas frontend podía generar tres problemas:

1. duplicar esfuerzo frente a las pruebas exigidas en cada historia implementable;
2. separar la validación del comportamiento funcional que debe probarse;
3. dificultar la revisión de si una historia específica quedó realmente terminada.

Por eso, cada historia implementable debe declarar y entregar su propia prueba unitaria mínima en la capa frontend/KMP cuando corresponda.

## Impacto

- No debe estimarse esta issue.
- No debe contarse como trabajo implementable independiente.
- No reemplaza el DoD de ninguna historia.
- Las pruebas frontend transversales específicas deben vivir en la issue técnica o funcional que introduce el comportamiento correspondiente.

## Trazabilidad de migración
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/32
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#32
Current issue: TheMonstersP4/mejengueros-app#33