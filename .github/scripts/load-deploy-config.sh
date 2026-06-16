#!/usr/bin/env bash
set -euo pipefail

if [ -z "${DEPLOY_CONFIG:-}" ]; then
  echo "DEPLOY_CONFIG is not set. Store terraform output deploy_config as DEPLOY_DEV_CONFIG." >&2
  exit 1
fi

config_file="$(mktemp)"
printf '%s' "$DEPLOY_CONFIG" > "$config_file"

for key in $(jq -r 'keys[]' "$config_file"); do
  value="$(jq -r --arg key "$key" '.[$key] // empty' "$config_file")"
  echo "${key}=${value}" >> "$GITHUB_ENV"
done

rm -f "$config_file"
