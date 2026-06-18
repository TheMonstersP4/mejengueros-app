CREATE TYPE "mejengueros_dev"."UserStatus" AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE "mejengueros_dev"."UserRoleKind" AS ENUM ('PLAYER', 'OWNER', 'ADMIN');
CREATE TYPE "mejengueros_dev"."ServiceScope" AS ENUM ('COMPLEX', 'COURT');
CREATE TYPE "mejengueros_dev"."Weekday" AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');
CREATE TYPE "mejengueros_dev"."ComplexStatus" AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE "mejengueros_dev"."CourtStatus" AS ENUM ('ACTIVE', 'INACTIVE');
CREATE TYPE "mejengueros_dev"."ReservationStatus" AS ENUM ('CONFIRMED', 'CANCELLED', 'COMPLETED');
CREATE TYPE "mejengueros_dev"."NotificationType" AS ENUM ('REVIEW_PROMPT');
CREATE TYPE "mejengueros_dev"."NotificationStatus" AS ENUM ('PENDING', 'SENT', 'FAILED', 'READ');

ALTER TABLE "mejengueros_dev"."User"
    ADD COLUMN "status" "mejengueros_dev"."UserStatus" NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE "mejengueros_dev"."UserRole" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "role" "mejengueros_dev"."UserRoleKind" NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "UserRole_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."Complex" (
    "id" TEXT NOT NULL,
    "ownerId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "address" TEXT NOT NULL,
    "status" "mejengueros_dev"."ComplexStatus" NOT NULL DEFAULT 'ACTIVE',
    "deletedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Complex_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."Court" (
    "id" TEXT NOT NULL,
    "complexId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "status" "mejengueros_dev"."CourtStatus" NOT NULL DEFAULT 'ACTIVE',
    "deletedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Court_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."ServiceCatalog" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "scope" "mejengueros_dev"."ServiceScope" NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ServiceCatalog_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."ComplexService" (
    "id" TEXT NOT NULL,
    "complexId" TEXT NOT NULL,
    "serviceCatalogId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ComplexService_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."CourtService" (
    "id" TEXT NOT NULL,
    "courtId" TEXT NOT NULL,
    "serviceCatalogId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "CourtService_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."CourtAvailability" (
    "id" TEXT NOT NULL,
    "courtId" TEXT NOT NULL,
    "startTime" TIME(0) NOT NULL,
    "endTime" TIME(0) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "CourtAvailability_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "CourtAvailability_time_window_check" CHECK ("startTime" < "endTime")
);

CREATE TABLE "mejengueros_dev"."CourtAvailabilityDay" (
    "id" TEXT NOT NULL,
    "availabilityId" TEXT NOT NULL,
    "day" "mejengueros_dev"."Weekday" NOT NULL,

    CONSTRAINT "CourtAvailabilityDay_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."Reservation" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "courtId" TEXT NOT NULL,
    "startsAt" TIMESTAMP(3) NOT NULL,
    "endsAt" TIMESTAMP(3) NOT NULL,
    "status" "mejengueros_dev"."ReservationStatus" NOT NULL DEFAULT 'CONFIRMED',
    "cancelledAt" TIMESTAMP(3),
    "completedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Reservation_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "Reservation_one_hour_duration_check" CHECK ("endsAt" = "startsAt" + INTERVAL '1 hour')
);

CREATE TABLE "mejengueros_dev"."Notification" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "reservationId" TEXT NOT NULL,
    "type" "mejengueros_dev"."NotificationType" NOT NULL,
    "status" "mejengueros_dev"."NotificationStatus" NOT NULL DEFAULT 'PENDING',
    "sentAt" TIMESTAMP(3),
    "readAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Notification_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."Review" (
    "id" TEXT NOT NULL,
    "reservationId" TEXT NOT NULL,
    "rating" INTEGER NOT NULL,
    "comment" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Review_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "Review_rating_range_check" CHECK ("rating" BETWEEN 1 AND 5)
);

CREATE UNIQUE INDEX "UserRole_userId_role_key" ON "mejengueros_dev"."UserRole"("userId", "role");
CREATE INDEX "UserRole_role_idx" ON "mejengueros_dev"."UserRole"("role");

CREATE INDEX "Complex_ownerId_idx" ON "mejengueros_dev"."Complex"("ownerId");
CREATE INDEX "Complex_status_idx" ON "mejengueros_dev"."Complex"("status");

CREATE INDEX "Court_complexId_idx" ON "mejengueros_dev"."Court"("complexId");
CREATE INDEX "Court_complexId_status_idx" ON "mejengueros_dev"."Court"("complexId", "status");

CREATE UNIQUE INDEX "ServiceCatalog_name_key" ON "mejengueros_dev"."ServiceCatalog"("name");
CREATE INDEX "ServiceCatalog_scope_isActive_idx" ON "mejengueros_dev"."ServiceCatalog"("scope", "isActive");

