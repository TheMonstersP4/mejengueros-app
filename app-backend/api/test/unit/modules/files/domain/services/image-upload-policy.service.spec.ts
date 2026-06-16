import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { FileOwnershipError } from '@/modules/files/domain/errors/file-ownership.error';
import { FileTooLargeError } from '@/modules/files/domain/errors/file-too-large.error';
import { InvalidFileObjectKeyError } from '@/modules/files/domain/errors/invalid-file-object-key.error';
import { InvalidFilePurposeError } from '@/modules/files/domain/errors/invalid-file-purpose.error';
import { UnsupportedFileTypeError } from '@/modules/files/domain/errors/unsupported-file-type.error';
import { ImageUploadPolicyService } from '@/modules/files/domain/services/image-upload-policy.service';

describe('ImageUploadPolicyService', () => {
  const policy = new ImageUploadPolicyService({
    allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
    profileImageMaxBytes: 1024,
    keyPrefix: 'dev'
  });

  it('validates a profile image upload intent', () => {
    expect(
      policy.validate({
        purpose: FilePurpose.ProfileImage,
        contentType: 'image/jpeg',
        sizeBytes: 512
      })
    ).toEqual({
      purpose: FilePurpose.ProfileImage,
      contentType: 'image/jpeg',
      sizeBytes: 512,
      maxSizeBytes: 1024
    });
  });

  it('rejects unsupported file purposes', () => {
    expect(() =>
      policy.validate({
        purpose: 'court-image',
        contentType: 'image/jpeg',
        sizeBytes: 512
      })
    ).toThrow(InvalidFilePurposeError);
  });

  it('rejects unsupported image types', () => {
    expect(() =>
      policy.validate({
        purpose: FilePurpose.ProfileImage,
        contentType: 'image/svg+xml',
        sizeBytes: 512
      })
    ).toThrow(UnsupportedFileTypeError);
  });

  it('rejects files larger than the configured profile limit', () => {
    expect(() =>
      policy.validate({
        purpose: FilePurpose.ProfileImage,
        contentType: 'image/png',
        sizeBytes: 2048
      })
    ).toThrow(FileTooLargeError);
  });

  it('builds stable private object keys', () => {
    expect(
      policy.buildObjectKey({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'user/sub:123',
        contentType: 'image/webp',
        date: new Date('2026-06-04T00:00:00.000Z'),
        id: 'file-id'
      })
    ).toBe('dev/profile-image/user-sub-123/2026/06/file-id.webp');
  });

  it('validates confirmed uploaded object metadata', () => {
    expect(
      policy.validateUploadedObject({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'user/sub:123',
        objectKey: 'dev/profile-image/user-sub-123/2026/06/file-id.jpg',
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: 'image/jpeg'
      })
    ).toEqual({
      purpose: FilePurpose.ProfileImage,
      objectKey: 'dev/profile-image/user-sub-123/2026/06/file-id.jpg',
      contentType: 'image/jpeg',
      sizeBytes: 512
    });
  });

  it('rejects uploaded objects owned by another user', () => {
    expect(() =>
      policy.validateObjectKeyForOwner({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'current-user',
        objectKey: 'dev/profile-image/other-user/2026/06/file-id.jpg'
      })
    ).toThrow(FileOwnershipError);
  });

  it('rejects confirmed uploads with unsupported stored content types', () => {
    expect(() =>
      policy.validateUploadedObject({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'user-sub-123',
        objectKey: 'dev/profile-image/user-sub-123/2026/06/file-id.gif',
        contentType: 'image/gif',
        sizeBytes: 512,
        detectedContentType: 'image/gif'
      })
    ).toThrow(UnsupportedFileTypeError);
  });

  it('rejects confirmed uploads whose byte signature is unknown', () => {
    expect(() =>
      policy.validateUploadedObject({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'user-sub-123',
        objectKey: 'dev/profile-image/user-sub-123/2026/06/file-id.jpg',
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: null
      })
    ).toThrow(UnsupportedFileTypeError);
  });

  it('rejects confirmed uploads above the configured size limit', () => {
    expect(() =>
      policy.validateUploadedObject({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'user-sub-123',
        objectKey: 'dev/profile-image/user-sub-123/2026/06/file-id.jpg',
        contentType: 'image/jpeg',
        sizeBytes: 2048,
        detectedContentType: 'image/jpeg'
      })
    ).toThrow(FileTooLargeError);
  });

  it('rejects confirmed uploads with unsupported purposes', () => {
    expect(() =>
      policy.validateObjectKeyForOwner({
        purpose: 'court-image',
        ownerId: 'current-user',
        objectKey: 'dev/profile-image/current-user/2026/06/file-id.jpg'
      })
    ).toThrow(InvalidFilePurposeError);
  });

  it('rejects unsafe uploaded object keys', () => {
    expect(() =>
      policy.validateObjectKeyForOwner({
        purpose: FilePurpose.ProfileImage,
        ownerId: 'current-user',
        objectKey: '../profile-image/current-user/file-id.jpg'
      })
    ).toThrow(InvalidFileObjectKeyError);
  });
});
