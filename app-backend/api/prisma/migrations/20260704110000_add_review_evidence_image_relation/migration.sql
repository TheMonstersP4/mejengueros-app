ALTER TABLE "mejengueros_dev"."Review"
ADD COLUMN "evidenceImageUploadId" TEXT;

CREATE UNIQUE INDEX "Review_evidenceImageUploadId_key"
ON "mejengueros_dev"."Review"("evidenceImageUploadId");

ALTER TABLE "mejengueros_dev"."Review"
ADD CONSTRAINT "Review_evidenceImageUploadId_fkey"
FOREIGN KEY ("evidenceImageUploadId")
REFERENCES "mejengueros_dev"."ImageUpload"("id")
ON DELETE SET NULL
ON UPDATE CASCADE;
