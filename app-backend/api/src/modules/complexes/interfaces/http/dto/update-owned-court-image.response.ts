import { ApiProperty } from '@nestjs/swagger';
import { MyComplexHubCourtResponse } from './my-complex-hub.response';

export class UpdateOwnedCourtImageResponse {
  @ApiProperty({ type: MyComplexHubCourtResponse })
  court!: MyComplexHubCourtResponse;
}
