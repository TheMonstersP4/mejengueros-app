import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional, IsString } from 'class-validator';
import { USER_ROLE_KINDS } from '../../../domain/entities/user.entity';
import type { UserRoleKind } from '../../../domain/entities/user.entity';

/**
 * Administrative user account update request.
 */
export class UpdateUserAccountRequest {
  @ApiPropertyOptional({
    description: 'Administrative display name stored on the account.',
    example: 'Alex Morgan'
  })
  @IsOptional()
  @IsString()
  name?: string;

  @ApiPropertyOptional({
    description: 'Application role assigned to the account.',
    enum: USER_ROLE_KINDS,
    example: 'OWNER'
  })
  @IsOptional()
  @IsIn(USER_ROLE_KINDS)
  role?: UserRoleKind;
}
