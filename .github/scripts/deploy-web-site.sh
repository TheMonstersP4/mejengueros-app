#!/usr/bin/env bash
set -euo pipefail

if [ -z "${POC_SITE_BUCKET:-}" ]; then
  echo "POC_SITE_BUCKET is empty. Enable the static web site in Terraform first." >&2
  exit 1
fi

npm ci --prefix app-web
npm run build --prefix app-web

aws s3 sync app-web/dist/ "s3://${POC_SITE_BUCKET}/" \
  --delete \
  --cache-control "public,max-age=300"

echo "Web site deployed to ${POC_SITE_URL:-s3://${POC_SITE_BUCKET}}"
