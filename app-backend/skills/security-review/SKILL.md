---
name: security-review
description: Review project changes for secrets, OAuth provider setup, Cognito, IAM least privilege, logs, public buckets, Cloudflare exposure, and accidental sensitive output.
---

# Security Review

Use this skill before merging auth, infrastructure, deployment, logging, or public endpoint changes.

## Checklist

- No secrets in committed code, docs, examples, logs, or generated output.
- OAuth redirect and logout URLs match the deployed domains.
- Cognito is the token issuer trusted by the API.
- Microsoft personal accounts use the consumers tenant authority when required.
- IAM policies grant only the resources and actions needed.
- S3 public access is intentional and documented for static sites only.
- Logs redact authorization headers, cookies, passwords, OAuth secrets, and tokens.
- Terraform sensitive outputs and variables are marked `sensitive`.
- GitHub Actions uses OIDC and environment secrets.
