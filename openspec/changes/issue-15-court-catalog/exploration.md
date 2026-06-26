## Exploration: issue-15-court-catalog

### Current State
GitHub issue `#15` defines the catalog as the mejenguero entry point for `search -> detail -> reserve`, with basic text/province/canton filtering and only visible/active courts. The visual references live in `docs/design/mockups/issues-15-16-catalogo-detalle/catalogo.html`, `detalle.html`, and `docs/design/mockups/issue-50-reservar/reservar.html`; they show a dark theme, prominent search field, province/canton filter chips, image-first cards with availability badges, rating/services in downstream detail, and a reservation CTA. In the current KMP app there is no court catalog feature yet: authenticated navigation still uses `Home | Kit | Pokédex`, `HomeScreen` is a placeholder, and the only Mejengueros data seam implemented today is the owner complex-creation flow. Reusable UI pieces already exist for cards, status pills, ratings, slot/date selectors, bottom action bars, and empty/success states, but there is no repository, route, ViewModel, or DTO set for court catalog, court detail, availability lookup, or reservation creation. Backend support is also not ready for this flow from the frontend perspective: the current frontend remote layer only calls `/v1/locations`, `/v1/services`, and authenticated `/v1/complexes`.

### Affected Areas
- `docs/design/mockups/issues-15-16-catalogo-detalle/catalogo.html` — source visual contract for search, filters, card density, availability badge, and bottom-nav intent.
- `docs/design/mockups/issues-15-16-catalogo-detalle/detalle.html` — source visual contract for downstream detail, rating, services, slots preview, and reserve CTA.
- `docs/design/mockups/issue-50-reservar/reservar.html` — confirms reservation-entry handoff from detail and reusable date/slot summary patterns.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AppRoute.kt` — needs typed routes for catalog/detail/reservation entry if implemented.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AuthenticatedNavigationState.kt` — current top-level stacks are `Home`, `Kit`, `Pokedex`; catalog must fit here unless shell IA is changed.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AppNavHost.kt` — owns authenticated vs login stack switching and route registration.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AppNavEntries.kt` — the right seam to add catalog/detail route entries and keep screens controller-free.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/navigation/AuthenticatedScaffold.kt` — current bottom bar labels conflict with the mockup’s `Buscar | Reservas | Notificaciones` product IA.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreen.kt` — current placeholder likely becomes the least disruptive catalog host.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/screens/pokedex/PokedexScreen.kt` and `presentation/pokedex/*` — best in-repo example of route entry + ViewModel + list/detail state flow.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/ui/components/MejenguerosContent.kt` — already provides `MejenguerosCourtCard`, thumbnails, pills, and list groups reusable for catalog cards.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/ui/components/MejenguerosReviewComponents.kt` — reusable compact rating/review summary pieces for catalog/detail.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/ui/components/AppDateTimeSelection.kt` and `MejenguerosReservationState.kt` — reusable slot/date/action-bar primitives for detail-to-reservation entry.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/domain/repository/IComplexRepository.kt`, `data/repository/ComplexRepository.kt`, `data/remote/ComplexRemoteDataSource.kt` — current reference seam for remote-first feature data; a new court repository should mirror this structure.
- `app-frontend/shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/di/modules/DataModule.kt` and `PresentationModule.kt` — dependency registration points for new catalog/detail ViewModels and repositories.
- `app-backend/api/src/modules/locations/**`, `service-catalog/**`, `complexes/**` — verified current HTTP surface; no court catalog/detail/reservation endpoint exists yet.

### Approaches
1. **Catalog inside current Home stack** — Replace the placeholder `HomeScreen` experience with a catalog route/content flow, then push court detail and reservation-entry routes inside the existing Home back stack.
   - Pros: best fit with current Navigation 3 state owner, smallest authenticated-shell churn, aligns with current route/content and ViewModel patterns, easier to keep under the review budget.
   - Cons: bottom-nav labels remain mismatched with mockups unless a later shell cleanup happens; backend contracts are still missing.
   - Effort: Medium

2. **Full authenticated-shell IA rewrite now** — Change the top-level shell to product tabs like `Buscar | Reservas | Notificaciones`, then hang catalog/detail/reservation from that new shell.
   - Pros: closest match to mockups and future product direction.
   - Cons: mixes issue `#15` with a broader navigation redesign, displaces current `Kit`/`Pokédex` dev/reference flows, increases reviewer load, and still depends on missing backend endpoints.
   - Effort: High

### Recommendation
Prefer **Approach 1**. Implement the catalog as the real `Home` experience first, with dedicated typed routes such as a catalog root plus pushed detail/reservation-entry routes, state-hoisted screens, and ViewModels backed by a new remote-first court repository. This respects the current repo DNA, keeps navigation ownership in `AppNavEntries.kt`, lets the UI follow the mockups closely without forcing a premature shell rewrite, and isolates the real blocker: missing backend contract/API support for court listing, detail, availability, and reservation creation.

### Risks
- No verified backend endpoint currently serves court catalog/detail/availability/reservation data, so proposal/spec work must either define the dependency explicitly or split UI-only mock data from real integration.
- Mockup bottom navigation (`Buscar | Reservas | Notificaciones`) does not match the current authenticated shell (`Home | Kit | Pokédex`), so scope must state whether that mismatch is deferred or absorbed.
- Issue `#15` acceptance references detail/reservation handoff, but the actual downstream flows belong to `#16`, `#49`, and `#50`; boundaries can blur if proposal scope is not disciplined.
- If image, rating, and service fields are not normalized in one domain model per card/detail, duplicated mapping logic will spread across list/detail screens.

### Ready for Proposal
Yes — but the proposal should explicitly state that frontend implementation is only clean if it either (a) pairs with new backend API contracts for court catalog/detail/availability/reservation, or (b) splits a UI-first slice from a later integration slice.
