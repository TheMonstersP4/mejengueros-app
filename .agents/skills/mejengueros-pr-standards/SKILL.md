---
name: mejengueros-pr-standards
description: "Trigger: Mejengueros PRs, branch naming, reviewers, GitHub PR creation. Extiende el flujo global branch-pr con reglas locales del repositorio."
license: Apache-2.0
metadata:
  author: mejengueros-team
  version: "1.0"
---

## Activation Contract

Úsalo cuando el trabajo incluya Pull Requests de Mejengueros, nombrado de ramas, selección de reviewers o creación de PRs en GitHub para este repositorio.

## Hard Rules

- Este skill COMPLEMENTA al skill global `branch-pr`; no lo reemplaza.
- Primero sigue el flujo completo de `branch-pr` para issue aprobado, labels `type:*`, cuerpo del PR, conventional commits y validaciones base.
- No crees ni sobrescribas un skill local llamado `branch-pr`.
- Todas las ramas deben usar el formato `type/<github-username>/<description>`.
- Los tipos válidos son `feat|fix|chore|docs|style|refactor|perf|test|build|ci|revert`.
- Todo PR debe mantener el formato Conventional Commit en el título, pero el resumen después de `:` debe escribirse en español.
- Todo cuerpo de PR debe escribirse en español claro, salvo nombres técnicos, comandos, rutas, símbolos, etiquetas y referencias externas.
- Todo PR debe solicitar a `@kevinah95` como reviewer.
- `CODEOWNERS` cubre todos los archivos y reforzará esta revisión cuando la protección de rama exija code owner reviews.

## Decision Gates

| Caso | Regla |
| --- | --- |
| Flujo base del PR | Sigue primero `branch-pr` |
| Rama nueva | Usa `type/<github-username>/<description>` |
| Título del PR | Usa formato `type(scope): resumen en español` |
| Cuerpo del PR | Escríbelo en español claro, respetando términos técnicos necesarios |
| Reviewer obligatorio | Solicita `@kevinah95` |
| Automatización de ownership | Confía en `CODEOWNERS` una vez exista branch protection con code owner reviews |

## Execution Steps

1. Ejecuta primero el workflow del skill global `branch-pr`.
2. Crea o valida la rama con el patrón `type/<github-username>/<description>`.
3. Verifica que `type` pertenezca al conjunto permitido.
4. Redacta el título del PR como `type(scope): resumen en español`, por ejemplo `chore(repo): estandarizar gobernanza de PRs`.
5. Redacta el cuerpo del PR en español claro y conserva en inglés solo nombres técnicos, comandos, rutas, símbolos, labels y referencias externas.
6. Al abrir o editar el PR, solicita a `@kevinah95` como reviewer.
7. Si documentas ejemplos, usa convenciones reales del repo como `feat/ddgutierrezc/issue-48-complex-frontend`, `fix/ddgutierrezc/owner-onboarding` y `chore/ddgutierrezc/readme-contribucion`.

## Output Contract

Reporta si el PR cumple el flujo global `branch-pr`, si la rama cumple el patrón local, si el título y cuerpo están en español y si `@kevinah95` quedó solicitado como reviewer.

## References

- `README.md`
- `CODEOWNERS`
- `.github/workflows/pr-validation.yml`
