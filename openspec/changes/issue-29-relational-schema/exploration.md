## Exploration: issue-29-relational-schema

### Current State
GitHub issues `#29` and `#14` define the MVP service scope as services attachable to either `Complejo` or `Cancha`, with a simple closed catalog for Semana 10. The backend Prisma schema currently contains only `User` and `ImageUpload`, so the relational service model is not implemented yet. GitHub is the intended source of truth, but local doc `docs/specs/issue-29-implementar-esquema-relacional-de-datos.md` has drifted from GitHub issue `#29` on sprint naming (`Sprint 3` locally vs `Sprint 2` in GitHub).

### Affected Areas
- `app-backend/api/prisma/schema.prisma` — future schema work will need the agreed MVP service structure.
- `docs/specs/issue-29-implementar-esquema-relacional-de-datos.md` — needs service-model wording aligned to the closed/global catalog decision and sprint drift correction.
- `docs/specs/issue-14-especificar-servicios-parqueo-iluminacion-tipo-de-cesped.md` — should clarify that Semana 10 uses a closed catalog only, without owner-defined services.
- `openspec/changes/issue-29-relational-schema/exploration.md` — exploration record for this change.

### Approaches
1. **Closed global catalog with join tables** — Use `ServiceCatalog` plus `ComplexService` and `CourtService`.
   - Pros: matches the current MVP decision, keeps schema small, preserves clear scope boundaries between complex and court services.
   - Cons: owners cannot define custom services yet.
   - Effort: Low

2. **Extensible custom-service model now** — Add owner-defined/custom services in Semana 10.
   - Pros: more flexible long term.
   - Cons: expands scope early, adds ownership/governance rules, and conflicts with the already made MVP decision.
   - Effort: Medium

### Recommendation
Keep Semana 10 on the closed/global catalog path: `ServiceCatalog(id, name, scope, isActive, createdAt, updatedAt)` with `ComplexService` and `CourtService` as association tables. Document explicitly that owner-defined/custom services are out of scope for Semana 10 and are only a future Semana 13 or post-MVP extension.

### Drift Identified
- **Material drift in issue 29**: GitHub issue `#29` says `Sprint 2`, while local file `docs/specs/issue-29-implementar-esquema-relacional-de-datos.md` says `Sprint 3` in both the re-scope note and acceptance criterion 10.
- **Service decision not yet explicit enough**: both issue `#29` and issue `#14` say “catálogo cerrado simple”, but neither explicitly records the chosen MVP structure (`ServiceCatalog`, `ComplexService`, `CourtService`) nor that custom services are deferred.
- **Issue 14 body is currently aligned**: no material GitHub vs local drift found in issue `#14` content, but it still needs clarification wording so future implementation does not re-open the custom-service question.

### Proposed Wording Targets
- **GitHub issue #29 comment** — add the concrete MVP service-model decision and state that local docs/OpenSpec must align to it.
- **GitHub issue #14 comment** — add only a short clarifying note that Semana 10 remains a closed catalog and custom services are deferred.
- **`docs/specs/issue-29-implementar-esquema-relacional-de-datos.md`** — update business rules, input/output notes, and acceptance wording.
- **`docs/specs/issue-14-especificar-servicios-parqueo-iluminacion-tipo-de-cesped.md`** — update scope/out-of-scope and rules wording.
- **`openspec/changes/issue-29-relational-schema/exploration.md`** — capture the decision now for downstream SDD phases.

### Proposed Wording Snippets (Spanish)
#### GitHub issue #29 comment
"Para Semana 10, el modelo de servicios de `#29` se cierra así: usaremos un catálogo global/cerrado `ServiceCatalog` con campos mínimos `id`, `name`, `scope`, `isActive`, `createdAt`, `updatedAt`. La relación de servicios ofrecidos se resuelve con `ComplexService` para servicios del complejo y `CourtService` para servicios de la cancha. Servicios personalizados definidos por el dueño no entran en Semana 10, quedan como posible extensión de Semana 13/post-MVP. Hay que alinear los docs locales y OpenSpec a esta decisión porque GitHub sigue siendo la fuente de verdad."

#### GitHub issue #14 comment
"Aclaración de alcance para Semana 10: este issue sigue un catálogo cerrado/global de servicios. El dueño podrá seleccionar servicios predefinidos según el alcance (`Complejo` o `Cancha`), pero no crear servicios personalizados en esta fase MVP. Esa extensión queda diferida para Semana 13/post-MVP."

#### Local doc `issue-29` update
"Para Semana 10, los servicios del esquema relacional deben modelarse con un catálogo global/cerrado `ServiceCatalog` (`id`, `name`, `scope`, `isActive`, `createdAt`, `updatedAt`) y tablas de asociación `ComplexService` y `CourtService` según el alcance. Los servicios personalizados definidos por el dueño no forman parte del MVP y quedan fuera de alcance en esta historia."

#### Local doc `issue-14` update
"El MVP de Semana 10 utiliza un catálogo cerrado de servicios predefinidos. El dueño puede asociar servicios existentes al `Complejo` o a una `Cancha` según corresponda, pero no puede crear servicios personalizados en esta fase."

### Risks
- If the comment/doc updates are skipped, implementation may re-open the custom-services debate during schema work.
- The `Sprint 2` vs `Sprint 3` mismatch can create avoidable confusion during review and acceptance.
- If `scope` semantics are not documented clearly, the schema could drift into duplicate or invalid assignments between complex and court services.

### Ready for Proposal
Yes, after the team records the service decision in GitHub issue `#29` and aligns the local docs wording to that source of truth. No schema implementation should start until that wording is accepted.
