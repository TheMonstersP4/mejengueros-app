import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

type PrismaBlockKind = 'model' | 'enum';

export type PrismaRelationalSchemaContract = {
  schema: string;
  migration: string;
};

const DEFAULT_SCHEMA_PATH = ['prisma', 'schema.prisma'];
const DEFAULT_MIGRATION_PATH = [
  'prisma',
  'migrations',
  '20260617000100_add_relational_mvp_schema',
  'migration.sql'
];

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
  return {
    schema: readFileSync(resolve(cwd, ...DEFAULT_SCHEMA_PATH), 'utf8'),
    migration: readFileSync(resolve(cwd, ...DEFAULT_MIGRATION_PATH), 'utf8')
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
