ALTER TABLE "mejengueros_dev"."Court"
ADD COLUMN "imageUploadId" TEXT;

CREATE UNIQUE INDEX "Court_imageUploadId_key"
ON "mejengueros_dev"."Court"("imageUploadId");

ALTER TABLE "mejengueros_dev"."Court"
ADD CONSTRAINT "Court_imageUploadId_fkey"
FOREIGN KEY ("imageUploadId")
REFERENCES "mejengueros_dev"."ImageUpload"("id")
ON DELETE SET NULL
ON UPDATE CASCADE;
