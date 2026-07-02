import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { SwaggerModule } from '@nestjs/swagger';
import type { OpenAPIObject } from '@nestjs/swagger';
import { createSwaggerConfig } from '@/bootstrap/swagger';
import { UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN } from '@/modules/reservations/shared/utc-reservation-starts-at';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

type OpenApiMethod = 'get' | 'post';
type SchemaRecord = Record<string, unknown>;

describe('OpenAPI document contract', () => {
  const originalDatabaseUrl = process.env.DATABASE_URL;
  let app: NestFastifyApplication;
  let document: OpenAPIObject;

  beforeAll(async () => {
    process.env.DATABASE_URL =
      'postgresql://test:test@localhost:5432/test?schema=mejengueros';

    const { AppModule } = await import('@/app.module');
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule]
    })
      .overrideProvider(PrismaService)
      .useValue({
        onModuleInit: jest.fn(),
        onModuleDestroy: jest.fn()
      })
      .compile();

    app = moduleRef.createNestApplication<NestFastifyApplication>(
      new FastifyAdapter({ logger: false })
    );
    app.setGlobalPrefix('v1');
    await app.init();

    document = SwaggerModule.createDocument(app, createSwaggerConfig());
  });

  afterAll(async () => {
    await app?.close();

    if (originalDatabaseUrl) {
      process.env.DATABASE_URL = originalDatabaseUrl;
    } else {
      delete process.env.DATABASE_URL;
    }
  });

  it('documents API responses with the standard envelope', () => {
    expect(responseSchema('/v1/auth/me', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/health', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/users', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/locations/provinces', 'get', '200')).toBeDefined();
    expect(
      responseSchema('/v1/locations/provinces/{provinceId}/cantons', 'get', '200')
    ).toBeDefined();
    expect(responseSchema('/v1/services', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/courts/catalog', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/complexes', 'post', '201')).toBeDefined();
    expect(responseSchema('/v1/complexes/my-hub', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/complexes/{complexId}/courts', 'post', '201')).toBeDefined();
    expect(responseSchema('/v1/reservations', 'post', '201')).toBeDefined();
    expect(responseSchema('/v1/courts/{courtId}/reservable-slots', 'get', '200')).toBeDefined();
    expect(responseSchema('/v1/files/uploads', 'post', '201')).toBeDefined();

    expectSuccessEnvelopeSchema(responseSchema('/v1/auth/me', 'get', '200'));
    expectSuccessEnvelopeSchema(responseSchema('/v1/health', 'get', '200'));
    expectSuccessEnvelopeSchema(responseSchema('/v1/files/uploads', 'post', '201'));
    expectArrayEnvelopeSchema(
      responseSchema('/v1/users', 'get', '200'),
      '#/components/schemas/UserProfileResponse'
    );
    expectArrayEnvelopeSchema(
      responseSchema('/v1/locations/provinces', 'get', '200'),
      '#/components/schemas/ProvinceCatalogResponse'
    );
    expectArrayEnvelopeSchema(
      responseSchema('/v1/locations/provinces/{provinceId}/cantons', 'get', '200'),
      '#/components/schemas/CantonCatalogResponse'
    );
    expectArrayEnvelopeSchema(
      responseSchema('/v1/courts/catalog', 'get', '200'),
      '#/components/schemas/CourtCatalogResponse'
    );
    expectArrayEnvelopeSchema(
      responseSchema('/v1/services', 'get', '200'),
      '#/components/schemas/ServiceCatalogResponse'
    );
    expectSuccessEnvelopeSchema(responseSchema('/v1/complexes', 'post', '201'));
    expectObjectEnvelopeSchema(
      responseSchema('/v1/complexes/my-hub', 'get', '200'),
      '#/components/schemas/MyComplexHubResponse'
    );
    expectObjectEnvelopeSchema(
      responseSchema('/v1/complexes/{complexId}/courts', 'post', '201'),
      '#/components/schemas/CreateOwnedCourtResponse'
    );
    expectObjectEnvelopeSchema(
      responseSchema('/v1/reservations', 'post', '201'),
      '#/components/schemas/CreateReservationResponse'
    );
    expectObjectEnvelopeSchema(
      responseSchema('/v1/courts/{courtId}/reservable-slots', 'get', '200'),
      '#/components/schemas/ReservableSlotsResponse'
    );
    expectErrorEnvelopeSchema('/v1/auth/me', 'get', '401');
    expectErrorEnvelopeSchema('/v1/complexes/my-hub', 'get', '401');
    expectErrorEnvelopeSchema('/v1/complexes/{complexId}/courts', 'post', '400');
    expectErrorEnvelopeSchema('/v1/complexes/{complexId}/courts', 'post', '401');
    expectErrorEnvelopeSchema('/v1/complexes/{complexId}/courts', 'post', '404');
    expectErrorEnvelopeSchema('/v1/reservations', 'post', '400');
    expectErrorEnvelopeSchema('/v1/reservations', 'post', '401');
    expectErrorEnvelopeSchema('/v1/reservations', 'post', '404');
    expectErrorEnvelopeSchema('/v1/reservations', 'post', '409');
    expectErrorEnvelopeSchema('/v1/courts/{courtId}/reservable-slots', 'get', '400');
    expectErrorEnvelopeSchema('/v1/courts/{courtId}/reservable-slots', 'get', '401');
    expectErrorEnvelopeSchema('/v1/courts/{courtId}/reservable-slots', 'get', '404');
    expectErrorEnvelopeSchema('/v1/services', 'get', '400');
    expectErrorEnvelopeSchema('/v1/courts/catalog', 'get', '400');
    expectErrorEnvelopeSchema(
      '/v1/locations/provinces/{provinceId}/cantons',
      'get',
      '400'
    );
    expectOperationHasPathUuidParameter('/v1/complexes/{complexId}/courts', 'post', 'complexId');
    expectOperationHasPathUuidParameter(
      '/v1/courts/{courtId}/reservable-slots',
      'get',
      'courtId'
    );
  });

  it('documents reservation startsAt as an explicit UTC whole-hour datetime with Z', () => {
    const requestSchema = componentSchema('CreateReservationRequest');
    const startsAt = schemaProperty(requestSchema, 'startsAt');

    expect(startsAt).toEqual(
      expect.objectContaining({
        description:
          'Reservation start time as a real UTC ISO datetime with explicit Z aligned to a whole hour.',
        example: '2026-07-01T18:00:00.000Z',
        format: 'date-time',
        pattern: UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN
      })
    );
  });

  it('documents optional court image uploads and the court-image file purpose', () => {
    const firstCourtSchema = componentSchema('CreateFirstCourtBodyRequest');
    const imageUploadId = schemaProperty(firstCourtSchema, 'imageUploadId');
    const createUploadUrlRequestSchema = componentSchema('CreateUploadUrlRequest');
    const purpose = schemaProperty(createUploadUrlRequestSchema, 'purpose');
    const required = (firstCourtSchema.required as string[] | undefined) ?? [];

    expect(imageUploadId).toEqual(
      expect.objectContaining({
        description: 'Optional confirmed upload identifier for the court image.',
        example: '9f6b4f0f-5f5a-4d8d-8c5e-2b2e7b0f6a3c',
        type: 'string'
      })
    );
    expect(required).not.toContain('imageUploadId');
    expect(purpose).toEqual(
      expect.objectContaining({
        enum: expect.arrayContaining(['profile-image', 'court-image'])
      })
    );
  });

  function responseSchema(
    path: string,
    method: OpenApiMethod,
    status: string
  ): SchemaRecord {
    const operation = document.paths[path]?.[method] as
      | { responses?: Record<string, unknown> }
      | undefined;
    const response = operation?.responses?.[status] as
      | { content?: Record<string, { schema?: SchemaRecord }> }
      | undefined;
    const schema = response?.content?.['application/json']?.schema;

    expect(schema).toBeDefined();

    return schema as SchemaRecord;
  }

  function componentSchema(name: string): SchemaRecord {
    const schema = document.components?.schemas?.[name];

    expect(schema).toBeDefined();

    return schema as SchemaRecord;
  }

  function schemaProperty(schema: SchemaRecord, propertyName: string): SchemaRecord {
    const properties = schema.properties as Record<string, unknown> | undefined;
    const property = properties?.[propertyName];

    expect(property).toBeDefined();

    return property as SchemaRecord;
  }

  function expectSuccessEnvelopeSchema(schema: SchemaRecord): void {
    expectBaseEnvelopeComponents();
    expect(schema.allOf).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          $ref: '#/components/schemas/ApiSuccessEnvelopeResponse'
        })
      ])
    );
    expect(dataSchema(schema)).toBeDefined();
  }

  function expectArrayEnvelopeSchema(
    schema: SchemaRecord,
    itemRef: string
  ): void {
    expectSuccessEnvelopeSchema(schema);
    expect(dataSchema(schema)).toEqual(
      expect.objectContaining({
        type: 'array',
        items: expect.objectContaining({
          $ref: itemRef
        })
      })
    );
  }

  function expectObjectEnvelopeSchema(
    schema: SchemaRecord,
    objectRef: string
  ): void {
    expectSuccessEnvelopeSchema(schema);
    expect(dataSchema(schema)).toEqual(
      expect.objectContaining({
        $ref: objectRef
      })
    );
  }

  function expectErrorEnvelopeSchema(
    path: string,
    method: OpenApiMethod,
    status: string
  ): void {
    expect(responseSchema(path, method, status)).toEqual({
      $ref: '#/components/schemas/ApiErrorEnvelopeResponse'
    });
  }

  function expectOperationHasPathUuidParameter(
    path: string,
    method: OpenApiMethod,
    parameterName: string
  ): void {
    const operation = document.paths[path]?.[method] as
      | { parameters?: Array<Record<string, unknown>> }
      | undefined;

    expect(operation?.parameters).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: parameterName,
          in: 'path',
          required: true,
          schema: expect.objectContaining({ format: 'uuid' })
        })
      ])
    );
  }

  function dataSchema(schema: SchemaRecord): unknown {
    const allOf = schema.allOf as SchemaRecord[] | undefined;
    const overrideSchema = allOf?.find((item) => 'properties' in item);
    const properties = overrideSchema?.properties as
      | Record<string, unknown>
      | undefined;

    return properties?.data;
  }

  function expectBaseEnvelopeComponents(): void {
    const successEnvelope = document.components?.schemas
      ?.ApiSuccessEnvelopeResponse as { properties?: SchemaRecord } | undefined;

    expect(successEnvelope?.properties).toEqual(
      expect.objectContaining({
        success: expect.any(Object),
        data: expect.any(Object),
        errors: expect.any(Object),
        meta: expect.any(Object)
      })
    );
  }
});
