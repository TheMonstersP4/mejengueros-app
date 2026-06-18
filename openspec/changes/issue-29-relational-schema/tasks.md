# Tasks: Issue 29 Relational MVP Schema

## Review Workload Forecast

| Field | Value |
|---|---|
| Estimated changed lines | 520-760 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR1 docs -> PR2 schema/contract evidence -> PR3 repositories/tests only if later evidence requires them -> PR4 wiring only if needed |
| Delivery strategy | ask-always |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|---|---|---|---|
| 1 | Align issue/docs/OpenSpec traceability | PR1 | Base branch only; no Prisma changes |
| 2 | Add Prisma schema, migration, contract evidence | PR2 | Depends on PR1 traceability decisions; demo seed work is owned by `#54` |
| 3 | Add repositories, mappers, RED-GREEN-REFACTOR tests only if later issue evidence requires them | PR3 | Deferred to `#48`-`#51` unless issue `#29` verification proves they are still necessary |
| 4 | Wire app/API only if central flow still blocked | PR4 | Depends on PR3; skip if repositories satisfy #29 |

## Phase 1: PR Slice 1 - Docs and Traceability
Depends on: none
Verify: manual diff review; `npm --prefix app-backend/api run lint` only if backend docs touch code comments.
Rollback boundary: revert traceability/docs only.

- [x] 1.1 Update `openspec/changes/issue-29-relational-schema/proposal.md`, `design.md`, and `specs/relational-mvp-schema/spec.md` references if needed so Sprint 2 + closed catalog are explicit and consistent.
- [x] 1.2 Prepare follow-up task notes for `docs/specs/issue-29-implementar-esquema-relacional-de-datos.md`, `docs/specs/issue-14-especificar-servicios-parqueo-iluminacion-tipo-de-cesped.md`, and GitHub `#29/#14`; do not edit them in apply until this slice is selected.

## Phase 2: PR Slice 2 - Prisma Foundation
Depends on: Phase 1.
Verify: `npm --prefix app-backend/api run prisma:validate`; `npm --prefix app-backend/api test -- --runInBand`.
Rollback boundary: revert `prisma/schema.prisma`, new migration folder, contract-evidence changes, generated client.

- [x] 2.1 RED: add failing integration/schema tests under `app-backend/api/test/integration/` for multi-role users, scoped services, shared availability, one-hour reservations, cancelled-slot rebooking, confirmed-slot conflict, and one review per reservation.
- [x] 2.2 GREEN: update `app-backend/api/prisma/schema.prisma` with enums/models from design, keeping custom owner-defined services out of Semana 10.
- [x] 2.3 Add migration SQL in `app-backend/api/prisma/migrations/*/migration.sql`, including the raw partial unique index for confirmed reservations and raw DB checks required by design.
- [x] 2.4 Scope decision recorded: issue `#29` stops at schema support for a seeded `ServiceCatalog`; the actual seed data/procedure is deferred to GitHub issue `#54` and is not a closure blocker for `#29`.

## Phase 3: PR Slice 3 - Repositories, Mappers, TDD (Deferred / Conditional)
Depends on: Phase 2 and later proof that issue `#29` still requires persistence adapters beyond schema/migration evidence.
Verify: `npm --prefix app-backend/api run test:integration`; `npm --prefix app-backend/api test -- --runInBand`; `npm --prefix app-backend/api run build`.
Rollback boundary: revert only new module/repository/mapper/test files.

- [ ] 3.1 RED: add repository and mapper tests under `app-backend/api/test/unit/modules/` and `test/integration/` proving Prisma types stay inside infrastructure adapters if follow-up issues `#48`-`#51` still need issue `#29` to carry this work.
- [ ] 3.2 GREEN: create domain contracts and entities under `app-backend/api/src/modules/reservations|complexes|reviews|notifications/**` only if the deferred `#48`-`#51` flow is explicitly pulled back into `#29`.
- [ ] 3.3 GREEN: implement Prisma repositories and mappers under `app-backend/api/src/modules/**/infrastructure/` with conflict translation for confirmed-slot uniqueness only if `#48`-`#51` do not absorb this slice.
- [ ] 3.4 REFACTOR: register providers in new `*.module.ts` files without leaking Prisma models into application DTOs or ports only if Phase 3 is reactivated.

## Phase 4: PR Slice 4 - Application/API Wiring (Conditional)
Depends on: Phase 3 and explicit need confirmation.
Verify: `npm --prefix app-backend/api run test:integration`; `npm --prefix app-backend/api run test:cov -- --runInBand`; `npm --prefix app-backend/api run build`.
Rollback boundary: revert controllers/use-cases/module wiring only.

- [ ] 4.1 Decide whether `app-backend/api/src/app.module.ts` plus new use cases/controllers are required to satisfy #29 acceptance evidence; skip this slice if repository-level proof is enough.
- [ ] 4.2 If required, wire the minimal reservation/review flow endpoints or use cases needed for central-flow verification, without broad CRUD expansion.
