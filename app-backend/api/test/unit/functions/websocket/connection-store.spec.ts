const mockSend = jest.fn();
const mockDocumentClientFrom = jest.fn(() => ({ send: mockSend }));
const mockDynamoDBClient = jest.fn();
const mockPutCommand = jest.fn((input) => ({ input, type: 'put' }));
const mockDeleteCommand = jest.fn((input) => ({ input, type: 'delete' }));
const mockGetCommand = jest.fn((input) => ({ input, type: 'get' }));
const mockQueryCommand = jest.fn((input) => ({ input, type: 'query' }));

jest.mock('@aws-sdk/client-dynamodb', () => ({
  DynamoDBClient: mockDynamoDBClient
}));

jest.mock('@aws-sdk/lib-dynamodb', () => ({
  DynamoDBDocumentClient: {
    from: mockDocumentClientFrom
  },
  PutCommand: mockPutCommand,
  DeleteCommand: mockDeleteCommand,
  GetCommand: mockGetCommand,
  QueryCommand: mockQueryCommand
}));

import { WebSocketConnectionStore } from '@/functions/websocket/connection-store';

describe('WebSocketConnectionStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers().setSystemTime(new Date('2026-01-02T03:04:05.000Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('stores a connection with identity and expiration metadata', async () => {
    const store = new WebSocketConnectionStore('us-east-1', 'connections');

    await store.store({
      connectionId: 'connection-1',
      identity: { sub: 'user-1', email: 'user@example.test' },
      roomId: 'room-1',
      ttlSeconds: 60
    });

    expect(mockDynamoDBClient).toHaveBeenCalledWith({ region: 'us-east-1' });
    expect(mockDocumentClientFrom).toHaveBeenCalledTimes(1);
    expect(mockPutCommand).toHaveBeenCalledWith({
      TableName: 'connections',
      Item: {
        connectionId: 'connection-1',
        roomId: 'room-1',
        userId: 'user-1',
        email: 'user@example.test',
        name: undefined,
        connectedAt: '2026-01-02T03:04:05.000Z',
        expiresAt: 1767323105
      }
    });
    expect(mockSend).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'put' })
    );
  });

  it('removes a connection by id', async () => {
    const store = new WebSocketConnectionStore('us-east-1', 'connections');

    await store.remove('connection-1');

    expect(mockDeleteCommand).toHaveBeenCalledWith({
      TableName: 'connections',
      Key: { connectionId: 'connection-1' }
    });
    expect(mockSend).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'delete' })
    );
  });

  it('finds a connection by id', async () => {
    mockSend.mockResolvedValueOnce({
      Item: {
        connectionId: 'connection-1',
        roomId: 'room-1',
        userId: 'user-1',
        email: 'user@example.test',
        name: 'User'
      }
    });
    const store = new WebSocketConnectionStore('us-east-1', 'connections');

    await expect(store.findById('connection-1')).resolves.toEqual({
      connectionId: 'connection-1',
      roomId: 'room-1',
      userId: 'user-1',
      email: 'user@example.test',
      name: 'User'
    });
    expect(mockGetCommand).toHaveBeenCalledWith({
      TableName: 'connections',
      Key: { connectionId: 'connection-1' }
    });
  });

  it('lists active connections', async () => {
    mockSend.mockResolvedValueOnce({
      Items: [
        {
          connectionId: 'connection-1',
          roomId: 'room-1',
          userId: 'user-1',
          email: 'user@example.test',
          name: 'User'
        },
        {
          connectionId: 123,
          userId: 'invalid'
        }
      ]
    });
    const store = new WebSocketConnectionStore('us-east-1', 'connections');

    await expect(store.listActive('room-1')).resolves.toEqual([
      {
        connectionId: 'connection-1',
        roomId: 'room-1',
        userId: 'user-1',
        email: 'user@example.test',
        name: 'User'
      }
    ]);
    expect(mockSend).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'query' })
    );
    expect(mockQueryCommand).toHaveBeenCalledWith({
      TableName: 'connections',
      IndexName: 'byRoomId',
      KeyConditionExpression: 'roomId = :roomId',
      FilterExpression: 'attribute_not_exists(expiresAt) OR expiresAt > :now',
      ExpressionAttributeValues: {
        ':roomId': 'room-1',
        ':now': 1767323045
      }
    });
  });
});
