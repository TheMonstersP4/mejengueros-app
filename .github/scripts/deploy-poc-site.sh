#!/usr/bin/env bash
set -euo pipefail

if [ -z "${POC_SITE_BUCKET:-}" ]; then
  echo "POC_SITE_BUCKET is empty. Enable the POC site in Terraform first." >&2
  exit 1
fi

runtime_config="$(jq -n \
  --arg cognitoDomain "${COGNITO_DOMAIN_URL:-}" \
  --arg clientId "${COGNITO_CLIENT_ID:-}" \
  --arg apiBaseUrl "${HTTP_API_ENDPOINT:-}" \
  --arg websocketUrl "${WEBSOCKET_URL:-}" \
  '{
    cognitoDomain: ($cognitoDomain | if length > 0 then . else null end),
    clientId: ($clientId | if length > 0 then . else null end),
    apiBaseUrl: ($apiBaseUrl | if length > 0 then . else null end),
    websocketUrl: ($websocketUrl | if length > 0 then . else null end)
  }')"

printf 'window.MEJENGUEROS_CONFIG = %s;\n' "$runtime_config" > app-backend/poc/web-chat/runtime-config.js

aws s3 sync app-backend/poc/web-chat/ "s3://${POC_SITE_BUCKET}/" \
  --delete \
  --exclude "README.md" \
  --cache-control "public,max-age=60"

echo "POC deployed to ${POC_SITE_URL:-s3://${POC_SITE_BUCKET}}"
