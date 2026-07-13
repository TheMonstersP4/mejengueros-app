import { ApiProperty } from '@nestjs/swagger';

export class CourtCatalogLocationResponse {
  @ApiProperty({ example: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1' })
  id!: string;

  @ApiProperty({ example: 'San José' })
  name!: string;
}

export class CourtCatalogRatingResponse {
  @ApiProperty({ example: 4.5, nullable: true })
  average!: number | null;

  @ApiProperty({ example: 12 })
  count!: number;
}

export class CourtCatalogResponse {
  @ApiProperty({ example: '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa' })
  courtId!: string;

  @ApiProperty({ example: 'Cancha 1' })
  courtName!: string;

  @ApiProperty({ example: '0b65bc36-78b2-4a80-b0f9-bb13dbf9555b' })
  complexId!: string;

  @ApiProperty({ example: 'Complejo Los Nogales' })
  complexName!: string;

  @ApiProperty({ type: CourtCatalogLocationResponse })
  province!: CourtCatalogLocationResponse;

  @ApiProperty({ type: CourtCatalogLocationResponse })
  canton!: CourtCatalogLocationResponse;

  @ApiProperty({ example: 9.935, nullable: true })
  latitude!: number | null;

  @ApiProperty({ example: -84.091, nullable: true })
  longitude!: number | null;

  @ApiProperty({ example: ['Sintetico', 'Iluminacion'] })
  services!: string[];

  @ApiProperty({ type: CourtCatalogRatingResponse })
  rating!: CourtCatalogRatingResponse;

  @ApiProperty({ example: true })
  isReservableToday!: boolean;

  @ApiProperty({ example: null, nullable: true })
  imageUrl!: string | null;
}
