import { readFileSync } from 'node:fs';
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

  it('runs the focused-test guard before lint and Jest entrypoints', () => {
    const packageJson = JSON.parse(
      readFileSync(resolve(apiRoot, 'package.json'), 'utf8')
    ) as {
      scripts: Record<string, string>;
    };

    expect(packageJson.scripts['guard:focused-tests']).toBe(
      'node ./scripts/ensure-no-focused-tests.cjs'
    );
    expect(packageJson.scripts.lint).toContain('npm run guard:focused-tests &&');
    expect(packageJson.scripts.pretest).toContain('npm run guard:focused-tests &&');
    expect(packageJson.scripts['pretest:integration']).toContain(
      'npm run guard:focused-tests &&'
    );
    expect(packageJson.scripts['pretest:cov']).toContain(
      'npm run guard:focused-tests &&'
    );
    expect(packageJson.scripts['pretest:e2e']).toContain(
      'npm run guard:focused-tests &&'
    );
  });
});
