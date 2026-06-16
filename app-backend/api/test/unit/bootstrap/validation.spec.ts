import type { INestApplication } from '@nestjs/common';
import { ValidationPipe } from '@nestjs/common';
import { configureValidation } from '@/bootstrap/validation';

describe('configureValidation', () => {
  it('registers the global validation pipe', () => {
    const app = {
      useGlobalPipes: jest.fn()
    } as unknown as INestApplication;

    configureValidation(app);

    expect(app.useGlobalPipes).toHaveBeenCalledWith(expect.any(ValidationPipe));
  });
});
