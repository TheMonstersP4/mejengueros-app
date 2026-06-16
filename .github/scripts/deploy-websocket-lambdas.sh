#!/usr/bin/env bash
set -euo pipefail

zip_file="app-backend/api/.lambda/websocket.zip"
region="${AWS_REGION:-us-east-2}"

if [ ! -f "$zip_file" ]; then
  echo "Missing $zip_file. Run package-websocket-lambdas.sh first." >&2
  exit 1
fi

functions=(
  "${WEBSOCKET_CONNECT_FUNCTION_NAME:-}"
  "${WEBSOCKET_DISCONNECT_FUNCTION_NAME:-}"
  "${WEBSOCKET_DEFAULT_FUNCTION_NAME:-}"
)

deployed=0

for function_name in "${functions[@]}"; do
  if [ -z "$function_name" ]; then
    continue
  fi

  aws lambda update-function-code \
    --function-name "$function_name" \
    --zip-file "fileb://${zip_file}" \
    --region "$region" \
    --query '{FunctionName:FunctionName,LastModified:LastModified,LastUpdateStatus:LastUpdateStatus}' \
    --output text

  aws lambda wait function-updated \
    --function-name "$function_name" \
    --region "$region"

  deployed=$((deployed + 1))
done

if [ "$deployed" -eq 0 ]; then
  echo "No WebSocket Lambda names found. Enable websocket lambdas in Terraform first." >&2
  exit 1
fi
