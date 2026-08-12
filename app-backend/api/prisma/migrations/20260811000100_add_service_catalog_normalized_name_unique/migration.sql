-- ServiceCatalog."name" is unique as an exact string, so rows that differ only by accents, casing,
-- or surrounding whitespace ("Sintetico" / "Sintético") can coexist. Every client renders those
-- variants through the same normalized label, so each extra row shows up as a duplicate entry in
-- the court and complex service lists. Collapse the existing variants onto one row per normalized
-- name and keep the catalog unique by that normalized name from now on.
--
-- The normalized form is lower(btrim(name)) with the Spanish accents folded away. Only immutable
-- built-ins are used so the expression is valid inside a unique index.

-- Canonical row per normalized name: the oldest row wins so the entry that has been referenced the
-- longest is the one that survives.
CREATE TEMP TABLE "service_catalog_duplicate" AS
SELECT
    duplicate."id" AS "duplicateId",
    canonical."id" AS "canonicalId"
FROM "mejengueros_dev"."ServiceCatalog" duplicate
INNER JOIN LATERAL (
    SELECT candidate."id"
    FROM "mejengueros_dev"."ServiceCatalog" candidate
    WHERE translate(lower(btrim(candidate."name")), 'áàäâéèëêíìïîóòöôúùüû', 'aaaaeeeeiiiioooouuuu')
        = translate(lower(btrim(duplicate."name")), 'áàäâéèëêíìïîóòöôúùüû', 'aaaaeeeeiiiioooouuuu')
    ORDER BY candidate."createdAt" ASC, candidate."id" ASC
    LIMIT 1
) canonical ON TRUE
WHERE canonical."id" <> duplicate."id";

-- Drop only the associations that already exist on the canonical row, then repoint the rest so no
-- court or complex loses a service it actually offers.
DELETE FROM "mejengueros_dev"."CourtService" "courtService"
USING "service_catalog_duplicate" duplicate
WHERE "courtService"."serviceCatalogId" = duplicate."duplicateId"
  AND EXISTS (
      SELECT 1
      FROM "mejengueros_dev"."CourtService" canonical
      WHERE canonical."courtId" = "courtService"."courtId"
        AND canonical."serviceCatalogId" = duplicate."canonicalId"
  );

UPDATE "mejengueros_dev"."CourtService" "courtService"
SET "serviceCatalogId" = duplicate."canonicalId"
FROM "service_catalog_duplicate" duplicate
WHERE "courtService"."serviceCatalogId" = duplicate."duplicateId";

DELETE FROM "mejengueros_dev"."ComplexService" "complexService"
USING "service_catalog_duplicate" duplicate
WHERE "complexService"."serviceCatalogId" = duplicate."duplicateId"
  AND EXISTS (
      SELECT 1
      FROM "mejengueros_dev"."ComplexService" canonical
      WHERE canonical."complexId" = "complexService"."complexId"
        AND canonical."serviceCatalogId" = duplicate."canonicalId"
  );

UPDATE "mejengueros_dev"."ComplexService" "complexService"
SET "serviceCatalogId" = duplicate."canonicalId"
FROM "service_catalog_duplicate" duplicate
WHERE "complexService"."serviceCatalogId" = duplicate."duplicateId";

DELETE FROM "mejengueros_dev"."ServiceCatalog" "serviceCatalog"
USING "service_catalog_duplicate" duplicate
WHERE "serviceCatalog"."id" = duplicate."duplicateId";

DROP TABLE "service_catalog_duplicate";

CREATE UNIQUE INDEX "ServiceCatalog_name_key_normalized"
    ON "mejengueros_dev"."ServiceCatalog"
    ((translate(lower(btrim("name")), 'áàäâéèëêíìïîóòöôúùüû', 'aaaaeeeeiiiioooouuuu')));
