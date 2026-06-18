import { resolve } from 'node:path';

describe('backend test quality gates', () => {
  const apiRoot = resolve(__dirname, '../../..');

  it('includes the Prisma relational contract spec in the primary Jest command', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const jestConfig = require(resolve(apiRoot, 'jest.config.cjs'));

    expect(jestConfig.testMatch).toContain(
      '<rootDir>/test/integration/prisma-relational-schema.contract.spec.ts'
    );
  });
});
