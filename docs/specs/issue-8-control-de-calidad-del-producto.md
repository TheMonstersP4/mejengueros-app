# Issue #8: Control de calidad del producto

## Tipo

Issue cerrada / alcance descartado.

## Estado de cierre

Esta issue queda cerrada como `not planned`.

## Decisión de alcance

El control de calidad del producto no se gestionará como una épica transversal separada dentro del MVP.

La decisión vigente del equipo es que la prueba unitaria mínima y la validación correspondiente vivan en el `Definition of Done` de cada issue implementable, en la capa que corresponda según el comportamiento de esa historia:

- servidor NestJS, cuando el comportamiento principal esté en API, dominio, casos de uso, validaciones, filtros o interceptores;
- cliente Kotlin Multiplatform, cuando el comportamiento principal esté en UI state, ViewModel, validación cliente o lógica compartida;
- otra capa pertinente cuando la implementación real lo justifique.

## Subissues descartadas

- `#32` Implementar las pruebas unitarias de backend.
- `#33` Implementar las pruebas unitarias de frontend.

Ambas quedan cerradas porque separar las pruebas en issues transversales podía duplicar esfuerzo, mover validación fuera de cada historia implementable y dejar DoDs individuales sin evidencia clara.

## Criterio de cierre

La issue se considera cerrada porque la responsabilidad de pruebas fue absorbida por las issues implementables del Project.

No debe estimarse ni contarse como trabajo implementable independiente.

## Trazabilidad de migración
---
Migrated from: https://github.com/ShantyCerdasB/mejengueros-app/issues/7
Original repository: ShantyCerdasB/mejengueros-app
Original issue in original repository: ShantyCerdasB/mejengueros-app#7
Current issue: TheMonstersP4/mejengueros-app#8