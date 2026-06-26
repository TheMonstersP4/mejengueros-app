## Apply Progress: issue-15-court-catalog

### Status
- Mode: Strict TDD
- Work unit: Frontend catalog first slice + real backend/frontend catalog integration
- Scope guard: issue #15 only; detail/reservation remain deferred

### Completed Tasks
- [x] Add a demo-backed catalog repository seam and shared ViewModel with search + province/canton filtering
- [x] Replace the authenticated Home placeholder with a catalog screen using existing Mejengueros cards, status pills, and option chips
- [x] Add typed navigation handoff to a pending detail route without implementing issue #16 detail behavior
- [x] Add focused JVM tests for catalog filtering state and Home-stack detail routing
- [x] Address Judgment Day blockers with explicit catalog error handling, retry coverage, closer mockup styling, and a visible secondary Create Complex access
- [x] Add the public backend `/v1/courts/catalog` module with DTOs, query validation, Prisma catalog repository, and Swagger/OpenAPI coverage
- [x] Add explicit Prisma publication flags plus migration/seed updates so the catalog can filter published complexes and courts honestly
- [x] Replace production DI wiring of `DemoCourtCatalogRepository` with a real Ktor remote datasource and repository backed by `/v1/courts/catalog`
- [x] Adapt shared catalog models/UI copy to real API fields (`services`, `rating`, province/canton ids) without inventing `courtType` or demo claims
- [x] Add focused backend + frontend tests for the real catalog contract, repository wiring, and public endpoint behavior

### TDD Cycle Evidence
| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| Catalog filtering ViewModel | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt` | Unit | N/A (new) | ✅ Written before production types/ViewModel existed | ✅ `:shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest"` | ✅ 3 cases: initial visibility, search, province/canton reset | ✅ Extracted pure state builders/filter helpers |
| Home detail handoff route | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/navigation/AuthenticatedNavigationStateTest.kt` | Unit | ✅ Baseline `:shared:jvmTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedNavigationStateTest"` passed before edits | ✅ Added failing route expectation first | ✅ Same focused JVM test command passed after route implementation | ✅ Existing route coverage + new detail route case | ➖ None needed |
| Judgment Day catalog hardening | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt`, `shared/src/androidHostTest/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreenBehaviorTest.kt` | Unit + host UI | ✅ Existing catalog/detail tests already covered the first slice | ✅ Added retry/error coverage in the ViewModel plus host-screen smoke coverage for the hardened Home states | ✅ Focused verification now proves ViewModel fallback and that the revised Home catalog states render on Android host tests | ✅ Kept the current ViewModel-bound Home contract while tightening copy and layout |
| Public court catalog backend | `app-backend/api/test/unit/modules/catalogs.spec.ts`, `app-backend/api/test/integration/courts-http.contract.spec.ts`, `app-backend/api/test/integration/openapi-document.contract.spec.ts` | Unit + integration | ⚠️ Focused safety net was not captured before the first edit; post-implementation focused suites passed with no unrelated failures | ✅ Wrote endpoint/repository contract tests before implementing the new `courts` module and `/v1/courts/catalog` controller | ✅ `npm test -- --runInBand test/unit/modules/catalogs.spec.ts test/integration/courts-http.contract.spec.ts test/integration/openapi-document.contract.spec.ts` | ✅ Covered public success path, invalid province/canton filter, repository query shape, and OpenAPI envelope schema | ✅ Extracted a dedicated courts module/repository and kept HTTP validation at the feature boundary |
| Publication flags and migration contract | `app-backend/api/test/integration/prisma-relational-schema.contract.spec.ts` | Integration | N/A (new migration contract) | ✅ Added failing schema contract expectations for `isPublished` before changing Prisma schema/migration | ✅ `npm test -- --runInBand test/integration/prisma-relational-schema.contract.spec.ts` | ✅ Validated both Complex and Court flags plus SQL migration fragments | ➖ No extra refactor needed beyond small schema/seed updates |
| Frontend real catalog integration | `app-frontend/shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/data/remote/CourtCatalogRemoteDataSourceTest.kt`, `app-frontend/shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/data/repository/CourtCatalogRepositoryTest.kt`, `app-frontend/shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt`, `app-frontend/shared/src/androidHostTest/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreenBehaviorTest.kt` | Unit + host UI | ⚠️ Existing Home/ViewModel safety net was executed after the first edit as focused verification; no unrelated failures were found | ✅ Added remote datasource/repository tests before wiring production DI and updating shared catalog models | ✅ `./gradlew spotlessCheck :shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest" --tests "io.github.themonstersp4.mejengueros.data.remote.CourtCatalogRemoteDataSourceTest" --tests "io.github.themonstersp4.mejengueros.data.repository.CourtCatalogRepositoryTest" --no-configuration-cache --console=plain` | ✅ Covered API envelope mapping, error mapping, repository delegation, and updated local filtering over real province/canton names | ✅ Removed demo-only DI, kept filtering behavior local, and derived card metadata from real services/rating fields |

### Verification
- ✅ `./gradlew spotlessApply --no-configuration-cache --console=plain`
- ✅ `./gradlew spotlessCheck :shared:testAndroidHostTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedScaffoldBehaviorTest" --tests "io.github.themonstersp4.mejengueros.screens.home.HomeScreenBehaviorTest" --no-configuration-cache --console=plain`
- ✅ `./gradlew :shared:jvmTest --tests "io.github.themonstersp4.mejengueros.navigation.AuthenticatedNavigationStateTest" --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest" --no-configuration-cache --console=plain`
- ✅ `npm run prisma:validate`
- ✅ `npm test -- --runInBand test/unit/modules/catalogs.spec.ts test/unit/modules/modules.spec.ts test/integration/courts-http.contract.spec.ts test/integration/openapi-document.contract.spec.ts test/integration/prisma-relational-schema.contract.spec.ts`
- ✅ `npm run lint`
- ✅ `./gradlew spotlessCheck :shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest" --tests "io.github.themonstersp4.mejengueros.data.remote.CourtCatalogRemoteDataSourceTest" --tests "io.github.themonstersp4.mejengueros.data.repository.CourtCatalogRepositoryTest" --no-configuration-cache --console=plain`
- ✅ `./gradlew :shared:testAndroidHostTest --tests "io.github.themonstersp4.mejengueros.screens.home.HomeScreenBehaviorTest" --no-configuration-cache --console=plain`

### Follow-up Adjustment
- ✅ Canceled the catalog-only shell alignment so the authenticated scaffold no longer pretends `Buscar / Reservas / Notificaciones`; the catalog remains centered in `HomeScreen` content.

### Notes
- The catalog now uses a real public backend endpoint (`GET /v1/courts/catalog`) and production DI no longer points to `DemoCourtCatalogRepository`.
- The later shell-alignment experiment was canceled: authenticated navigation stays generic (`Mejengueros` plus `Home / Kit / Pokédex`) and the honest catalog framing lives inside `HomeScreen` instead of the shell.
- The `Crear complejo` entrypoint now stays visible as a secondary owner/admin action under the catalog filters so Home keeps the mejenguero-first catalog focus without orphaning the existing create flow.
- The backend now adds explicit `isPublished` flags to `Complex` and `Court`; create-complex flow still defaults to unpublished so issue #15 stays catalog-only and does not absorb owner publishing workflows.
