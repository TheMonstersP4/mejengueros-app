import type { APIGatewayProxyWebsocketEventV2 } from 'aws-lambda';

describe('connect websocket handler', () => {
  const env = {
    awsRegion: 'us-east-1',
    cognitoUserPoolId: 'pool',
    cognitoClientId: 'client',
    cognitoTokenUse: 'id' as const,
    connectionsTableName: 'connections',
    connectionTtlSeconds: 60
  };

  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  async function loadHandler(options?: {
    verify?: jest.Mock;
    store?: jest.Mock;
  }) {
    const verify = options?.verify ?? jest.fn().mockResolvedValue({ sub: 'user' });
    const store = options?.store ?? jest.fn().mockResolvedValue(undefined);

    jest.doMock('@/functions/websocket/websocket-env', () => ({
      loadWebSocketFunctionEnv: jest.fn(() => env)
    }));
    jest.doMock('@/functions/websocket/websocket-token-verifier', () => ({
      WebSocketTokenVerifier: jest.fn().mockImplementation(() => ({ verify }))
    }));
    jest.doMock('@/functions/websocket/connection-store', () => ({
      DEFAULT_WEBSOCKET_ROOM_ID: 'dev',
      WebSocketConnectionStore: jest.fn().mockImplementation(() => ({ store }))
    }));

    const module = await import('@/functions/websocket/connect.handler');

    return { handler: module.handler, verify, store };
  }

  function event(
    overrides: Record<string, unknown> = {}
  ): APIGatewayProxyWebsocketEventV2 {
    return ({
      requestContext: {
        connectionId: 'connection-1'
      },
      headers: {
        authorization: 'Bearer token'
      },
      ...overrides
    } as unknown) as APIGatewayProxyWebsocketEventV2;
  }

  it('rejects events without a connection id', async () => {
    const { handler, verify, store } = await loadHandler();

    await expect(
      handler(event({ requestContext: {} as never }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 400,
      body: JSON.stringify({ message: 'Missing connection id' })
    });
    expect(verify).not.toHaveBeenCalled();
    expect(store).not.toHaveBeenCalled();
  });

  it('rejects events without an authorization token', async () => {
    const { handler, verify, store } = await loadHandler();

    await expect(
      handler(event({ headers: {}, queryStringParameters: {} }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 401,
      body: JSON.stringify({ message: 'Missing authorization token' })
    });
    expect(verify).not.toHaveBeenCalled();
    expect(store).not.toHaveBeenCalled();
  });

  it('verifies the token and stores the connection', async () => {
    const identity = { sub: 'user-1', email: 'user@example.test' };
    const verify = jest.fn().mockResolvedValue(identity);
    const store = jest.fn().mockResolvedValue(undefined);
    const { handler } = await loadHandler({ verify, store });

    await expect(
      handler(event(), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 200,
      body: JSON.stringify({ message: 'Connected' })
    });
    expect(verify).toHaveBeenCalledWith('token');
    expect(store).toHaveBeenCalledWith({
      connectionId: 'connection-1',
      identity,
      roomId: 'dev',
      ttlSeconds: 60
    });
  });

  it('stores the requested room id when present', async () => {
    const store = jest.fn().mockResolvedValue(undefined);
    const { handler } = await loadHandler({ store });

    await handler(
      event({ queryStringParameters: { roomId: 'room-7' } }),
      {} as never,
      jest.fn()
    );

    expect(store).toHaveBeenCalledWith(
      expect.objectContaining({ roomId: 'room-7' })
    );
  });

  it('rejects invalid authorization tokens', async () => {
    const verify = jest.fn().mockRejectedValue(new Error('invalid'));
    const store = jest.fn();
    const { handler } = await loadHandler({ verify, store });

    await expect(
      handler(event(), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 401,
      body: JSON.stringify({ message: 'Invalid authorization token' })
    });
    expect(store).not.toHaveBeenCalled();
  });
});
