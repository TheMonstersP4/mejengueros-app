# mejengueros-app

Official development repository for **Mejengueros**, the TheMonstersP4 team project. This repository brings together the backend implementation, the initial mobile/frontend application foundation, and the operational documentation that connects the course backlog with GitHub Projects.

## Current status

The project is in the initial MVP integration stage. The current priority is to consolidate the technical foundation so the team can move forward on bookings, courts, availability, catalog flows, and the demo path.

The operational tracking source is the team GitHub Project:

- Organization: [`TheMonstersP4`](https://github.com/orgs/TheMonstersP4)
- Project: [`Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1)
- Repository linked to issues: [`TheMonstersP4/mejengueros-app`](https://github.com/TheMonstersP4/mejengueros-app)
- Current milestone: [`Semana 10 — Flujo MVP`](https://github.com/TheMonstersP4/mejengueros-app/milestone/1)

## Main structure

| Path | Purpose |
|------|-----------|
| `app-backend/` | Backend, infrastructure, and deployment. |
| `app-frontend/` | Initial mobile/frontend application foundation for the project. |
| `docs/design/` | Visual and technical design artifacts, such as mockups and database diagrams. |
| `openspec/` | Formal SDD/OpenSpec changes when the team uses that workflow. |
| `.github/` | Repository workflows and automation configuration. |

## Internal guides by subproject

Before working in any area, review its internal README:

| Area | When to read it | Reference |
|------|---------------|------------|
| Backend | If you will touch the API, infrastructure, deployment, or the web POC. | [`app-backend/README.md`](app-backend/README.md) |
| Frontend | If you will run the KMP app, review the architecture, or use development commands. | [`app-frontend/README.md`](app-frontend/README.md) |
| Design | If you need functional context, mockups, or support diagrams. | [`docs/design/README.md`](docs/design/README.md) |

## How to work by area

### Backend

The backend lives in `app-backend/`.

- `app-backend/api`: NestJS API with Fastify, Prisma, and WebSocket lambdas.
- `app-backend/infra`: AWS, Cognito, API Gateway, S3, ECR, and Lambda Terraform.
- `.github`: deployment workflows and scripts adjusted to run the backend from `app-backend`.

For more technical details, review [`app-backend/README.md`](app-backend/README.md).

### Mobile/frontend application

The initial mobile application foundation lives in `app-frontend/`.

This foundation works as the technical starting point for the MVP frontend. It may include sample code, example screens, or temporary flows that come from a template. That content should be treated as initial scaffolding, not as the product's final functional scope.

The goal of this initial integration is to leave a reviewable foundation so the team can:

- run and validate the frontend project;
- review the initial technical structure;
- progressively connect real Mejengueros flows;
- replace template examples with project domain cases.

To work on the frontend, run commands from `app-frontend/`. The recommended interface is that subproject's `Taskfile.yml` (`task check`, `task test`, `task verify`, `task spotless:apply`, `task spotless:check`), while raw Gradle commands remain the fallback when Task is not available.

For more technical details, review [`app-frontend/README.md`](app-frontend/README.md).

### Design and functional documentation

Visual and technical design artifacts live in `docs/design/`. That folder centralizes references such as mockups, diagrams, and visual or structural decisions that help explain the product before touching code.

The entry guide for that folder is [`docs/design/README.md`](docs/design/README.md).

## Work tracking

The project's operational tracking happens in [GitHub Projects](https://github.com/orgs/TheMonstersP4/projects/1) and [issues](https://github.com/TheMonstersP4/mejengueros-app/issues). Tasks, priorities, scope decisions, and links between pending work and Pull Requests should live there.

The repository's versioned documentation should complement that tracking, not replace it. In general:

| Space | Responsibility |
|---------|-----------------|
| [GitHub Project `Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1) | Operational backlog, priority, status, estimates, and MVP traceability. |
| [Repository issues](https://github.com/TheMonstersP4/mejengueros-app/issues) | Work description, acceptance criteria, and discussion before the Pull Request. |
| [`docs/design/`](docs/design/) | Mockups, diagrams, and design artifacts. |
| [`openspec/`](openspec/) | Formal change specifications when SDD/OpenSpec is used. |
| Pull requests | Implementable changes linked to an issue or a decision accepted by the team. |

## Branch workflow and parallel work

Every change must start from `main` in its own focused branch. If you need to work on more than one feature at the same time, do not reuse the same directory or mix unrelated changes: use `git worktree` to keep each feature isolated.

The branch convention is `type/<github-username>/<descripcion>`, for example `feat/ddgutierrezc/reservas`. Every Pull Request must request review from `@kevinah95`; in addition, `CODEOWNERS` covers the entire repository to reinforce this rule when branch protection requires code owner reviews.

Recommended path for parallel work:

```powershell
git switch main
git pull
git worktree add ..\mejengueros-app-<feature> -b <tipo>/<github-username>/<descripcion> main
```

Example:

```powershell
git worktree add ..\mejengueros-app-reservas -b feat/ddgutierrezc/reservas main
```

Practical rules:

- one branch per issue, fix, or feature;
- each worktree must point to a different branch;
- always use the `type/<github-username>/<descripcion>` pattern with a valid `type` (`feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert`);
- before opening a Pull Request, verify that the change is linked to an approved issue;
- request `@kevinah95` as reviewer on every Pull Request;
- do not mix documentation, infrastructure, backend, and frontend in the same branch unless they are part of the same reviewable change.

## Recommended reading

- `app-backend/README.md`: backend technical guide, API, infrastructure, and deployment.
- `app-frontend/README.md`: frontend technical guide and work commands.
- `docs/design/README.md`: visual and technical design references.

## Editorial convention

README documentation and Pull Request titles/bodies must be written in clear English.

Technical identifiers may stay in ASCII when the implementation requires it, such as variable names, JSON fields, routes, endpoints, files, environment variables, or configuration keys.

Widely accepted technical terms are also allowed when they are clearer than a forced translation, such as `frontend`, `backend`, `seed`, `wireframes`, `roadmap`, `backlog`, `Social Login`, `end-to-end`, `MVP`, `QR`, and `post-MVP`.

## Current MVP scope

The active MVP backlog lives in the [GitHub Project `Mejengueros`](https://github.com/orgs/TheMonstersP4/projects/1) and in the milestone [`Semana 10 — Flujo MVP`](https://github.com/TheMonstersP4/mejengueros-app/milestone/1). From a product perspective, the critical path is focused on:

- project technical setup;
- complex, court, services, and bookable availability;
- catalog, detail, and 1-hour slot booking;
- minimal database seed for a visible demo;
- post-booking notification;
- basic review, visible rating, and simple owner-facing review reading.

Features such as social login, landing pages, profile/favorites/photo, global admin, QR/code, review images, questionnaires, and advanced metrics remain outside the initial critical path unless the team explicitly reschedules them in the [Project](https://github.com/orgs/TheMonstersP4/projects/1).
