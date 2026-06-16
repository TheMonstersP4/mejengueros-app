---
name: repo-documentation
description: Apply repository documentation standards for TypeScript, NestJS, Terraform modules, README files, and public contracts. Use when Codex writes or reviews TSDoc, JSDoc-like comments, Terraform descriptions, module READMEs, or documentation that should avoid noisy comments.
---

# Repo Documentation

Use this skill when documenting TypeScript, NestJS, Terraform, or module README files.

## TypeScript

Prefer TSDoc for exported contracts:

- Use cases.
- Ports.
- Entities.
- Value objects.
- Public DTOs with business meaning.
- Public methods with non-obvious behavior.

Use these tags only when they add context:

```text
@remarks
@param
@returns
@throws
@example
@packageDocumentation
```

Do not add file headers everywhere. File headers are useful for bootstrap files, package entry points, compatibility adapters, and files with non-obvious lifecycle or security behavior.

Avoid comments that repeat the code.

## Terraform

Follow this pattern:

```text
versions.tf   -> no comments, only providers
variables.tf  -> every variable has description
main.tf       -> comments only for cost, security, or non-obvious behavior
outputs.tf    -> every output has description
README.md     -> terraform-docs or equivalent manual sections
```

Use descriptive Terraform labels. Do not name resources, data sources, or modules `this`; prefer labels that describe the role inside the module, such as `oauth_client`, `user_pool`, `repository`, `access_logs`, or `connection_table`.

## README Files

Use concise sections:

```text
# Module Name

Short purpose statement.

## Resources
## Inputs
## Outputs
```

## Reference

Read these only when needed:

- `references/tsdoc-standards.md`
- `references/terraform-standards.md`
