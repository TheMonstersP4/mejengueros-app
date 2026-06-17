import { Controller, Get } from '@nestjs/common';
import { ApiOperation, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { HealthResponse } from '../dto/health.response';

/**
 * HTTP health check endpoint.
 */
@ApiTags('health')
@Controller('health')
export class HealthController {
  /**
   * Returns process health information.
   *
   * @returns Basic service health response.
   */
  @Get()
  @ApiOperation({
    summary: 'Return service health information.',
    description:
      'Returns a lightweight health response used by clients and deployment checks.'
  })
  @ApiEnvelopeOk(
    HealthResponse,
    'Service health information wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(500)
  check(): HealthResponse {
    return {
      status: 'ok',
      timestamp: new Date().toISOString()
    };
  }
}
