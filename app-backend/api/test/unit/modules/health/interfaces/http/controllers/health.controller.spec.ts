import { HealthController } from '@/modules/health/interfaces/http/controllers/health.controller';

describe('HealthController', () => {
  beforeEach(() => {
    jest.useFakeTimers().setSystemTime(new Date('2026-01-02T03:04:05.000Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('returns service health with a timestamp', () => {
    expect(new HealthController().check()).toEqual({
      status: 'ok',
      timestamp: '2026-01-02T03:04:05.000Z'
    });
  });
});
