# Terraform Documentation Reference

Module files:

```text
versions.tf
variables.tf
main.tf
outputs.tf
README.md
```

Rules:

- `versions.tf`: no comments, only Terraform and provider constraints.
- `variables.tf`: every variable must include `description`.
- `main.tf`: comments only for cost, security, or non-obvious behavior.
- `outputs.tf`: every output must include `description`.
- `README.md`: generated with terraform-docs when available, or written by hand with Resources, Inputs, Outputs.
- Terraform labels must be descriptive. Do not use `this` for resources, data sources, or modules.
