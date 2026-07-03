export interface IFileReadUrlPort {
  /**
   * Creates a short-lived read URL for a private file.
   *
   * @param objectKey - Private storage object key.
   * @returns Presigned read URL.
   */
  createReadUrl(objectKey: string): Promise<string>;
}

/**
 * Injection token for read-only file URL generation.
 */
export const FILE_READ_URL_PORT = Symbol('FILE_READ_URL_PORT');
