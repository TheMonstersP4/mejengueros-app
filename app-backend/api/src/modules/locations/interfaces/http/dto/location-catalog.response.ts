import { ApiProperty } from '@nestjs/swagger';

/**
 * Province catalog item returned by the wizard catalog endpoint.
 */
export class ProvinceCatalogResponse {
  @ApiProperty({ example: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1' })
  id!: string;

  @ApiProperty({ example: 'SJ' })
  code!: string;

  @ApiProperty({ example: 'San José' })
  name!: string;
}

/**
 * Canton catalog item returned by the wizard catalog endpoint.
 */
export class CantonCatalogResponse {
  @ApiProperty({ example: '1f6adf24-ea42-4c49-9179-c5f73fef7a41' })
  id!: string;

  @ApiProperty({ example: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1' })
  provinceId!: string;

  @ApiProperty({ example: 'SJ-ESC' })
  code!: string;

  @ApiProperty({ example: 'Escazú' })
  name!: string;
}
