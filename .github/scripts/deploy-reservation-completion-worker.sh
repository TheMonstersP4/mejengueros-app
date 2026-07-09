#!/usr/bin/env bash
set -euo pipefail

zip_file="app-backend/api/.lambda/reservation-completion.zip"
region="${AWS_REGION:-us-east-2}"
function_name="${RESERVATION_COMPLETION_WORKER_FUNCTION_NAME:-}"

if [ ! -f "$zip_file" ]; then
  echo "Missing $zip_file. Run package-reservation-completion-worker.sh first." >&2
  exit 1
fi

if [ -z "$function_name" ]; then
  echo "RESERVATION_COMPLETION_WORKER_FUNCTION_NAME is empty. Reservation completion worker deploy skipped."
  exit 0
fi

if ! aws lambda get-function --function-name "$function_name" --region "$region" >/dev/null 2>&1; then
  echo "Lambda function $function_name does not exist yet. Apply Terraform with reservation_completion_worker_enabled=true first." >&2
  exit 1
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
