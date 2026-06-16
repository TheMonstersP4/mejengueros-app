# Issue #32: Implementar las pruebas unitarias de backend

## Estado

Cerrada como `not planned`.

## Decisión de alcance

Esta issue no se implementará como una historia transversal separada.

Las pruebas unitarias de backend quedan absorbidas por el `Definition of Done` de cada issue implementable que toque backend NestJS, API, dominio, caso de uso, validación, filtro, interceptor, persistencia o integración backend relevante.

## Rationale

Mantener una issue global de pruebas backend podía generar tres problemas:

1. duplicar esfuerzo frente a las pruebas exigidas en cada historia implementable;
2. mover la validación fuera del contexto funcional que debe probarse;
3. dificultar la revisión de si una historia específica quedó realmente terminada.

Por eso, cada historia implementable debe declarar y entregar su propia prueba unitaria mínima en la capa backend cuando corresponda.

## Impacto

- No debe estimarse esta issue.
- No debe contarse como trabajo implementable independiente.
- No reemplaza el DoD de ninguna historia.
- Las pruebas backend transversales específicas deben vivir en la issue técnica o funcional que introduce el comportamiento correspondiente.

## Trazabilidad de migración
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/31
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#31
Current issue: TheMonstersP4/mejengueros-app#32