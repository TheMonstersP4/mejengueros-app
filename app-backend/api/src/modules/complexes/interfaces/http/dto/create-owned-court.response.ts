import { ApiProperty } from '@nestjs/swagger';
import { CreatedCourtResponse } from './create-complex.response';

/**
 * HTTP response body for adding a court to an owned complex.
 */
export class CreateOwnedCourtResponse {
  @ApiProperty({ type: CreatedCourtResponse })
  court!: CreatedCourtResponse;
}
