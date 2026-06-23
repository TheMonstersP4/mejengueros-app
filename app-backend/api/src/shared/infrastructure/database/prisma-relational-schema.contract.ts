import { readdirSync, readFileSync, statSync } from 'node:fs';
import { resolve } from 'node:path';

type PrismaBlockKind = 'model' | 'enum';

export type PrismaRelationalSchemaContract = {
  schema: string;
  migration: string;
};

const DEFAULT_SCHEMA_PATH = ['prisma', 'schema.prisma'];
const DEFAULT_MIGRATIONS_DIR = ['prisma', 'migrations'];

function escapeForRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

export function prismaFieldPattern(name: string, type: string): RegExp {
  return new RegExp(
    `^\\s*${escapeForRegex(name)}\\s+${escapeForRegex(type)}(?=\\s|@|$)`,
    'm'
  );
}

export function sqlFragmentPattern(fragment: string): RegExp {
  const normalizedFragment = normalizeWhitespace(fragment);
  const regexSource = escapeForRegex(normalizedFragment).replace(/\s+/g, '\\s+');

  return new RegExp(regexSource, 'm');
}

export function loadPrismaRelationalSchemaContract(
  cwd = process.cwd()
): PrismaRelationalSchemaContract {
  const migrationsRoot = resolve(cwd, ...DEFAULT_MIGRATIONS_DIR);
  const migrationDirectories = readdirSync(migrationsRoot)
    .filter((entry) => {
      const fullPath = resolve(migrationsRoot, entry);

      return statSync(fullPath).isDirectory();
    })
    .sort((left, right) => left.localeCompare(right));

  if (migrationDirectories.length === 0) {
    throw new Error('Unable to find a Prisma migration directory.');
  }

  return {
    schema: readFileSync(resolve(cwd, ...DEFAULT_SCHEMA_PATH), 'utf8'),
    migration: migrationDirectories
      .map((directory) =>
        readFileSync(resolve(migrationsRoot, directory, 'migration.sql'), 'utf8')
      )
      .join('\n\n')
  };
}

export function extractPrismaBlock(
  source: string,
  kind: PrismaBlockKind,
  name: string
): string {
  const match = source.match(
    new RegExp(`${kind}\\s+${name}\\s+\\{[\\s\\S]*?\\n\\}`, 'm')
  );

  if (!match) {
    throw new Error(`Unable to find ${kind} ${name} in Prisma schema.`);
  }

  return match[0];
}
