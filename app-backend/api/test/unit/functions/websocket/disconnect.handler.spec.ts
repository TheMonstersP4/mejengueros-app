import type { APIGatewayProxyWebsocketEventV2 } from 'aws-lambda';

describe('disconnect websocket handler', () => {
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
    findById?: jest.Mock;
    listActive?: jest.Mock;
    remove?: jest.Mock;
  }) {
    const findById =
      options?.findById ??
      jest.fn().mockResolvedValue({
        connectionId: 'connection-1',
        roomId: 'room-1',
        userId: 'user-1'
      });
    const listActive = options?.listActive ?? jest.fn().mockResolvedValue([]);
    const remove = options?.remove ?? jest.fn().mockResolvedValue(undefined);

    jest.doMock('@/functions/websocket/websocket-env', () => ({
      loadWebSocketFunctionEnv: jest.fn(() => env)
    }));
    jest.doMock('@/functions/websocket/connection-store', () => ({
      WebSocketConnectionStore: jest.fn().mockImplementation(() => ({
        findById,
        listActive,
        remove
      }))
    }));

    const module = await import('@/functions/websocket/disconnect.handler');

    return { handler: module.handler, findById, listActive, remove };
  }

  function event(
    overrides: Partial<APIGatewayProxyWebsocketEventV2> = {}
  ): APIGatewayProxyWebsocketEventV2 {
    return {
      requestContext: {
        connectionId: 'connection-1'
      },
      ...overrides
    } as APIGatewayProxyWebsocketEventV2;
  }

  it('rejects events without a connection id', async () => {
    const { handler, remove } = await loadHandler();

    await expect(
      handler(event({ requestContext: {} as never }), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 400,
      body: JSON.stringify({ message: 'Missing connection id' })
    });
    expect(remove).not.toHaveBeenCalled();
  });

  it('removes the connection id', async () => {
    const remove = jest.fn().mockResolvedValue(undefined);
    const { handler, findById, listActive } = await loadHandler({ remove });

    await expect(
      handler(event(), {} as never, jest.fn())
    ).resolves.toMatchObject({
      statusCode: 200,
      body: JSON.stringify({ message: 'Disconnected' })
    });
    expect(findById).toHaveBeenCalledWith('connection-1');
    expect(remove).toHaveBeenCalledWith('connection-1');
    expect(listActive).not.toHaveBeenCalled();
  });

  it('broadcasts room presence when websocket context is available', async () => {
    const listActive = jest.fn().mockResolvedValue([]);
    const { handler } = await loadHandler({ listActive });

    await handler(
      event({
        requestContext: {
          connectionId: 'connection-1',
          domainName: 'ws.example.test',
          stage: 'dev'
        } as never
      }),
      {} as never,
      jest.fn()
    );

    expect(listActive).toHaveBeenCalledWith('room-1');
  });
});
