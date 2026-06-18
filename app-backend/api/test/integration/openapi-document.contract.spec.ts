import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { SwaggerModule } from '@nestjs/swagger';
import type { OpenAPIObject } from '@nestjs/swagger';
import { createSwaggerConfig } from '@/bootstrap/swagger';
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
    expect(responseSchema('/v1/files/uploads', 'post', '201')).toBeDefined();

    expectSuccessEnvelopeSchema(responseSchema('/v1/auth/me', 'get', '200'));
    expectSuccessEnvelopeSchema(responseSchema('/v1/health', 'get', '200'));
    expectSuccessEnvelopeSchema(responseSchema('/v1/files/uploads', 'post', '201'));
    expectArrayEnvelopeSchema(responseSchema('/v1/users', 'get', '200'));
    expectErrorEnvelopeSchema('/v1/auth/me', 'get', '401');
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

  function expectArrayEnvelopeSchema(schema: SchemaRecord): void {
    expectSuccessEnvelopeSchema(schema);
    expect(dataSchema(schema)).toEqual(
      expect.objectContaining({
        type: 'array',
        items: expect.objectContaining({
          $ref: '#/components/schemas/UserProfileResponse'
        })
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
