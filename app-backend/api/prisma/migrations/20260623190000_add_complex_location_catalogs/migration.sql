CREATE TABLE "mejengueros_dev"."Province" (
    "id" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Province_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mejengueros_dev"."Canton" (
    "id" TEXT NOT NULL,
    "provinceId" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Canton_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "mejengueros_dev"."Complex"
    ADD COLUMN "provinceId" TEXT,
    ADD COLUMN "cantonId" TEXT,
    ADD COLUMN "latitude" DOUBLE PRECISION,
    ADD COLUMN "longitude" DOUBLE PRECISION;

CREATE UNIQUE INDEX "Province_code_key" ON "mejengueros_dev"."Province"("code");
CREATE UNIQUE INDEX "Province_name_key" ON "mejengueros_dev"."Province"("name");

CREATE UNIQUE INDEX "Canton_code_key" ON "mejengueros_dev"."Canton"("code");
CREATE UNIQUE INDEX "Canton_id_provinceId_key" ON "mejengueros_dev"."Canton"("id", "provinceId");
CREATE UNIQUE INDEX "Canton_provinceId_name_key" ON "mejengueros_dev"."Canton"("provinceId", "name");
CREATE INDEX "Canton_provinceId_idx" ON "mejengueros_dev"."Canton"("provinceId");

CREATE INDEX "Complex_provinceId_idx" ON "mejengueros_dev"."Complex"("provinceId");
CREATE INDEX "Complex_cantonId_idx" ON "mejengueros_dev"."Complex"("cantonId");
CREATE INDEX "Complex_provinceId_cantonId_idx" ON "mejengueros_dev"."Complex"("provinceId", "cantonId");

ALTER TABLE "mejengueros_dev"."Canton"
    ADD CONSTRAINT "Canton_provinceId_fkey"
    FOREIGN KEY ("provinceId") REFERENCES "mejengueros_dev"."Province"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Complex"
    ADD CONSTRAINT "Complex_provinceId_fkey"
    FOREIGN KEY ("provinceId") REFERENCES "mejengueros_dev"."Province"("id")
    ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "mejengueros_dev"."Complex"
    ADD CONSTRAINT "Complex_canton_requires_province_check"
    CHECK ("cantonId" IS NULL OR "provinceId" IS NOT NULL);

ALTER TABLE "mejengueros_dev"."Complex"
    ADD CONSTRAINT "Complex_canton_matches_province_fkey"
    FOREIGN KEY ("cantonId", "provinceId")
    REFERENCES "mejengueros_dev"."Canton"("id", "provinceId")
    ON DELETE RESTRICT ON UPDATE CASCADE;
