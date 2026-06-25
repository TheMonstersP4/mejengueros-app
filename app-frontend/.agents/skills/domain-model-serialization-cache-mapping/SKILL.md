---
name: domain-model-serialization-cache-mapping
description: "Trigger: domain models, DTOs, cache mapping. Decide when API/domain/local models may be shared or split."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding entities, API responses, SQLDelight schemas, or mapping logic.

## Hard Rules

- Make model-boundary coupling explicit; do not accidentally use one model everywhere.
- Small apps may use one serializable domain model for API and UI if the API shape is stable.
- Split DTO, domain, and local entities when API shape, persistence shape, or UI semantics diverge.
- Keep SQLDelight row flattening/reconstruction in local datasource mapping.
- Avoid mutable derived fields in domain models; prefer computed properties or mapper output.

## Decision Gates

| Condition                                | Model strategy                                  |
| ---------------------------------------- | ----------------------------------------------- |
| API shape equals domain and app is small | Shared serializable domain model acceptable     |
| API naming/nullability differs           | DTO + mapper                                    |
| Cache flattens nested data               | Local row mapper                                |
| UI needs derived values                  | Domain computed property or presentation mapper |

## Execution Steps

1. Decide model boundaries before writing schema/API code.
2. Add serializers only where network decoding needs them.
3. Keep cache insert/select mapping explicit.
4. Test nullable and missing-field cases.

## Output Contract

Report chosen model strategy and mapping points.

## References

- `shared/src/commonMain/kotlin/io/github/kevinah95/spacex/domain/entity/Entity.kt`
- `shared/src/commonMain/sqldelight/io/github/kevinah95/spacex/data/local/AppDatabase.sq`
- `assets/dto-domain-local-mapping-example.kt`
