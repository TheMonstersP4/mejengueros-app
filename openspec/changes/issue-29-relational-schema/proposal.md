# Proposal: Issue 29 Relational MVP Schema

## Intent

Establish the relational schema foundation for the Semana 10 DB-connected MVP flow: owner creates a complex/court, configures services and availability, players reserve a court, receive review prompts, and ratings are persisted. This also corrects source-of-truth drift before implementation.

## Scope

### In Scope
- Align GitHub issues `#29` and `#14` with local docs/specs, including the `Sprint 2` drift fix in `#29`.
- Document the Semana 10 service decision: fixed global `ServiceCatalog` catalog, no owner-defined services.
- Produce OpenSpec proposal/spec/design/tasks before `schema.prisma`.
- Implement the Prisma models, migration, and schema-level contract evidence required so Semana 10 can support a seeded `ServiceCatalog`.

### Out of Scope
- Custom owner services, favorites, images, metrics/questionnaires, special `1-star` logic.
- Full CRUD/admin, paid infra, frontend UX beyond future API consumption.
- Demo seed data/procedure ownership for `ServiceCatalog`; GitHub issue `#54` owns the actual seed convention and execution path.
- Phase 3 repositories/mappers unless a later verification pass proves they are still required for issue `#29`; those remain deferred to `#48`-`#51`.

## Capabilities

### New Capabilities
- `relational-mvp-schema`: Prisma/PostgreSQL data model for users, roles, complexes, courts, fixed services, availability, reservations, notifications, and reviews.

### Modified Capabilities
- None. No existing OpenSpec capability files exist yet.

## Approach

Start with documentation alignment: comment on GitHub `#29` and `#14`, then update `docs/specs/issue-29-*.md` and `docs/specs/issue-14-*.md`. Then define the delta spec and design for: closed/global `ServiceCatalog` support that future seed work can populate; `ComplexService` and `CourtService` join tables; non-exclusive `UserRole`; `CourtAvailability` plus `CourtAvailabilityDay` for selected days with one shared range; `Reservation` references `Court`; confirmed-only reservation uniqueness with cancelled-slot rebooking; `Review` derives user/court through `Reservation`. The actual seed data/procedure stays out of `#29` and is tracked by `#54`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `docs/specs/issue-29-*.md`, `docs/specs/issue-14-*.md` | Modified | Align local docs to GitHub source of truth and service decision. |
| GitHub issues `#29`, `#14` | Modified | Add clarifying comments before implementation. |
| `openspec/changes/issue-29-relational-schema/` | New | Proposal, future spec/design/tasks. |
| `app-backend/api/prisma/schema.prisma` | Modified later | MVP relational models and constraints. |
| `app-backend/api/src/**` | Modified later if required | Repositories/mappers/tests are deferred unless later evidence shows issue `#29` still needs them. |
| `app-frontend/**`, `infra/**` | No direct change expected | Only downstream consumers/environment affected. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Service scope re-opens during schema work | Medium | Record closed catalog in GitHub, docs, OpenSpec. |
| Reservation uniqueness is hard with status filtering | Medium | Design DB-supported confirmed-only strategy before migration so cancelled reservations do not block rebooking. |
| Review workload exceeds 400 lines | High | Split docs, schema/migration, repository/tests into chained reviewable slices. |

## Rollback Plan

Revert OpenSpec/docs/GitHub wording if scope changes. For implementation slices, revert Prisma migration and repository/test commits before dependent APIs land.

## Dependencies

- User-confirmed Semana 10 service decision.
- `#53` database baseline and `#23` user identity contract.

## Success Criteria

- [ ] GitHub and local docs agree on Sprint 2 scope and closed/global services.
- [ ] Spec/design preserve every listed schema decision before implementation.
- [ ] Prisma schema and migration support Semana 10 flow without out-of-scope models.
- [ ] Seeded `ServiceCatalog` remains a supported schema contract for this change, while the actual seed procedure is handed off to `#54`.
