# Apply Progress: issue-29-relational-schema

## Change Summary

- Delivery mode: chained PR slices, stacked-to-main approved.
- Current slice applied in this batch: local Docker migration validation follow-up under `app-backend/api`.
- Overall status: ready for verify - issue `#29` now has schema/migration/contract-test evidence plus disposable Docker-backed PostgreSQL migration execution evidence.

## Completed Slices

### Slice 1 - Docs and Traceability

1. Posted Spanish alignment comment on GitHub issue `#29` documenting the Semana 10 service decision.
2. Posted Spanish clarification comment on GitHub issue `#14` confirming closed/global predefined services for MVP.
3. Updated local Spanish specs in `docs/specs/` to remove Sprint drift and align service scope/source-of-truth wording.
4. Marked Phase 1 Slice 1 tasks complete in `openspec/changes/issue-29-relational-schema/tasks.md`.

### Slice 2 - Prisma Foundation

1. Updated `app-backend/api/prisma/schema.prisma` with Semana 10 relational enums and models: `UserRole`, `Complex`, `Court`, `ServiceCatalog`, `ComplexService`, `CourtService`, `CourtAvailability`, `CourtAvailabilityDay`, `Reservation`, `Notification`, and `Review`.
2. Preserved existing `User` identity fields and `ImageUpload` compatibility while extending `User` with multi-role, reservation, complex-owner, and notification relations.
3. Added migration SQL at `app-backend/api/prisma/migrations/20260617000100_add_relational_mvp_schema/migration.sql` including:
   - PostgreSQL enums and new tables.
   - Partial unique confirmed-slot index on `Reservation(courtId, startsAt)`.
   - Check constraints for one-hour reservations, rating range, and availability start/end ordering.
4. Added schema/migration contract coverage at `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` plus the text-loading helper `app-backend/api/src/shared/infrastructure/database/prisma-relational-schema.contract.ts`.
5. Marked Slice 2 tasks `2.1`, `2.2`, and `2.3` complete in `openspec/changes/issue-29-relational-schema/tasks.md`.

### Slice 2 - Test/CI Remediation Follow-up

1. Updated `app-backend/api/jest.config.cjs` so the primary `npm test -- --runInBand` path now executes `test/integration/prisma-relational-schema.contract.spec.ts`, which is the same command used by `.github/workflows/deploy.yml`.
2. Added `app-backend/api/scripts/ensure-no-focused-tests.cjs` and wired it into `lint`, `pretest`, `pretest:integration`, `pretest:cov`, and `pretest:e2e` so `it.only` / `describe.only` / `fit` / `fdescribe` fail before CI or local Jest execution continues.
3. Added focused-test and CI coverage regression tests under `app-backend/api/test/unit/config/` and `app-backend/api/test/unit/scripts/`.
4. Refactored `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` to use regex/whitespace-tolerant helpers from `app-backend/api/src/shared/infrastructure/database/prisma-relational-schema.contract.ts` instead of spacing-sensitive exact substrings.

## Final Scope Decision After Verification

- Issue `#29` closes on schema support for `ServiceCatalog`, `ComplexService`, `CourtService`, and the related relational constraints/tests; it does **not** own the demo seed data/procedure.
- GitHub issue `#54` owns the seeded `ServiceCatalog` data/procedure and any future seed convention execution path.
- Phase 3 repositories/mappers are now treated as conditional follow-up work for issues `#48`-`#51` unless a later verification pass proves issue `#29` still needs them.
- Live PostgreSQL migration execution is now satisfied through the disposable local Docker harness under `app-backend/api/docker/`.

## Slice 2 - Local Migration Validation Environment Follow-up

1. Added `app-backend/api/docker/docker-compose.migration-validation.yml` with a single disposable PostgreSQL 16 service dedicated to local Prisma migration validation.
2. Added `app-backend/api/docker/README.md` plus the versioned placeholder template `app-backend/api/docker/migration-validation.env.example`; local setup now copies that template to ignored `migration-validation.env.local` before running Prisma commands.
3. Added minimal backend helper scripts in `app-backend/api/package.json`: `docker:migration-db:up`, `docker:migration-db:down`, and `docker:migration-db:reset`.
4. Executed the relational Prisma migrations against the disposable Docker PostgreSQL instance to close the earlier live-environment blocker.

## Cumulative Task Status

