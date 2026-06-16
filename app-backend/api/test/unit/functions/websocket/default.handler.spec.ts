import type { APIGatewayProxyWebsocketEventV2 } from 'aws-lambda';

describe('default websocket handler', () => {
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
    listActive?: jest.Mock;
    broadcast?: jest.Mock;
    remove?: jest.Mock;
  }) {
    const listActive =
      options?.listActive ??
      jest.fn().mockResolvedValue([
        {
          connectionId: 'connection-1',
          roomId: 'dev',
          userId: 'user-1',
          email: 'one@example.test',
          name: 'One'
        },
        {
          connectionId: 'connection-2',
          roomId: 'dev',
          userId: 'user-2',
          email: 'two@example.test',
          name: 'Two'
        }
      ]);
    const remove = options?.remove ?? jest.fn().mockResolvedValue(undefined);
    const broadcast = options?.broadcast ?? jest.fn().mockResolvedValue([]);

    jest.doMock('@/functions/websocket/websocket-env', () => ({
      loadWebSocketFunctionEnv: jest.fn(() => env)
    }));
    jest.doMock('@/functions/websocket/connection-store', () => ({
      WebSocketConnectionStore: jest.fn().mockImplementation(() => ({
        listActive,
        remove
      }))
    }));
    jest.doMock('@/functions/websocket/websocket-broadcaster', () => {
      const actual = jest.requireActual('@/functions/websocket/websocket-broadcaster');

      return {
        ...actual,
        WebSocketBroadcaster: jest.fn().mockImplementation(() => ({ broadcast }))
      };
    });

    const module = await import('@/functions/websocket/default.handler');

    return { handler: module.handler, listActive, broadcast, remove };
  }

  function event(
    body: Record<string, unknown> | undefined,
    overrides: Partial<APIGatewayProxyWebsocketEventV2> = {}
  ): APIGatewayProxyWebsocketEventV2 {
    return {
      requestContext: {
        connectionId: 'connection-1',
        domainName: 'ws.example.test',
        stage: 'dev'
      },
      body: body ? JSON.stringify(body) : undefined,
      ...overrides
    } as APIGatewayProxyWebsocketEventV2;
  }

  it('broadcasts room presence when a user joins', async () => {
    const { handler, listActive, broadcast } = await loadHandler();

    await expect(
      handler(event({ action: 'joinRoom', roomId: 'dev' }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 200,
      body: JSON.stringify({ message: 'Delivered' })
    });
    expect(broadcast).toHaveBeenCalledWith(
      expect.any(Array),
      expect.objectContaining({
        type: 'presence',
        roomId: 'dev',
        users: [
          { sub: 'user-1', email: 'one@example.test', name: 'One' },
          { sub: 'user-2', email: 'two@example.test', name: 'Two' }
        ]
      })
    );
    expect(listActive).toHaveBeenCalledWith('dev');
  });

  it('broadcasts chat messages with sender data', async () => {
    const { handler, broadcast } = await loadHandler();

    await expect(
      handler(
        event({
          action: 'sendMessage',
          roomId: 'dev',
          message: 'hola',
          sentAt: '2026-01-01T00:00:00.000Z'
        }),
        {} as never,
        jest.fn()
      )
    ).resolves.toMatchObject({
      statusCode: 200
    });
    expect(broadcast).toHaveBeenCalledWith(
      expect.any(Array),
      {
        type: 'message',
        roomId: 'dev',
        message: 'hola',
        sentAt: '2026-01-01T00:00:00.000Z',
        sender: {
          sub: 'user-1',
          email: 'one@example.test',
          name: 'One'
        }
      }
    );
  });

  it('removes stale connections reported by the broadcaster', async () => {
    const remove = jest.fn().mockResolvedValue(undefined);
    const broadcast = jest.fn().mockResolvedValue(['connection-3']);
    const { handler } = await loadHandler({ broadcast, remove });

    await expect(
      handler(event({ action: 'joinRoom' }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 200
    });
    expect(remove).toHaveBeenCalledWith('connection-3');
  });

  it('rejects events without websocket context', async () => {
    const { handler, broadcast } = await loadHandler();

    await expect(
      handler(event(undefined, { requestContext: {} as never }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 400,
      body: JSON.stringify({ message: 'Missing WebSocket context' })
    });
    expect(broadcast).not.toHaveBeenCalled();
  });
});
