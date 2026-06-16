import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { configureSwagger } from '@/bootstrap/swagger';

jest.mock('@nestjs/swagger', () => ({
  DocumentBuilder: jest.fn(),
  SwaggerModule: {
    createDocument: jest.fn(),
    setup: jest.fn()
  }
}));

describe('configureSwagger', () => {
  it('registers Swagger UI and OpenAPI JSON routes', () => {
    const build = jest.fn().mockReturnValue({ title: 'config' });
    const builder = {
      setTitle: jest.fn().mockReturnThis(),
      setDescription: jest.fn().mockReturnThis(),
      setVersion: jest.fn().mockReturnThis(),
      addBearerAuth: jest.fn().mockReturnThis(),
      build
    };
    jest.mocked(DocumentBuilder).mockReturnValue(builder as never);
    jest.mocked(SwaggerModule.createDocument).mockReturnValue({ paths: {} } as never);
    const app = { getHttpAdapter: jest.fn() };

    configureSwagger(app as never);

    expect(builder.setTitle).toHaveBeenCalledWith('Mejengueros API');
    expect(builder.addBearerAuth).toHaveBeenCalledTimes(1);
    expect(SwaggerModule.createDocument).toHaveBeenCalledWith(app, {
      title: 'config'
    });
    expect(SwaggerModule.setup).toHaveBeenCalledWith('docs', app, { paths: {} }, {
      jsonDocumentUrl: 'openapi.json'
    });
  });
});