| Task | Status | Notes |
|---|---|---|
| 1.1 | Done | Sprint 2 + closed catalog wording aligned in OpenSpec/docs. |
| 1.2 | Done | Follow-up notes prepared through docs/GitHub traceability updates. |
| 2.1 | Done | Contract tests cover schema/migration behavior without requiring a live PostgreSQL instance. |
| 2.2 | Done | Prisma schema updated for relational MVP foundation. |
| 2.3 | Done | Migration SQL added with partial unique/check constraints. |
| 2.4 | Done (re-scoped) | Issue `#29` only needs schema support for a seeded `ServiceCatalog`; demo seed data/procedure is deferred to `#54`. |
| Slice 2 follow-up | Done | Primary Jest/CI path now covers the Prisma contract spec; focused-test guard and less brittle assertions are in place. |
| Slice 2 migration validation env | Done | Local disposable Docker PostgreSQL harness added under `app-backend/api/docker/`, and Prisma migrations were executed against it. |
| 3.1-3.4 | Deferred / conditional | Follow-up repository/mapper work belongs to `#48`-`#51` unless later proof pulls it back into `#29`. |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 2.1 | `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` | Integration/schema contract | N/A (new file) | ✅ Targeted test written first and failed on missing contract helper (`TS2307`) | ✅ Targeted integration run passed `7/7`; full integration suite passed `14/14` | ✅ 7 contract scenarios covering schema and migration text; live DB execution explicitly deferred | ✅ Added small reusable schema/migration text loader; no further refactor needed |
| 2.2 | N/A - schema foundation | Structural/validation | ✅ `npm test -- --runInBand` baseline 152/152 | ❌ Strict-TDD limitation documented before implementation | ✅ `prisma validate`, `prisma generate`, post-change Jest 152/152 | ➖ Structural schema slice | ➖ None |
| 2.3 | N/A - migration SQL | Structural/inspection | ✅ `npm test -- --runInBand` baseline 152/152 | ❌ Strict-TDD limitation documented before implementation | ✅ Migration SQL written and inspected; schema validation/generation passed | ➖ Structural SQL slice | ➖ None |
| 2.4 | Not implemented by design | N/A | N/A | ➖ OpenSpec cleanup only; task re-scoped after verification | ✅ Task is now satisfied by recorded ownership handoff to `#54` | ➖ No code change required in `#29` | ➖ None |

## Follow-up TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| Slice 2 CI coverage | `app-backend/api/test/unit/config/testing-quality-gates.spec.ts` | Unit/config | ✅ Baseline contract integration `7/7`; baseline lint passed | ✅ Added failing assertions for missing primary-Jest coverage and absent focused-test guard wiring | ✅ Targeted unit run passed `2/2`; primary Jest contract-path run passed `7/7` | ✅ Verified all lint/Jest entrypoints include the guard and contract spec | ➖ Structural config follow-up |
| Slice 2 focused-test guard | `app-backend/api/test/unit/scripts/ensure-no-focused-tests.spec.ts` | Unit/script | ✅ Baseline `npm test` and targeted contract tests passed before edits | ✅ Added failing tests for a missing guard module and matcher behavior | ✅ Targeted unit run passed `2/2`; lint now executes the guard cleanly | ✅ Positive and negative cases cover `.only` and alias variants | ✅ Kept the guard dependency-free with exported pure helpers |
| Slice 2 contract helper hardening | `app-backend/api/test/unit/shared/infrastructure/database/prisma-relational-schema.contract-helper.spec.ts` and `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` | Unit + integration | ✅ Baseline contract integration `7/7` | ✅ New helper-export tests failed with `TS2305`; integration follow-up exposed the array-type regex gap | ✅ Helper unit tests passed `2/2`; contract spec passed `7/7` in both primary and integration paths | ✅ Covered scalar fields, list fields, and SQL whitespace normalization | ✅ Replaced brittle exact-spacing assertions with helper-driven regex checks |
| Slice 2 migration validation env | N/A - Docker/PostgreSQL harness | Structural/live validation | ✅ Prior verify pass documented the blocker clearly | ❌ Strict-TDD limitation documented: local Docker environment and docs were added to enable safe live migration execution rather than from a failing automated test | ✅ `docker compose up`, `prisma validate`, `prisma generate`, and `prisma migrate deploy` succeeded against the disposable local PostgreSQL instance | ✅ Followed with `prisma migrate status` to confirm the database is up to date | ➖ No additional refactor required |

## Verification

### Files and artifacts read before editing

