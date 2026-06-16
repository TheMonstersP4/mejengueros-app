const mockSend = jest.fn();
const mockGetSecretValueCommand = jest.fn();

jest.mock('@aws-sdk/client-secrets-manager', () => ({
  GetSecretValueCommand: mockGetSecretValueCommand,
  SecretsManagerClient: jest.fn().mockImplementation(() => ({
    send: mockSend
  }))
}));

import {
  GetSecretValueCommand,
  SecretsManagerClient
} from '@aws-sdk/client-secrets-manager';
import { loadDatabaseUrlFromSecret } from '@/bootstrap/database-secret';

describe('loadDatabaseUrlFromSecret', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.clearAllMocks();
    process.env = {
      ...originalEnv,
      AWS_REGION: 'us-east-2'
    };
    delete process.env.DATABASE_URL;
    delete process.env.DATABASE_SECRET_ARN;
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('does not read Secrets Manager when DATABASE_URL already exists', async () => {
    process.env.DATABASE_URL = 'postgresql://user:pass@example.test/app';
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:test';

    await loadDatabaseUrlFromSecret();

    expect(SecretsManagerClient).not.toHaveBeenCalled();
    expect(mockSend).not.toHaveBeenCalled();
  });

  it('does not read Secrets Manager when no secret ARN is configured', async () => {
    await loadDatabaseUrlFromSecret();

    expect(SecretsManagerClient).not.toHaveBeenCalled();
    expect(mockSend).not.toHaveBeenCalled();
  });

  it('loads a raw database URL from Secrets Manager', async () => {
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:test';
    mockSend.mockResolvedValue({
      SecretString: 'postgresql://user:pass@example.test/app?schema=mejengueros_dev'
    });

    await loadDatabaseUrlFromSecret();

    expect(SecretsManagerClient).toHaveBeenCalledWith({ region: 'us-east-2' });
    expect(GetSecretValueCommand).toHaveBeenCalledWith({
      SecretId: 'arn:aws:secretsmanager:test'
    });
    expect(process.env.DATABASE_URL).toBe(
      'postgresql://user:pass@example.test/app?schema=mejengueros_dev'
    );
  });

  it('loads a database URL from a JSON secret payload', async () => {
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:test';
    mockSend.mockResolvedValue({
      SecretString: JSON.stringify({
        DATABASE_URL: 'postgresql://user:pass@example.test/app'
      })
    });

    await loadDatabaseUrlFromSecret();

    expect(process.env.DATABASE_URL).toBe('postgresql://user:pass@example.test/app');
  });

  it('loads a database URL from a binary secret payload', async () => {
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:test';
    mockSend.mockResolvedValue({
      SecretBinary: Buffer.from('postgresql://user:pass@example.test/app')
    });

    await loadDatabaseUrlFromSecret();

    expect(process.env.DATABASE_URL).toBe('postgresql://user:pass@example.test/app');
  });

  it('rejects empty secret payloads', async () => {
    process.env.DATABASE_SECRET_ARN = 'arn:aws:secretsmanager:test';
    mockSend.mockResolvedValue({ SecretString: '   ' });

    await expect(loadDatabaseUrlFromSecret()).rejects.toThrow('Database secret is empty.');
  });
});
