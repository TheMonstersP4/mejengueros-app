import { Injectable } from '@nestjs/common';
import type { ImageUploadEntity } from '../../domain/entities/image-upload.entity';
import type {
  IImageUploadRepository,
  ISaveImageUploadInput
} from '../../domain/repositories/image-upload.repository';
import { ImageUploadPersistenceUnavailableError } from '../errors/image-upload-persistence-unavailable.error';

/**
 * Repository used when image metadata persistence is not configured.
 */
@Injectable()
export class DisabledImageUploadRepository implements IImageUploadRepository {
  findById(id: string): Promise<ImageUploadEntity | null> {
    void id;
    throw new ImageUploadPersistenceUnavailableError();
  }

  /**
   * Fails because confirmed upload metadata requires a database connection.
   *
   * @param input - Confirmed upload metadata.
   * @returns Never resolves successfully.
   * @throws ImageUploadPersistenceUnavailableError when DB is disabled.
   */
  saveConfirmedUpload(
    input: ISaveImageUploadInput
  ): Promise<ImageUploadEntity> {
    void input;
    throw new ImageUploadPersistenceUnavailableError();
  }

  /**
   * Fails because uploaded image listing requires a database connection.
   *
   * @param limit - Maximum number of images requested.
   * @returns Never resolves successfully.
   * @throws ImageUploadPersistenceUnavailableError when DB is disabled.
   */
  listRecent(ownerSub: string, limit: number): Promise<ImageUploadEntity[]> {
    void ownerSub;
    void limit;
    throw new ImageUploadPersistenceUnavailableError();
  }
}
