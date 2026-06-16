#!/usr/bin/env bash
set -euo pipefail

if [ -z "${DATABASE_URL:-}" ]; then
  echo "DATABASE_URL is empty. Skipping API database migrations."
  exit 0
fi

cd app-backend/api
npx prisma migrate deploy
