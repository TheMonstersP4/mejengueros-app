import {
  costaRicaBusinessDayBounds,
  formatCostaRicaBusinessDate
} from '@/shared/domain/time/costa-rica-business-time';

const JULY_UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT = '2026-07-05T05:59:59.000Z';
const JULY_UTC_COSTA_RICA_MIDNIGHT = '2026-07-05T06:00:00.000Z';
const NEW_YEAR_UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT = '2027-01-01T05:59:59.000Z';
const NEW_YEAR_UTC_COSTA_RICA_MIDNIGHT = '2027-01-01T06:00:00.000Z';

describe('costa rica business time', () => {
  it('switches business date exactly at 06:00:00Z', () => {
    expect(formatCostaRicaBusinessDate(new Date(JULY_UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT))).toBe(
      '2026-07-04'
    );
    expect(formatCostaRicaBusinessDate(new Date(JULY_UTC_COSTA_RICA_MIDNIGHT))).toBe(
      '2026-07-05'
    );
  });

  it('keeps month and year rollovers aligned to Costa Rica civil dates', () => {
    expect(
      formatCostaRicaBusinessDate(new Date(NEW_YEAR_UTC_SECOND_BEFORE_COSTA_RICA_MIDNIGHT))
    ).toBe('2026-12-31');
    expect(formatCostaRicaBusinessDate(new Date(NEW_YEAR_UTC_COSTA_RICA_MIDNIGHT))).toBe(
      '2027-01-01'
    );
  });

  it('builds Costa Rica day bounds from 06:00:00Z to the next 06:00:00Z', () => {
    const bounds = costaRicaBusinessDayBounds('2026-07-04');

    expect(bounds.start.toISOString()).toBe('2026-07-04T06:00:00.000Z');
    expect(bounds.end.toISOString()).toBe('2026-07-05T06:00:00.000Z');
  });
});
