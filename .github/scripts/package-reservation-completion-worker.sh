#!/usr/bin/env bash
set -euo pipefail

cd app-backend/api

npm ci
npm run build
npm run lambda:package:reservation-completion
