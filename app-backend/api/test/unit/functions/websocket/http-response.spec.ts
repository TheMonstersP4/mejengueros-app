import { jsonResponse } from '@/functions/websocket/http-response';

describe('jsonResponse', () => {
  it('builds an API Gateway JSON response', () => {
    expect(jsonResponse(201, { ok: true })).toEqual({
      statusCode: 201,
      headers: {
        'content-type': 'application/json'
      },
      body: JSON.stringify({ ok: true })
    });
  });
});
