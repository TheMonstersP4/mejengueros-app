CREATE TABLE "mejengueros_dev"."ImageUpload" (
    "id" TEXT NOT NULL,
    "ownerSub" TEXT NOT NULL,
    "ownerEmail" TEXT,
    "ownerName" TEXT,
    "ownerPictureUrl" TEXT,
    "ownerProvider" TEXT,
    "purpose" TEXT NOT NULL,
    "objectKey" TEXT NOT NULL,
    "contentType" TEXT NOT NULL,
    "sizeBytes" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ImageUpload_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "ImageUpload_objectKey_key" ON "mejengueros_dev"."ImageUpload"("objectKey");

CREATE INDEX "ImageUpload_ownerSub_idx" ON "mejengueros_dev"."ImageUpload"("ownerSub");

CREATE INDEX "ImageUpload_createdAt_idx" ON "mejengueros_dev"."ImageUpload"("createdAt");
