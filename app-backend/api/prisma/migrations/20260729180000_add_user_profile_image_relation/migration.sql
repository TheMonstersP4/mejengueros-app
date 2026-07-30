ALTER TABLE "mejengueros_dev"."User"
ADD COLUMN "profileImageUploadId" TEXT;

CREATE UNIQUE INDEX "User_profileImageUploadId_key"
ON "mejengueros_dev"."User"("profileImageUploadId");

ALTER TABLE "mejengueros_dev"."User"
ADD CONSTRAINT "User_profileImageUploadId_fkey"
FOREIGN KEY ("profileImageUploadId")
REFERENCES "mejengueros_dev"."ImageUpload"("id")
ON DELETE SET NULL
ON UPDATE CASCADE;