- `openspec/config.yaml`
- `openspec/changes/issue-29-relational-schema/proposal.md`
- `openspec/changes/issue-29-relational-schema/specs/relational-mvp-schema/spec.md`
- `openspec/changes/issue-29-relational-schema/design.md`
- `openspec/changes/issue-29-relational-schema/tasks.md`
- `openspec/changes/issue-29-relational-schema/apply-progress.md`
- Previous Engram apply-progress topic `sdd/issue-29-relational-schema/apply-progress`
- `app-backend/api/prisma/schema.prisma`
- Existing migration SQL files under `app-backend/api/prisma/migrations/`
- `app-backend/api/package.json`

### Commands run from `app-backend/api`

1. `npm run test:integration -- --runInBand test/integration/prisma-relational-schema.contract.spec.ts` - RED failed with `TS2307` for missing `@/shared/infrastructure/database/prisma-relational-schema.contract`, then GREEN passed `1/1`, then after triangulation passed `7/7`.
2. `npm run test:integration -- --runInBand` - passed `2/2` suites, `14/14` tests.
3. `npm test -- --runInBand` - passed `38/38` suites, `152/152` tests.
4. `npm run prisma:validate` - passed.
5. `npm run prisma:generate` - passed.
6. `npm test -- --runInBand test/unit/config/testing-quality-gates.spec.ts test/unit/scripts/ensure-no-focused-tests.spec.ts test/unit/shared/infrastructure/database/prisma-relational-schema.contract-helper.spec.ts` - RED failed first on missing guard/helper/config coverage, then GREEN passed `3/3` suites and `6/6` tests.
7. `npm test -- --runInBand test/integration/prisma-relational-schema.contract.spec.ts` - passed `1/1` suite and `7/7` tests via the primary Jest/CI path.
8. `npm test -- --runInBand` - passed `42/42` suites and `165/165` tests, now including the Prisma contract spec and focused-test regression tests.
9. `npm run test:integration -- --runInBand` - passed `2/2` suites and `14/14` tests.
10. `npm run lint` - passed with the focused-test guard executing first.
11. `npm run build` - passed.
12. `npm run docker:migration-db:up` - passed; local PostgreSQL container became healthy.
13. `Get-Content .\docker\migration-validation.env.local | ...; npm run prisma:validate` - passed after loading `DATABASE_URL` from the ignored local env file.
14. `Get-Content .\docker\migration-validation.env.local | ...; npm run prisma:generate` - passed after loading `DATABASE_URL` from the ignored local env file.
15. `Get-Content .\docker\migration-validation.env.local | ...; npx prisma migrate deploy` - passed; all migrations applied to the disposable database.
16. `Get-Content .\docker\migration-validation.env.local | ...; npx prisma migrate status` - passed; Prisma reported the database is up to date.

### Migration execution result

- A disposable Docker PostgreSQL harness now exists under `app-backend/api/docker/` for local-only migration validation.
- The existing Prisma migration set was executed successfully against that disposable database, removing the prior environment blocker for live PostgreSQL migration evidence.

## Files Changed

