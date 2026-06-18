# Relational MVP Schema Specification

## Purpose

Define the Semana 10 relational MVP data behavior before Prisma implementation.

## Requirements

### Requirement: Source-of-Truth Alignment

The system specification MUST treat GitHub issues `#29` and `#14` as the business source of truth, and SHALL preserve the Sprint 2 correction and closed-catalog decision in this change artifact before implementation starts.

#### Scenario: Drift is corrected before schema work
- GIVEN Sprint wording or service scope differs across artifacts
- WHEN this spec is used for design or implementation planning
- THEN Sprint 2 and the closed-catalog decision are the authoritative inputs

### Requirement: Closed Service Catalog

The schema MUST support a seeded global `ServiceCatalog` for Semana 10. Owners MUST NOT define custom services in this MVP.

#### Scenario: Owner selects only seeded services
- GIVEN a complex owner configures services
- WHEN services are persisted
- THEN each selected service comes from `ServiceCatalog` only

### Requirement: Scoped Service Associations

The schema MUST associate services to complexes through `ComplexService` and to courts through `CourtService`. A service assignment SHALL respect its declared scope.

#### Scenario: Service is linked at the correct level
- GIVEN a seeded service with complex or court scope
- WHEN the owner attaches that service
- THEN the relation is stored only through the matching join table

### Requirement: Non-Exclusive User Roles

The schema MUST allow one user to hold multiple roles, including OWNER and PLAYER, without duplicating the user identity.

#### Scenario: Same user acts as owner and player
- GIVEN one registered user
- WHEN that user enables owner capabilities and makes a reservation as player
- THEN both roles remain active for the same user record

### Requirement: Shared Court Availability Window

The schema MUST represent court availability as selected days plus one shared start time and one shared end time. The MVP MUST NOT model different hours per day.

#### Scenario: Availability uses one time range for many days
- GIVEN a court available on Monday and Wednesday
- WHEN availability is stored
- THEN both days share the same start and end time range

### Requirement: Court Reservation Ownership

Each reservation MUST belong to exactly one `User` and one `Court`, SHALL NOT belong directly to a `Complex`, and MUST represent exactly one hour.

#### Scenario: Reservation is created for one court hour
- GIVEN a player books a court
- WHEN the reservation is persisted
- THEN it references one user, one court, and a one-hour slot only

### Requirement: Confirmed Reservation Exclusivity

The schema MUST prevent double booking for confirmed reservations on the same court hour. Cancelled reservations MUST NOT block rebooking of that slot.

#### Scenario: Cancelled slot becomes bookable again
- GIVEN a reservation for a court hour was cancelled
- WHEN another player books the same court hour later
- THEN the new reservation is allowed if no confirmed reservation exists

### Requirement: Reservation Follow-Up

The schema MUST support a post-reservation notification and a review/rating that is tied to a completed reservation.

#### Scenario: Review is created from completion
- GIVEN a reservation reached completed status
- WHEN the player receives a follow-up and submits feedback
- THEN the notification and review reference that reservation

### Requirement: MVP Readiness Boundaries

The schema SHOULD include lifecycle fields such as `status` and `deletedAt` where needed for Semana 13 readiness, and MUST NOT model favorites, images, metrics, questionnaires, or special one-star logic in this MVP change.

#### Scenario: Future readiness does not expand scope
- GIVEN the MVP schema is prepared for future extension
- WHEN entities need lifecycle tracking
- THEN only generic readiness fields are added without introducing out-of-scope features

### Requirement: Prisma Containment

Prisma-generated types and persistence details MUST remain inside infrastructure adapters and SHALL NOT leak into domain or application contracts.

#### Scenario: Domain stays persistence-agnostic
- GIVEN repositories or mappers expose data beyond infrastructure
- WHEN upper layers consume relational data
- THEN they depend on domain contracts rather than Prisma types
