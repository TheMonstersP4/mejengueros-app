## Exploration: issue-15-court-catalog

### Current State
Issue `#15` was explored against a broader product journey (`search -> detail -> reserve`), but the implemented PR slice is narrower: a real public catalog entry point with text/province/canton filtering, published-only courts, aggregate rating, reservable-today signal, fixed capped results, and explicit empty/error/retry handling. The visual references in `docs/design/mockups/issues-15-16-catalogo-detalle/catalogo.html`, `detalle.html`, and `docs/design/mockups/issue-50-reservar/reservar.html` were useful discovery inputs, yet only the catalog-facing portions apply to this PR. Court detail, availability lookup, and reservation entry remain separate follow-up concerns. Before this slice, the KMP app had no court catalog feature: authenticated navigation still used `Home | Kit | Pokédex`, `HomeScreen` was a placeholder, and the only Mejengueros data seam implemented was the owner complex-creation flow. Reusable UI pieces already existed for cards, status pills, ratings, and empty/success states, while the backend/frontend integration for the public catalog had to be added from scratch.

### Affected Areas
- `docs/design/mockups/issues-15-16-catalogo-detalle/catalogo.html` — source visual contract for search, filters, card density, and high-level card presentation.
- `docs/design/mockups/issues-15-16-catalogo-detalle/detalle.html` and `docs/design/mockups/issue-50-reservar/reservar.html` — historical discovery inputs only; they informed future follow-up scope but are not delivered here.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreen.kt` — least disruptive host for the delivered catalog slice.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AuthenticatedNavigationState.kt` and `AuthenticatedScaffold.kt` — shell constraints that explain why the catalog stays inside Home instead of rewriting authenticated IA.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/ui/components/MejenguerosContent.kt` — reusable court card pieces used for the real catalog cards.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/ui/components/MejenguerosReviewComponents.kt` — reusable compact rating summary for aggregate catalog rating.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/domain/repository/IComplexRepository.kt`, `data/repository/ComplexRepository.kt`, `data/remote/ComplexRemoteDataSource.kt` — reference seam for the remote-first data flow mirrored by the catalog implementation.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/di/modules/DataModule.kt` and `PresentationModule.kt` — dependency registration points for the delivered catalog repository and ViewModel wiring.
- `app-backend/api/src/modules/courts/**` — new public catalog HTTP surface and persistence logic introduced for this slice.

### Approaches
1. **Catalog inside current Home stack** — Replace the placeholder `HomeScreen` with the real public catalog and keep filtering, cards, aggregate rating, and empty/error/retry states local to that Home experience.
   - Pros: best fit with current Navigation 3 state owner, smallest authenticated-shell churn, aligns with current route/content and ViewModel patterns, and keeps PR #207 reviewable under issue `#15`.
   - Cons: bottom-nav labels remain mismatched with mockups unless a later shell cleanup happens.
   - Effort: Medium

2. **Broader flow expansion in the same PR** — Add court detail, availability lookup, reservation entry, or a new authenticated IA together with the catalog.
   - Pros: closer to the full product journey.
   - Cons: clear scope creep for PR #207, depends on follow-up issues `#16`, `#49`, and `#50`, and would blur review ownership across unrelated runtime changes.
   - Effort: High

### Recommendation
Prefer **Approach 1** and treat it as the delivered slice for PR #207: implement the catalog as the real `Home` experience, backed by a new remote-first public catalog repository and backend endpoint, while explicitly deferring court detail, availability lookup, and reservation entry to follow-up issues. This keeps the OpenSpec history useful without pretending that downstream flows shipped here.

### Risks
- Mockup bottom navigation (`Buscar | Reservas | Notificaciones`) does not match the current authenticated shell (`Home | Kit | Pokédex`), so the spec must record that shell alignment is deferred instead of silently half-implementing it.
- Issue `#15` discovery material references a broader detail/reservation journey, but the actual downstream implementation belongs to `#16`, `#49`, and `#50`; documentation can create false scope claims if that boundary is not explicit.
- If aggregate rating, services, and publication filters are not normalized in one catalog model, duplicated mapping logic can spread across backend and frontend seams.

### Ready for Proposal
Historical note only: yes, the proposal was ready once it was narrowed to a catalog-only slice with real backend/frontend integration. Any future work for court detail, availability, or reservation should be proposed separately against `#16`, `#49`, and `#50` rather than documented as part of PR #207 delivery.
