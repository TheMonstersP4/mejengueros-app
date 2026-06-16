import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const apiRoot = resolve(scriptDir, '..');
const stage = join(apiRoot, '.lambda', 'websocket-package');
const zipFile = join(apiRoot, '.lambda', 'websocket.zip');
const functionSource = join(apiRoot, 'dist', 'functions', 'websocket');
const functionTarget = join(stage, 'functions', 'websocket');

const routes = ['connect', 'default', 'disconnect'];

if (!existsSync(functionSource)) {
  throw new Error('Missing dist/functions/websocket. Run npm run build before packaging.');
}

rmSync(stage, { recursive: true, force: true });
rmSync(zipFile, { force: true });

mkdirSync(functionTarget, { recursive: true });
cpSync(functionSource, functionTarget, { recursive: true });

for (const route of routes) {
  writeFileSync(
    join(functionTarget, `${route}.js`),
    `module.exports.handler = require('./${route}.handler').handler;\n`,
    'ascii'
  );
}

writeFileSync(
  join(stage, 'package.json'),
  JSON.stringify(
    {
      name: 'mejengueros-websocket-lambda',
      version: '0.1.0',
      private: true,
      dependencies: {
        '@aws-sdk/client-apigatewaymanagementapi': '^3.0.0',
        '@aws-sdk/client-dynamodb': '^3.0.0',
        '@aws-sdk/lib-dynamodb': '^3.0.0',
        'aws-jwt-verify': '^4.0.0'
      }
    },
    null,
    2
  ),
  'ascii'
);

run('npm', ['install', '--omit=dev', '--no-audit', '--no-fund', '--package-lock=false'], stage);
rmSync(join(stage, 'node_modules', 'backend-api'), { recursive: true, force: true });

if (process.platform === 'win32') {
  run('tar', ['-a', '-c', '-f', zipFile, 'functions', 'node_modules', 'package.json'], stage);
} else {
  run('zip', ['-qr', zipFile, '.'], stage);
}

console.log(`Created ${zipFile}`);

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    stdio: 'inherit',
    shell: process.platform === 'win32'
  });

  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed`);
  }
}
