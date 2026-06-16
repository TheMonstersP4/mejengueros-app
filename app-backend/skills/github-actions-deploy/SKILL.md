---
name: github-actions-deploy
description: Review and build GitHub Actions deploy pipelines using OIDC, quality gates, Docker image deploys, Lambda zip deploys, and script-driven release steps.
---

# GitHub Actions Deploy

Use this skill when editing `.github/workflows` or deploy scripts.

## Rules

- Keep deploy logic in scripts; keep Actions focused on checkout, auth, routing, and logs.
- Use OIDC roles instead of long-lived AWS keys.
- Add a quality gate before deploy jobs for lint, tests, and build.
- Use the same package script locally and in CI.
- Load environment-specific deploy config from GitHub environment secrets.
- Keep deploy scripts idempotent and fail fast with clear messages.
- Do not echo secrets or generated tokens.

## Validation

- Run the package script locally when Lambda zip shape changes.
- Run API lint, tests, and build before changing deploy jobs.
- Check workflow dependencies so deploy jobs cannot bypass quality gates.
