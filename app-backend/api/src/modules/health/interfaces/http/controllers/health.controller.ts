import { Controller, Get } from '@nestjs/common';

interface IHealthResponse {
  status: 'ok';
  timestamp: string;
}

/**
 * HTTP health check endpoint.
 */
@Controller('health')
export class HealthController {
  /**
   * Returns process health information.
   *
   * @returns Basic service health response.
   */
  @Get()
  check(): IHealthResponse {
    return {
      status: 'ok',
      timestamp: new Date().toISOString()
    };
  }
}
