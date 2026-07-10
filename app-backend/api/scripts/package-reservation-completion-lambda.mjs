import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const apiRoot = resolve(scriptDir, '..');
const stage = join(apiRoot, '.lambda', 'reservation-completion-package');
const zipFile = join(apiRoot, '.lambda', 'reservation-completion.zip');
const distSource = join(apiRoot, 'dist');
const distTarget = join(stage, 'dist');
const functionTarget = join(stage, 'functions', 'reservations');
const rootNodeModules = join(apiRoot, 'node_modules');
const workerRuntimeDependencies = [
  '@aws-sdk/client-secrets-manager',
  '@nestjs/common',
  '@nestjs/config',
  '@nestjs/core',
  '@prisma/adapter-pg',
  '@prisma/client',
  'nestjs-pino',
  'pg',
  'pino',
  'reflect-metadata',
  'rxjs',
  'zod'
];

if (!existsSync(join(distSource, 'functions', 'reservations', 'completion.handler.js'))) {
  throw new Error(
    'Missing dist/functions/reservations/completion.handler.js. Run npm run build before packaging.'
  );
}

if (!existsSync(rootNodeModules)) {
  throw new Error('Missing node_modules. Run npm ci before packaging the reservation worker.');
}

rmSync(stage, { recursive: true, force: true });
rmSync(zipFile, { force: true });

mkdirSync(distTarget, { recursive: true });
mkdirSync(functionTarget, { recursive: true });
cpSync(distSource, distTarget, { recursive: true });
copyDependencyClosure(workerRuntimeDependencies);

writeFileSync(
  join(functionTarget, 'completion.js'),
  "module.exports.handler = require('../../dist/functions/reservations/completion.handler').handler;\n",
  'ascii'
);

writeFileSync(
  join(stage, 'package.json'),
  JSON.stringify(
    {
      name: 'mejengueros-reservation-completion-lambda',
      version: '0.1.0',
      private: true,
      dependencies: Object.fromEntries(
        workerRuntimeDependencies.map((dependencyName) => [
          dependencyName,
          readInstalledPackageManifest(dependencyName).version
        ])
      )
    },
    null,
    2
  ),
  'ascii'
);

if (process.platform === 'win32') {
  run('tar', ['-a', '-c', '-f', zipFile, 'dist', 'functions', 'node_modules', 'package.json'], stage);
} else {
  run('zip', ['-qr', zipFile, '.'], stage);
}

console.log(`Created ${zipFile}`);

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    stdio: 'inherit'
  });

  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed`);
  }
}

function copyDependencyClosure(entryDependencyNames) {
  const copiedDependencyNames = new Set();

  for (const dependencyName of entryDependencyNames) {
    copyDependency(dependencyName, copiedDependencyNames);
  }
}

function copyDependency(dependencyName, copiedDependencyNames) {
  if (copiedDependencyNames.has(dependencyName)) {
    return;
  }

  const sourcePath = resolveInstalledPackagePath(dependencyName);
  const targetPath = join(stage, 'node_modules', ...dependencyName.split('/'));

  mkdirSync(dirname(targetPath), { recursive: true });
  cpSync(sourcePath, targetPath, { recursive: true });
  copiedDependencyNames.add(dependencyName);

  const manifest = readInstalledPackageManifest(dependencyName);
  const nestedDependencyNames = [
    ...Object.keys(manifest.dependencies ?? {}),
    ...Object.keys(manifest.optionalDependencies ?? {})
  ];

  for (const nestedDependencyName of nestedDependencyNames) {
    if (existsSync(resolveInstalledPackagePath(nestedDependencyName))) {
      copyDependency(nestedDependencyName, copiedDependencyNames);
    }
  }
}

function readInstalledPackageManifest(dependencyName) {
  return JSON.parse(readFileSync(join(resolveInstalledPackagePath(dependencyName), 'package.json'), 'utf8'));
}

function resolveInstalledPackagePath(dependencyName) {
  return join(rootNodeModules, ...dependencyName.split('/'));
}
