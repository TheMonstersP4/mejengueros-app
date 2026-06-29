ALTER TABLE "mejengueros_dev"."Complex"
ADD COLUMN "isPublished" BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE "mejengueros_dev"."Court"
ADD COLUMN "isPublished" BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX "Complex_isPublished_idx" ON "mejengueros_dev"."Complex"("isPublished");

CREATE INDEX "Court_isPublished_idx" ON "mejengueros_dev"."Court"("isPublished");
