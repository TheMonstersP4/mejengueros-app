describe('main bootstrap', () => {
  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  it('creates, configures, and starts the HTTP app', async () => {
    const app = {
      listen: jest.fn().mockResolvedValue(undefined)
    };
    const createFastifyApp = jest.fn().mockResolvedValue(app);
    const configureShutdown = jest.fn();
    const configureSwagger = jest.fn();
    const configureValidation = jest.fn();

    jest.doMock('@/bootstrap/fastify', () => ({ createFastifyApp }));
    jest.doMock('@/bootstrap/shutdown', () => ({ configureShutdown }));
    jest.doMock('@/bootstrap/swagger', () => ({ configureSwagger }));
    jest.doMock('@/bootstrap/validation', () => ({ configureValidation }));
    jest.doMock('@/config/configuration', () => ({
      configuration: () => ({ app: { port: 3333 } })
    }));

    await jest.isolateModulesAsync(async () => {
      await import('@/main');
    });
    await new Promise((resolve) => setImmediate(resolve));

    expect(createFastifyApp).toHaveBeenCalledTimes(1);
    expect(configureValidation).toHaveBeenCalledWith(app);
    expect(configureSwagger).toHaveBeenCalledWith(app);
    expect(configureShutdown).toHaveBeenCalledWith(app);
    expect(app.listen).toHaveBeenCalledWith({ host: '0.0.0.0', port: 3333 });
  });
});
