#!/usr/bin/env bash
set -euo pipefail

if [ -z "${ECR_REPOSITORY_URI:-}" ]; then
  echo "ECR_REPOSITORY_URI is empty." >&2
  exit 1
fi

region="${AWS_REGION:-us-east-2}"
registry="${ECR_REPOSITORY_URI%%/*}"
tag="${GITHUB_SHA:-local}"
image_uri="${ECR_REPOSITORY_URI}:${tag}"

if [ -n "${DATABASE_SECRET_ARN:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  aws secretsmanager put-secret-value \
    --secret-id "$DATABASE_SECRET_ARN" \
    --secret-string "$DATABASE_URL" \
    --region "$region" \
    --query 'ARN' \
    --output text >/dev/null
  echo "Database secret updated."
elif [ -n "${DATABASE_SECRET_ARN:-}" ]; then
  echo "DATABASE_SECRET_ARN is configured, but DATABASE_URL is empty. Skipping database secret update."
fi

aws ecr get-login-password --region "$region" | docker login --username AWS --password-stdin "$registry"

docker build -f app-backend/api/Dockerfile -t "$image_uri" -t "${ECR_REPOSITORY_URI}:latest" app-backend/api
docker push "$image_uri"
docker push "${ECR_REPOSITORY_URI}:latest"

if [ -z "${API_LAMBDA_FUNCTION_NAME:-}" ]; then
  echo "API_LAMBDA_FUNCTION_NAME is empty. Image pushed, skipping Lambda update."
  exit 0
fi

if ! aws lambda get-function --function-name "$API_LAMBDA_FUNCTION_NAME" --region "$region" >/dev/null 2>&1; then
  echo "Lambda ${API_LAMBDA_FUNCTION_NAME} does not exist yet. Image pushed; apply Terraform with ${ECR_REPOSITORY_URI}:latest to create it."
  exit 0
fi

aws lambda update-function-code \
  --function-name "$API_LAMBDA_FUNCTION_NAME" \
  --image-uri "$image_uri" \
  --region "$region" \
  --query '{FunctionName:FunctionName,LastModified:LastModified,LastUpdateStatus:LastUpdateStatus}' \
  --output text

aws lambda wait function-updated \
  --function-name "$API_LAMBDA_FUNCTION_NAME" \
  --region "$region"