| File | Action | Notes |
|---|---|---|
| `docs/specs/issue-29-implementar-esquema-relacional-de-datos.md` | Modified in Slice 1 | Corrected Sprint 2 wording and documented closed/global `ServiceCatalog`, `ComplexService`, `CourtService`, and no custom owner services in Semana 10. |
| `docs/specs/issue-14-especificar-servicios-parqueo-iluminacion-tipo-de-cesped.md` | Modified in Slice 1 | Clarified closed/global predefined services and no owner-created custom services in MVP. |
| `app-backend/api/prisma/schema.prisma` | Modified in Slice 2 | Added relational MVP models, enums, relations, indexes, and compatibility-preserving `User` extension. |
| `app-backend/api/prisma/migrations/20260617000100_add_relational_mvp_schema/migration.sql` | Created in Slice 2 | Added relational schema DDL, partial unique confirmed-slot index, and DB checks. |
| `app-backend/api/src/shared/infrastructure/database/prisma-relational-schema.contract.ts` | Created in Slice 2 follow-up | Reads Prisma schema/migration text for contract-style verification without a live DB. |
| `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` | Created in Slice 2 follow-up | Covers multi-role, closed catalog, shared availability, reservation ownership/status, partial unique rebooking semantics, review constraints, and one-hour duration checks. |
| `app-backend/api/package.json` | Modified in Slice 2 remediation + follow-up | Added the focused-test guard wiring earlier and now also includes minimal Docker helper scripts for migration validation. |
| `app-backend/api/docker/docker-compose.migration-validation.yml` | Created in Slice 2 follow-up | Defines a single disposable PostgreSQL service for local Prisma migration validation only. |
| `app-backend/api/docker/migration-validation.env.example` | Created in Slice 2 follow-up | Provides a versioned placeholder-only template for the ignored local env file used by Docker and Prisma. |
| `app-backend/api/docker/README.md` | Created in Slice 2 follow-up | Explains the copy-to-local-env flow, Docker commands, and Prisma migration steps for disposable local validation only. |
| `app-backend/api/jest.config.cjs` | Modified in Slice 2 remediation | Included the Prisma relational contract spec in the primary Jest command used by CI. |
| `app-backend/api/scripts/ensure-no-focused-tests.cjs` | Created in Slice 2 remediation | Blocks `.only` and focused Jest aliases before lint or tests continue. |
| `app-backend/api/test/unit/config/testing-quality-gates.spec.ts` | Created in Slice 2 remediation | Locks primary Jest coverage and focused-test guard wiring in package scripts/config. |
| `app-backend/api/test/unit/scripts/ensure-no-focused-tests.spec.ts` | Created in Slice 2 remediation | Verifies the focused-test guard detects and ignores the right patterns. |
| `app-backend/api/test/unit/shared/infrastructure/database/prisma-relational-schema.contract-helper.spec.ts` | Created in Slice 2 remediation | Verifies whitespace-tolerant Prisma field and SQL fragment helper behavior. |
| `openspec/changes/issue-29-relational-schema/proposal.md` | Modified in cleanup | Clarified `#29` schema scope versus `#54` seed ownership and conditional repository follow-up. |
| `openspec/changes/issue-29-relational-schema/design.md` | Modified in cleanup | Removed seed work from `#29` PR2 split and documented `#54` dependency. |
| `openspec/changes/issue-29-relational-schema/tasks.md` | Modified in cleanup | Re-scoped task `2.4`, marked Phase 3 as deferred/conditional, and updated workload split wording. |
| `openspec/changes/issue-29-relational-schema/apply-progress.md` | Modified in cleanup | Merged cumulative apply progress with final post-verify scope decisions and migration blocker status. |

## GitHub Traceability

| Issue | Action | Reference |
|---|---|---|
| `#29` | Added service decision comment | https://github.com/TheMonstersP4/mejengueros-app/issues/29#issuecomment-4736724000 |
| `#14` | Added service clarification comment | https://github.com/TheMonstersP4/mejengueros-app/issues/14#issuecomment-4736724041 |

## Rollback Boundary

Revert only:

- `app-backend/api/src/shared/infrastructure/database/prisma-relational-schema.contract.ts`
- `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts`
- `app-backend/api/package.json`
- `app-backend/api/docker/docker-compose.migration-validation.yml`
- `app-backend/api/docker/migration-validation.env.example`
- `app-backend/api/docker/README.md`
- `app-backend/api/jest.config.cjs`
- `app-backend/api/scripts/ensure-no-focused-tests.cjs`
- `app-backend/api/test/unit/config/testing-quality-gates.spec.ts`
- `app-backend/api/test/unit/scripts/ensure-no-focused-tests.spec.ts`
- `app-backend/api/test/unit/shared/infrastructure/database/prisma-relational-schema.contract-helper.spec.ts`
- `app-backend/api/prisma/schema.prisma`
- `app-backend/api/prisma/migrations/20260617000100_add_relational_mvp_schema/migration.sql`
- `openspec/changes/issue-29-relational-schema/tasks.md`
- `openspec/changes/issue-29-relational-schema/apply-progress.md`

## Remaining Work for Issue #29

- Keep using the local disposable Docker harness when future Prisma migration changes need a safe validation target.

## Deferred Follow-up Outside Issue #29

- Issue `#54`: add/demo the actual `ServiceCatalog` seed data/procedure and any supporting seed convention.
- Issues `#48`-`#51`: own repositories, mappers, and any related application/API wiring unless later verification proves issue `#29` still needs part of that work.

## Notes

- GitHub issues remain the business source of truth.
- Reservation statuses stay limited to `CONFIRMED`, `CANCELLED`, and `COMPLETED`; no `PENDING` was introduced in the schema.
- `Review` derives user/court context through `Reservation` only, avoiding duplicated review foreign keys.
- This repository now has a disposable PostgreSQL migration harness under `app-backend/api/docker/` for local-only migration execution evidence.
- `prisma generate` can collide if two backend test commands run concurrently in separate shells because the generated client output directory is mutated in place; CI remains safe because its quality steps run sequentially.
