CREATE TABLE "mejengueros_dev"."UserIdentity" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "providerSubject" TEXT NOT NULL,
    "emailAtLogin" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "UserIdentity_pkey" PRIMARY KEY ("id")
);

INSERT INTO "mejengueros_dev"."UserIdentity" (
    "id",
    "userId",
    "provider",
    "providerSubject",
    "emailAtLogin",
    "createdAt",
    "updatedAt"
)
SELECT
    'legacy-' || md5("id" || ':' || "cognitoSub"),
    "id",
    COALESCE(NULLIF("provider", ''), 'Cognito'),
    "cognitoSub",
    "email",
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM "mejengueros_dev"."User";

CREATE UNIQUE INDEX "UserIdentity_provider_providerSubject_key"
    ON "mejengueros_dev"."UserIdentity"("provider", "providerSubject");

CREATE INDEX "UserIdentity_userId_idx"
    ON "mejengueros_dev"."UserIdentity"("userId");

CREATE INDEX "UserIdentity_provider_idx"
    ON "mejengueros_dev"."UserIdentity"("provider");

ALTER TABLE "mejengueros_dev"."UserIdentity"
    ADD CONSTRAINT "UserIdentity_userId_fkey"
    FOREIGN KEY ("userId") REFERENCES "mejengueros_dev"."User"("id")
    ON DELETE CASCADE ON UPDATE CASCADE;

DROP INDEX "mejengueros_dev"."User_cognitoSub_key";

ALTER TABLE "mejengueros_dev"."User"
    DROP COLUMN "cognitoSub",
    DROP COLUMN "provider";
