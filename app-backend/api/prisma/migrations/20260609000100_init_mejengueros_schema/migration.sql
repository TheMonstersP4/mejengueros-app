CREATE SCHEMA IF NOT EXISTS "mejengueros_dev";

CREATE TABLE "mejengueros_dev"."User" (
    "id" TEXT NOT NULL,
    "cognitoSub" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "name" TEXT,
    "pictureUrl" TEXT,
    "provider" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "User_cognitoSub_key" ON "mejengueros_dev"."User"("cognitoSub");

CREATE UNIQUE INDEX "User_email_key" ON "mejengueros_dev"."User"("email");
