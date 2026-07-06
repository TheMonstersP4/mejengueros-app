describe('reservation completion worker handler', () => {
  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  async function loadHandler(options?: {
    execute?: jest.Mock;
    info?: jest.Mock;
    createApplicationContext?: jest.Mock;
  }) {
    const execute = options?.execute ?? jest.fn().mockResolvedValue(2);
    const info = options?.info ?? jest.fn();
    const get = jest.fn(() => ({ execute }));
    const createApplicationContext =
      options?.createApplicationContext ?? jest.fn().mockResolvedValue({ get });

    jest.doMock('@/bootstrap/application-context', () => ({
      createReservationCompletionWorkerApplicationContext: createApplicationContext
    }));
    jest.doMock('pino', () => ({
      __esModule: true,
      default: jest.fn(() => ({ info }))
    }));

    const module = await import('@/functions/reservations/completion.handler');

    return {
      handler: module.handler,
      createApplicationContext,
      execute,
      info,
      get
    };
  }

  it('runs the completion use case and logs only the completed count', async () => {
    const { handler, execute, info } = await loadHandler({
      execute: jest.fn().mockResolvedValue(4)
    });

    await expect(handler({} as never, {} as never)).resolves.toEqual({
      completedReservationsCount: 4
    });
    expect(execute).toHaveBeenCalledTimes(1);
    expect(info).toHaveBeenCalledWith(
      { completedReservationsCount: 4 },
      'Expired reservation completion worker finished.'
    );
  });

  it('reuses the Nest application context across warm invocations', async () => {
    const { handler, createApplicationContext, execute } = await loadHandler();

    await handler({} as never, {} as never);
    await handler({} as never, {} as never);

    expect(createApplicationContext).toHaveBeenCalledTimes(1);
    expect(execute).toHaveBeenCalledTimes(2);
  });

  it('resets the cached bootstrap promise after a startup failure and retries on the next invocation', async () => {
    const startupError = new Error('bootstrap failed');
    const execute = jest.fn().mockResolvedValue(3);
    const createApplicationContext = jest
      .fn()
      .mockRejectedValueOnce(startupError)
      .mockResolvedValueOnce({ get: jest.fn(() => ({ execute })) });
    const { handler } = await loadHandler({
      createApplicationContext,
      execute
    });

    await expect(handler({} as never, {} as never)).rejects.toThrow(startupError);
    await expect(handler({} as never, {} as never)).resolves.toEqual({
      completedReservationsCount: 3
    });

    expect(createApplicationContext).toHaveBeenCalledTimes(2);
    expect(execute).toHaveBeenCalledTimes(1);
  });
});
