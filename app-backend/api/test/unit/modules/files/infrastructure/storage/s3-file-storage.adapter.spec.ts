import {
  GetObjectCommand,
  HeadObjectCommand,
  type S3Client
} from '@aws-sdk/client-s3';
import { createPresignedPost } from '@aws-sdk/s3-presigned-post';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import type { ConfigService } from '@nestjs/config';
import { S3FileStorageAdapter } from '@/modules/files/infrastructure/storage/s3-file-storage.adapter';
import { StorageInspectionError } from '@/modules/files/infrastructure/errors/storage-inspection.error';
import { StorageObjectNotFoundError } from '@/modules/files/infrastructure/errors/storage-object-not-found.error';
import { StorageUploadUrlError } from '@/modules/files/infrastructure/errors/storage-upload-url.error';

jest.mock('@aws-sdk/s3-presigned-post', () => ({
  createPresignedPost: jest.fn()
}));

jest.mock('@aws-sdk/s3-request-presigner', () => ({
  getSignedUrl: jest.fn()
}));

describe('S3FileStorageAdapter', () => {
  const mockedCreatePresignedPost = jest.mocked(createPresignedPost);
  const mockedGetSignedUrl = jest.mocked(getSignedUrl);
  const config = {
    get: jest.fn().mockReturnValue('bucket-name')
  } as unknown as ConfigService;

  beforeEach(() => {
    mockedCreatePresignedPost.mockReset();
    mockedGetSignedUrl.mockReset();
  });

  it('creates presigned S3 POST forms with upload policy conditions', async () => {
    mockedCreatePresignedPost.mockResolvedValue({
      url: 'https://upload.example.test',
      fields: {
        key: 'dev/profile-image/sub/file.jpg',
        policy: 'policy'
      }
    });
    const s3Client = {} as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.createPresignedUploadUrl({
        objectKey: 'dev/profile-image/sub/file.jpg',
        contentType: 'image/jpeg',
        expiresInSeconds: 300,
        maxSizeBytes: 1024
      })
    ).resolves.toEqual({
      method: 'POST',
      uploadUrl: 'https://upload.example.test',
      fields: {
        key: 'dev/profile-image/sub/file.jpg',
        policy: 'policy'
      }
    });

    expect(mockedCreatePresignedPost).toHaveBeenCalledWith(s3Client, {
      Bucket: 'bucket-name',
      Key: 'dev/profile-image/sub/file.jpg',
      Conditions: [
        ['content-length-range', 1, 1024],
        ['eq', '$Content-Type', 'image/jpeg']
      ],
      Fields: {
        'Content-Type': 'image/jpeg'
      },
      Expires: 300
    });
  });

  it('wraps S3 presigner failures', async () => {
    mockedCreatePresignedPost.mockRejectedValue(new Error('provider failed'));
    const adapter = new S3FileStorageAdapter({} as S3Client, config);

    await expect(
      adapter.createPresignedUploadUrl({
        objectKey: 'dev/profile-image/sub/file.jpg',
        contentType: 'image/jpeg',
        expiresInSeconds: 300,
        maxSizeBytes: 1024
      })
    ).rejects.toThrow(StorageUploadUrlError);
  });

  it('rejects missing bucket configuration before calling S3', async () => {
    const config = {
      get: jest.fn().mockReturnValue('')
    } as unknown as ConfigService;
    const adapter = new S3FileStorageAdapter({} as S3Client, config);

    await expect(
      adapter.createPresignedUploadUrl({
        objectKey: 'dev/profile-image/sub/file.jpg',
        contentType: 'image/jpeg',
        expiresInSeconds: 300,
        maxSizeBytes: 1024
      })
    ).rejects.toThrow(StorageUploadUrlError);
    expect(mockedCreatePresignedPost).not.toHaveBeenCalled();
  });

  it('inspects uploaded S3 objects and detects JPEG signatures', async () => {
    const send = jest
      .fn()
      .mockResolvedValueOnce({
        ContentType: 'image/jpeg',
        ContentLength: 512
      })
      .mockResolvedValueOnce({
        Body: {
          transformToByteArray: jest
            .fn()
            .mockResolvedValue(new Uint8Array([0xff, 0xd8, 0xff, 0x00]))
        }
      });
    const s3Client = {
      send
    } as unknown as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).resolves.toEqual({
      contentType: 'image/jpeg',
      sizeBytes: 512,
      detectedContentType: 'image/jpeg'
    });

    const calls = send.mock.calls;
    expect(calls[0]?.[0]).toBeInstanceOf(HeadObjectCommand);
    expect(calls[1]?.[0]).toBeInstanceOf(GetObjectCommand);
    expect((calls[1]?.[0] as GetObjectCommand).input).toEqual({
      Bucket: 'bucket-name',
      Key: 'dev/uploads/profile-image/sub/file.jpg',
      Range: 'bytes=0-15'
    });
  });

  it.each([
    [
      'image/png',
      new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
    ],
    [
      'image/webp',
      new Uint8Array([
        0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42,
        0x50
      ])
    ],
    ['application/octet-stream', new Uint8Array([0x00, 0x01, 0x02])]
  ])('detects %s uploaded object signatures', async (expected, bytes) => {
    const send = jest
      .fn()
      .mockResolvedValueOnce({
        ContentType: expected,
        ContentLength: 12
      })
      .mockResolvedValueOnce({
        Body: {
          transformToByteArray: jest.fn().mockResolvedValue(bytes)
        }
      });
    const adapter = new S3FileStorageAdapter(
      { send } as unknown as S3Client,
      config
    );

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file'
      })
    ).resolves.toEqual({
      contentType: expected,
      sizeBytes: 12,
      detectedContentType:
        expected === 'application/octet-stream' ? null : expected
    });
  });

  it('reads signature bytes from async iterable bodies', async () => {
    async function* body(): AsyncIterable<Buffer> {
      yield Buffer.from([0xff, 0xd8]);
      yield Buffer.from([0xff, 0x00]);
    }

    const send = jest
      .fn()
      .mockResolvedValueOnce({
        ContentType: 'image/jpeg',
        ContentLength: 4
      })
      .mockResolvedValueOnce({ Body: body() });
    const adapter = new S3FileStorageAdapter(
      { send } as unknown as S3Client,
      config
    );

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).resolves.toEqual({
      contentType: 'image/jpeg',
      sizeBytes: 4,
      detectedContentType: 'image/jpeg'
    });
  });

  it('uses safe defaults when storage metadata or body is missing', async () => {
    const send = jest.fn().mockResolvedValueOnce({}).mockResolvedValueOnce({});
    const adapter = new S3FileStorageAdapter(
      { send } as unknown as S3Client,
      config
    );

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file'
      })
    ).resolves.toEqual({
      contentType: 'application/octet-stream',
      sizeBytes: 0,
      detectedContentType: null
    });
  });

  it('maps missing uploaded objects to not found errors', async () => {
    const s3Client = {
      send: jest.fn().mockRejectedValue({ name: 'NotFound' })
    } as unknown as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).rejects.toThrow(StorageObjectNotFoundError);
  });

  it('maps S3 404 metadata failures to not found errors', async () => {
    const s3Client = {
      send: jest.fn().mockRejectedValue({ $metadata: { httpStatusCode: 404 } })
    } as unknown as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).rejects.toThrow(StorageObjectNotFoundError);
  });

  it('wraps storage inspection failures', async () => {
    const s3Client = {
      send: jest.fn().mockRejectedValue(new Error('s3 down'))
    } as unknown as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).rejects.toThrow(StorageInspectionError);
  });

  it('wraps missing bucket configuration during inspection', async () => {
    const adapter = new S3FileStorageAdapter(
      { send: jest.fn() } as unknown as S3Client,
      { get: jest.fn().mockReturnValue('') } as unknown as ConfigService
    );

    await expect(
      adapter.inspectUploadedObject({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg'
      })
    ).rejects.toThrow(StorageInspectionError);
  });

  it('creates presigned read URLs for private objects', async () => {
    mockedGetSignedUrl.mockResolvedValue('https://read.example.test/file.jpg');
    const s3Client = {} as S3Client;
    const adapter = new S3FileStorageAdapter(s3Client, config);

    await expect(
      adapter.createPresignedReadUrl({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg',
        expiresInSeconds: 300
      })
    ).resolves.toEqual({
      readUrl: 'https://read.example.test/file.jpg'
    });

    expect(mockedGetSignedUrl).toHaveBeenCalledWith(
      s3Client,
      expect.any(GetObjectCommand),
      { expiresIn: 300 }
    );
  });

  it('wraps read URL failures as storage inspection errors', async () => {
    mockedGetSignedUrl.mockRejectedValue(new Error('sign failed'));
    const adapter = new S3FileStorageAdapter({} as S3Client, config);

    await expect(
      adapter.createPresignedReadUrl({
        objectKey: 'dev/uploads/profile-image/sub/file.jpg',
        expiresInSeconds: 300
      })
    ).rejects.toThrow(StorageInspectionError);
  });
});
