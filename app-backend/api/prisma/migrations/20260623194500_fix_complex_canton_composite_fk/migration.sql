ALTER TABLE "mejengueros_dev"."Complex"
    DROP CONSTRAINT IF EXISTS "Complex_cantonId_fkey";

CREATE UNIQUE INDEX IF NOT EXISTS "Canton_id_provinceId_key"
    ON "mejengueros_dev"."Canton"("id", "provinceId");

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'Complex_canton_requires_province_check'
          AND conrelid = '"mejengueros_dev"."Complex"'::regclass
    ) THEN
        ALTER TABLE "mejengueros_dev"."Complex"
            ADD CONSTRAINT "Complex_canton_requires_province_check"
            CHECK ("cantonId" IS NULL OR "provinceId" IS NOT NULL);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'Complex_canton_matches_province_fkey'
          AND conrelid = '"mejengueros_dev"."Complex"'::regclass
    ) THEN
        ALTER TABLE "mejengueros_dev"."Complex"
            ADD CONSTRAINT "Complex_canton_matches_province_fkey"
            FOREIGN KEY ("cantonId", "provinceId")
            REFERENCES "mejengueros_dev"."Canton"("id", "provinceId")
            ON DELETE RESTRICT
            ON UPDATE CASCADE;
    END IF;
END $$;