CREATE UNIQUE INDEX "ComplexService_complexId_serviceCatalogId_key" ON "mejengueros_dev"."ComplexService"("complexId", "serviceCatalogId");
CREATE INDEX "ComplexService_serviceCatalogId_idx" ON "mejengueros_dev"."ComplexService"("serviceCatalogId");

CREATE UNIQUE INDEX "CourtService_courtId_serviceCatalogId_key" ON "mejengueros_dev"."CourtService"("courtId", "serviceCatalogId");
CREATE INDEX "CourtService_serviceCatalogId_idx" ON "mejengueros_dev"."CourtService"("serviceCatalogId");

CREATE UNIQUE INDEX "CourtAvailability_courtId_key" ON "mejengueros_dev"."CourtAvailability"("courtId");

CREATE UNIQUE INDEX "CourtAvailabilityDay_availabilityId_day_key" ON "mejengueros_dev"."CourtAvailabilityDay"("availabilityId", "day");
CREATE INDEX "CourtAvailabilityDay_day_idx" ON "mejengueros_dev"."CourtAvailabilityDay"("day");

CREATE INDEX "Reservation_userId_idx" ON "mejengueros_dev"."Reservation"("userId");
CREATE INDEX "Reservation_courtId_idx" ON "mejengueros_dev"."Reservation"("courtId");
CREATE INDEX "Reservation_startsAt_idx" ON "mejengueros_dev"."Reservation"("startsAt");
CREATE INDEX "Reservation_status_idx" ON "mejengueros_dev"."Reservation"("status");
CREATE INDEX "Reservation_courtId_startsAt_idx" ON "mejengueros_dev"."Reservation"("courtId", "startsAt");
CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key"
    ON "mejengueros_dev"."Reservation" ("courtId", "startsAt")
    WHERE "status" = 'CONFIRMED';

CREATE INDEX "Notification_userId_status_idx" ON "mejengueros_dev"."Notification"("userId", "status");
CREATE INDEX "Notification_reservationId_idx" ON "mejengueros_dev"."Notification"("reservationId");

CREATE UNIQUE INDEX "Review_reservationId_key" ON "mejengueros_dev"."Review"("reservationId");

ALTER TABLE "mejengueros_dev"."UserRole"
    ADD CONSTRAINT "UserRole_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "mejengueros_dev"."User"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Complex"
    ADD CONSTRAINT "Complex_ownerId_fkey"
    FOREIGN KEY ("ownerId") REFERENCES "mejengueros_dev"."User"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Court"
    ADD CONSTRAINT "Court_complexId_fkey"
    FOREIGN KEY ("complexId") REFERENCES "mejengueros_dev"."Complex"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."ComplexService"
    ADD CONSTRAINT "ComplexService_complexId_fkey"
    FOREIGN KEY ("complexId") REFERENCES "mejengueros_dev"."Complex"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."ComplexService"
    ADD CONSTRAINT "ComplexService_serviceCatalogId_fkey"
    FOREIGN KEY ("serviceCatalogId") REFERENCES "mejengueros_dev"."ServiceCatalog"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."CourtService"
    ADD CONSTRAINT "CourtService_courtId_fkey"
    FOREIGN KEY ("courtId") REFERENCES "mejengueros_dev"."Court"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."CourtService"
    ADD CONSTRAINT "CourtService_serviceCatalogId_fkey"
    FOREIGN KEY ("serviceCatalogId") REFERENCES "mejengueros_dev"."ServiceCatalog"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."CourtAvailability"
    ADD CONSTRAINT "CourtAvailability_courtId_fkey"
    FOREIGN KEY ("courtId") REFERENCES "mejengueros_dev"."Court"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."CourtAvailabilityDay"
    ADD CONSTRAINT "CourtAvailabilityDay_availabilityId_fkey"
    FOREIGN KEY ("availabilityId") REFERENCES "mejengueros_dev"."CourtAvailability"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Reservation"
    ADD CONSTRAINT "Reservation_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "mejengueros_dev"."User"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Reservation"
    ADD CONSTRAINT "Reservation_courtId_fkey"
    FOREIGN KEY ("courtId") REFERENCES "mejengueros_dev"."Court"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Notification"
    ADD CONSTRAINT "Notification_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "mejengueros_dev"."User"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Notification"
    ADD CONSTRAINT "Notification_reservationId_fkey"
    FOREIGN KEY ("reservationId") REFERENCES "mejengueros_dev"."Reservation"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Review"
    ADD CONSTRAINT "Review_reservationId_fkey"
    FOREIGN KEY ("reservationId") REFERENCES "mejengueros_dev"."Reservation"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;
