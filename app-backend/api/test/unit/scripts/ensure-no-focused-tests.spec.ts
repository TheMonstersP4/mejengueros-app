import { resolve } from 'node:path';

describe('ensure-no-focused-tests script', () => {
  const apiRoot = resolve(__dirname, '../../..');

  it('detects .only and focused Jest aliases', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { findFocusedTestMatches } = require(
      resolve(apiRoot, 'scripts/ensure-no-focused-tests.cjs')
    ) as {
      findFocusedTestMatches: (source: string) => Array<{ line: number; text: string }>;
    };

    const focusedIt = `it.${'only'}('focus', () => {});`;
    const focusedDescribe = `${'fdescribe'}('legacy focus', () => {});`;

    const matches = findFocusedTestMatches([
      "describe('suite', () => {});",
      focusedIt,
      focusedDescribe
    ].join('\n'));

    expect(matches).toEqual([
      { line: 2, text: focusedIt },
      { line: 3, text: focusedDescribe }
    ]);
  });

  it('ignores regular Jest declarations', () => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { findFocusedTestMatches } = require(
      resolve(apiRoot, 'scripts/ensure-no-focused-tests.cjs')
    ) as {
      findFocusedTestMatches: (source: string) => Array<{ line: number; text: string }>;
    };

    const matches = findFocusedTestMatches([
      "describe('suite', () => {});",
      "it('works', () => {});",
      "test('also works', () => {});"
    ].join('\n'));

    expect(matches).toEqual([]);
  });
});
