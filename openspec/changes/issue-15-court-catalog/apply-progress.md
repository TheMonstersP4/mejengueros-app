## Apply Progress: issue-15-court-catalog

### Status
- Mode: Strict TDD
- Work unit: Frontend catalog first slice
- Scope guard: issue #15 only; detail/reservation remain deferred

### Completed Tasks
- [x] Add a demo-backed catalog repository seam and shared ViewModel with search + province/canton filtering
- [x] Replace the authenticated Home placeholder with a catalog screen using existing Mejengueros cards, status pills, and option chips
- [x] Add typed navigation handoff to a pending detail route without implementing issue #16 detail behavior
- [x] Add focused JVM tests for catalog filtering state and Home-stack detail routing
- [x] Address Judgment Day blockers with explicit catalog error handling, retry coverage, closer mockup styling, and a visible secondary Create Complex access

### TDD Cycle Evidence
| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| Catalog filtering ViewModel | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt` | Unit | N/A (new) | ✅ Written before production types/ViewModel existed | ✅ `:shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest"` | ✅ 3 cases: initial visibility, search, province/canton reset | ✅ Extracted pure state builders/filter helpers |
| Home detail handoff route | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/navigation/AuthenticatedNavigationStateTest.kt` | Unit | ✅ Baseline `:shared:jvmTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedNavigationStateTest"` passed before edits | ✅ Added failing route expectation first | ✅ Same focused JVM test command passed after route implementation | ✅ Existing route coverage + new detail route case | ➖ None needed |
| Judgment Day catalog hardening | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt`, `shared/src/androidHostTest/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreenBehaviorTest.kt` | Unit + host UI | ✅ Existing catalog/detail tests already covered the first slice | ✅ Added retry/error coverage in the ViewModel plus host-screen smoke coverage for the hardened Home states | ✅ Focused verification now proves ViewModel fallback and that the revised Home catalog states render on Android host tests | ✅ Kept the current ViewModel-bound Home contract while tightening copy and layout |

### Verification
- ✅ `./gradlew spotlessApply --no-configuration-cache --console=plain`
- ✅ `./gradlew spotlessCheck :shared:testAndroidHostTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedScaffoldBehaviorTest" --tests "io.github.themonstersp4.mejengueros.screens.home.HomeScreenBehaviorTest" --no-configuration-cache --console=plain`
- ✅ `./gradlew :shared:jvmTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedNavigationStateTest" --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest" --no-configuration-cache --console=plain`

### Follow-up Adjustment
- ✅ Canceled the catalog-only shell alignment so the authenticated scaffold no longer pretends `Buscar / Reservas / Notificaciones`; the catalog remains centered in `HomeScreen` content.

### Notes
- The current backend/frontend contract still lacks a real public catalog endpoint, so this slice isolates demo catalog data behind `ICourtCatalogRepository` rather than pretending there is live integration.
- The later shell-alignment experiment was canceled: authenticated navigation stays generic (`Mejengueros` plus `Home / Kit / Pokédex`) and the honest catalog framing lives inside `HomeScreen` instead of the shell.
- The `Crear complejo` entrypoint now stays visible as a secondary owner/admin action under the catalog filters so Home keeps the mejenguero-first catalog focus without orphaning the existing create flow.
