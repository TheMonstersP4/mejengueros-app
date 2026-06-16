const mockSend = jest.fn();
const mockClient = jest.fn(() => ({ send: mockSend }));
const mockPostToConnectionCommand = jest.fn((input) => ({
  input,
  type: 'post'
}));
const mockDeleteConnectionCommand = jest.fn((input) => ({
  input,
  type: 'delete'
}));

jest.mock('@aws-sdk/client-apigatewaymanagementapi', () => ({
  ApiGatewayManagementApiClient: mockClient,
  PostToConnectionCommand: mockPostToConnectionCommand,
  DeleteConnectionCommand: mockDeleteConnectionCommand
}));

import {
  WebSocketBroadcaster,
  buildManagementEndpoint,
  mapRoomUsers
} from '@/functions/websocket/websocket-broadcaster';

describe('WebSocketBroadcaster', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('builds the management endpoint from API Gateway context', () => {
    expect(buildManagementEndpoint('ws.example.test', 'dev')).toBe(
      'https://ws.example.test/dev'
    );
  });

  it('maps unique users from active connections', () => {
    expect(
      mapRoomUsers([
        { connectionId: 'a', roomId: 'dev', userId: '2', email: 'two@example.test', name: 'Two' },
        { connectionId: 'b', roomId: 'dev', userId: '1', email: 'one@example.test', name: 'One' },
        { connectionId: 'c', roomId: 'dev', userId: '1', email: 'one@example.test', name: 'One' }
      ])
    ).toEqual([
      { sub: '1', email: 'one@example.test', name: 'One' },
      { sub: '2', email: 'two@example.test', name: 'Two' }
    ]);
  });

  it('posts a payload to every active connection', async () => {
    const broadcaster = new WebSocketBroadcaster('https://ws.example.test/dev');

    await expect(
      broadcaster.broadcast(
        [
          { connectionId: 'a', roomId: 'dev', userId: '1' },
          { connectionId: 'b', roomId: 'dev', userId: '2' }
        ],
        { type: 'presence' }
      )
    ).resolves.toEqual([]);
    expect(mockClient).toHaveBeenCalledWith({
      endpoint: 'https://ws.example.test/dev'
    });
    expect(mockPostToConnectionCommand).toHaveBeenCalledTimes(2);
  });

  it('returns stale connection ids for gone connections', async () => {
    mockSend.mockRejectedValueOnce({ name: 'GoneException' });
    const broadcaster = new WebSocketBroadcaster('https://ws.example.test/dev');

    await expect(
      broadcaster.broadcast([{ connectionId: 'a', roomId: 'dev', userId: '1' }], {
        type: 'presence'
      })
    ).resolves.toEqual(['a']);
  });

  it('disconnects a connection', async () => {
    const broadcaster = new WebSocketBroadcaster('https://ws.example.test/dev');

    await broadcaster.disconnect('connection-1');

    expect(mockDeleteConnectionCommand).toHaveBeenCalledWith({
      ConnectionId: 'connection-1'
    });
  });
});
