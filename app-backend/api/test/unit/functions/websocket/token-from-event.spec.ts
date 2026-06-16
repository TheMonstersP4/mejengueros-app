import { extractTokenFromEvent } from '@/functions/websocket/token-from-event';

describe('extractTokenFromEvent', () => {
  it.each([
    [{ headers: { authorization: 'Bearer header-token' } }, 'header-token'],
    [{ headers: { Authorization: 'raw-header-token' } }, 'raw-header-token'],
    [
      { queryStringParameters: { token: 'Bearer query-token' } },
      'query-token'
    ],
    [
      { queryStringParameters: { authorization: 'query-authorization' } },
      'query-authorization'
    ],
    [
      { queryStringParameters: { Authorization: 'query-Authorization' } },
      'query-Authorization'
    ],
    [{}, undefined]
  ])('extracts token from %#', (event, expected) => {
    expect(extractTokenFromEvent(event as never)).toBe(expected);
  });

  it('prefers headers over query string values', () => {
    expect(
      extractTokenFromEvent({
        headers: { authorization: 'Bearer header-token' },
        queryStringParameters: { token: 'query-token' }
      } as never)
    ).toBe('header-token');
  });
});
