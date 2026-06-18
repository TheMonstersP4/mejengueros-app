# Design: Issue 29 Relational MVP Schema

## Technical Approach

Add the Semana 10 relational foundation in Prisma/PostgreSQL without expanding product scope. The design keeps Prisma inside backend infrastructure, uses the existing `mejengueros_dev` schema, preserves the closed `ServiceCatalog`, models non-exclusive roles through a join model, and enforces confirmed reservation slot exclusivity with PostgreSQL partial uniqueness rather than Prisma `@@unique`.

## Architecture Decisions

| Decision | Choice | Tradeoff / Rationale |
|---|---|---|
| Table naming | Keep existing Prisma-style PascalCase model names and camelCase columns in `mejengueros_dev`. | Existing migrations already create quoted `"User"`, `"ImageUpload"`, `"cognitoSub"`. Switching to snake_case now would be a separate naming migration. |
| Roles | `UserRole` join table with `role: UserRoleKind` and `@@unique([userId, role])`. | Allows OWNER and PLAYER on one `User` without duplicating identity. |
| Services | Seeded `ServiceCatalog` plus `ComplexService` and `CourtService`. | Enforces closed catalog and keeps complex/court scope explicit. |
| Availability | `CourtAvailability` owns one `startTime`/`endTime`; `CourtAvailabilityDay` stores selected weekdays. | Matches one shared window across many days; per-day hours remain out of scope. |
| Reservations | `Reservation` belongs to `User` and `Court`, not `Complex`; duration is derived/enforced as one hour. | Complex is reachable through `Court`; direct `complexId` would duplicate ownership. |
| Reviews | `Review` references one completed `Reservation`; user/court are derived through reservation. | Avoids drift and supports one review per reservation. |

## Proposed Models / Enums

Enums: `UserRoleKind { PLAYER OWNER ADMIN }`, `ServiceScope { COMPLEX COURT }`, `Weekday { MONDAY ... SUNDAY }`, `ComplexStatus { ACTIVE INACTIVE }`, `CourtStatus { ACTIVE INACTIVE }`, `ReservationStatus { CONFIRMED CANCELLED COMPLETED }`, `NotificationType { REVIEW_PROMPT }`, `NotificationStatus { PENDING SENT FAILED READ }`.

Models: extend `User` with `roles`, `ownedComplexes`, `reservations`. Add `UserRole(id,userId,role,createdAt)`, `Complex(id,ownerId,name,address,status,deletedAt,createdAt,updatedAt)`, `Court(id,complexId,name,status,deletedAt,createdAt,updatedAt)`, `ServiceCatalog(id,name,scope,isActive,createdAt,updatedAt)`, `ComplexService(id,complexId,serviceCatalogId,createdAt)`, `CourtService(id,courtId,serviceCatalogId,createdAt)`, `CourtAvailability(id,courtId,startTime,endTime,createdAt,updatedAt)`, `CourtAvailabilityDay(id,availabilityId,day)`, `Reservation(id,userId,courtId,startsAt,endsAt,status,cancelledAt,completedAt,createdAt,updatedAt)`, `Notification(id,userId,reservationId,type,status,sentAt,readAt,createdAt,updatedAt)`, `Review(id,reservationId,rating,comment,createdAt,updatedAt)`.

## Relationships and Delete Behavior

- `User -> UserRole`: cascade; roles are identity metadata.
- `User -> Complex` and `User -> Reservation`: restrict hard delete; prefer future soft/lifecycle handling.
- `Complex -> Court`: restrict when reservations exist; otherwise soft delete via `deletedAt`/status.
- `ComplexService`, `CourtService`, `CourtAvailability`, `CourtAvailabilityDay`: cascade from their owner config.
- `Reservation -> Notification` and `Reservation -> Review`: restrict by default for audit; if physical purge is introduced, purge dependents in one repository transaction.

## Indexes and Constraints

- Unique: `UserRole(userId, role)`, `ServiceCatalog(name)`, `ComplexService(complexId, serviceCatalogId)`, `CourtService(courtId, serviceCatalogId)`, `CourtAvailabilityDay(availabilityId, day)`, `Review(reservationId)`.
- Index: owner complexes, courts by complex/status, services by scope/active, reservations by `userId`, `courtId`, `startsAt`, `status`, notifications by user/status.
- Confirmed slot exclusivity raw migration SQL:

```sql
CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key"
ON "mejengueros_dev"."Reservation" ("courtId", "startsAt")
WHERE "status" = 'CONFIRMED';
```

- Add DB check constraints via raw SQL for `Reservation.endsAt = startsAt + interval '1 hour'`, `Review.rating BETWEEN 1 AND 5`, and `CourtAvailability.startTime < endTime` if Prisma cannot express them.

## Validation Split

DB enforces referential integrity, uniqueness, partial confirmed booking exclusivity, required fields, enum values, and simple checks. Application services/repositories enforce service scope before inserting join rows, active catalog selection, reservation availability/day matching, completed-only reviews, notification timing, ownership/authorization, and user-friendly conflict translation from database errors.

## File Changes

| File | Action | Description |
|---|---|---|
| `app-backend/api/prisma/schema.prisma` | Modify later | Add models/enums only after RED tests. |
| `app-backend/api/prisma/migrations/*/migration.sql` | Create later | Include raw partial unique/check constraints. |
| `app-backend/api/src/modules/**` | Modify later | Keep Prisma in infrastructure repositories/mappers. |
| `docs/specs/issue-29-*.md`, `docs/specs/issue-14-*.md`, GitHub `#29/#14` | Update later | Record Sprint 2 correction and closed catalog decision after design approval. |

## Migration / Testing Strategy

Strict TDD: first add failing schema/migration tests or repository integration tests for role multiplicity, service scope, shared availability, one-hour reservations, cancelled-slot rebooking, confirmed-slot conflict, and review uniqueness. Then update Prisma schema, generate client, create migration with raw SQL, run `npm run prisma:validate`, `npm test -- --runInBand`, `npm run test:integration`, `npm run build`, and coverage in verify.

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
400-line budget risk: High

Suggested split: PR1 docs/GitHub alignment; PR2 schema/enums/migration/contract evidence; PR3 repository/mappers and integration tests only if later issue `#29` verification still needs them; PR4 application services/API wiring only if still required. Demo seed data/procedure is not part of `#29` PR2 and is tracked separately by `#54`. Each slice should stay reviewable and independently verifiable.

## Open Questions

- Should hard deletes be completely disallowed for business records in favor of status/`deletedAt`?
