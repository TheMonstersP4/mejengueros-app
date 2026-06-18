const { readdirSync, readFileSync, statSync } = require('node:fs');
const { join, relative } = require('node:path');

const SOURCE_DIRECTORIES = ['src', 'test'];
const SKIP_DIRECTORIES = new Set(['dist', 'node_modules', 'generated']);
const TEST_FILE_PATTERN = /\.ts$/;
const FOCUSED_TEST_PATTERN =
  /\b(?:describe|it|test)\.only\s*\(|\b(?:fdescribe|fit|ftest)\s*\(/;

function findFocusedTestMatches(source) {
  return source
    .split(/\r?\n/)
    .map((text, index) => ({ line: index + 1, text }))
    .filter(({ text }) => FOCUSED_TEST_PATTERN.test(text));
}

function collectTestFiles(rootDir, currentDir) {
  const entries = readdirSync(currentDir);
  const files = [];

  for (const entry of entries) {
    const absolutePath = join(currentDir, entry);
    const stats = statSync(absolutePath);

    if (stats.isDirectory()) {
      if (SKIP_DIRECTORIES.has(entry)) {
        continue;
      }

      files.push(...collectTestFiles(rootDir, absolutePath));
      continue;
    }

    if (TEST_FILE_PATTERN.test(entry)) {
      files.push({
        absolutePath,
        relativePath: relative(rootDir, absolutePath)
      });
    }
  }

  return files;
}

function ensureNoFocusedTests(rootDir = process.cwd()) {
  const findings = [];

  for (const directory of SOURCE_DIRECTORIES) {
    const absoluteDirectory = join(rootDir, directory);

    for (const file of collectTestFiles(rootDir, absoluteDirectory)) {
      const matches = findFocusedTestMatches(
        readFileSync(file.absolutePath, 'utf8')
      );

      for (const match of matches) {
        findings.push({
          filePath: file.relativePath,
          line: match.line,
          text: match.text.trim()
        });
      }
    }
  }

  if (findings.length > 0) {
    const details = findings
      .map(({ filePath, line, text }) => `- ${filePath}:${line} -> ${text}`)
      .join('\n');

    throw new Error(`Focused tests are not allowed.\n${details}`);
  }
}

if (require.main === module) {
  try {
    ensureNoFocusedTests();
  } catch (error) {
    console.error(error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

module.exports = {
  ensureNoFocusedTests,
  findFocusedTestMatches
};
