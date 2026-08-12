const ACCENTED_CHARACTERS = 'áàäâéèëêíìïîóòöôúùüû';
const UNACCENTED_CHARACTERS = 'aaaaeeeeiiiioooouuuu';

const UNACCENT_BY_CHARACTER = new Map<string, string>(
  Array.from(ACCENTED_CHARACTERS, (character, index) => [
    character,
    UNACCENTED_CHARACTERS[index] as string
  ])
);

/**
 * Canonical identity of a service catalog name.
 *
 * Service names are shown through an accent-normalized label, so rows whose names differ only by
 * accents, casing, or surrounding whitespace ("Sintetico", "Sintético", " SINTETICO ") describe the
 * same service and must never be listed as separate entries. This key mirrors the normalization
 * enforced by the `ServiceCatalog_name_key_normalized` unique index so application code and the
 * database agree on what counts as the same service.
 */
export function serviceCatalogNameKey(name: string): string {
  return Array.from(name.trim().toLowerCase(), (character) => {
    return UNACCENT_BY_CHARACTER.get(character) ?? character;
  }).join('');
}
