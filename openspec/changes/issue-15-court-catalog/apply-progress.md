## Apply Progress: issue-15-court-catalog

### Status
- Mode: Strict TDD
- Work unit: Public court catalog real backend/frontend integration
- Scope guard: issue #15 only; court detail, availability handoff, and reservation entry remain out of scope

### Completed Tasks
- [x] Add a catalog repository seam and shared ViewModel with search plus province/canton filtering by real location ids
- [x] Replace the authenticated Home placeholder with a catalog screen that renders real catalog cards, aggregate rating, reservable-today badge, and fixed-cap results
- [x] Add focused JVM and host-screen coverage for catalog filtering, empty states, error handling, and retry behavior
- [x] Add the public backend `/v1/courts/catalog` module with DTOs, query validation, Prisma catalog repository, and Swagger/OpenAPI coverage
- [x] Add explicit Prisma publication flags plus migration/seed updates so the catalog lists only published complexes and courts
- [x] Replace production DI wiring of `DemoCourtCatalogRepository` with a real Ktor remote datasource and repository backed by `/v1/courts/catalog`
- [x] Adapt shared catalog models/UI copy to real API fields (`services`, aggregated `rating`, province/canton ids) without inventing demo-only fields
- [x] Add focused backend + frontend tests for the real catalog contract, repository wiring, empty responses, and public endpoint behavior
- [x] Fix PR #207 catalog blockers so Home refetches the backend catalog with real `q/provinceId/cantonId`, frontend filters use `{ id, label }`, and the backend enforces the fixed internal cap

### TDD Cycle Evidence
| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| Catalog filtering ViewModel | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt` | Unit | N/A (new) | ✅ Written before production types/ViewModel existed | ✅ `:shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest"` | ✅ Cases cover initial visibility, search, province/canton reset, error, and retry | ✅ Extracted pure state builders/filter helpers |
| Judgment Day catalog hardening | `shared/src/commonTest/kotlin/io/github/themonstersp4/mejengueros/presentation/catalog/CourtCatalogViewModelTest.kt`, `shared/src/androidHostTest/kotlin/io/github/themonstersp4/mejengueros/screens/home/HomeScreenBehaviorTest.kt` | Unit + host UI | ✅ Existing catalog tests covered the first slice | ✅ Added retry/error coverage in the ViewModel plus host-screen smoke coverage for the hardened Home states | ✅ Focused verification now proves ViewModel fallback and that the revised Home catalog states render on Android host tests | ✅ Kept the current ViewModel-bound Home contract while tightening copy and layout |
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
- ✅ `./gradlew spotlessApply --no-configuration-cache --console=plain`
- ✅ `./gradlew spotlessCheck :shared:jvmTest --tests "io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModelTest" --tests "io.github.themonstersp4.mejengueros.data.remote.CourtCatalogRemoteDataSourceTest" --tests "io.github.themonstersp4.mejengueros.data.repository.CourtCatalogRepositoryTest" :shared:testAndroidHostTest --tests "io.github.themonstersp4.mejengueros.screens.home.HomeScreenBehaviorTest" --no-configuration-cache --console=plain`
- ✅ `npm test -- --runInBand test/unit/modules/complexes/complexes.spec.ts test/unit/modules/catalogs.spec.ts test/integration/courts-http.contract.spec.ts`
- ✅ `npm run lint`

### Follow-up Adjustment
- ✅ Canceled the catalog-only shell alignment so the authenticated scaffold no longer pretends `Buscar / Reservas / Notificaciones`; the catalog remains centered in `HomeScreen` content.

### Notes
- The catalog now uses a real public backend endpoint (`GET /v1/courts/catalog`) and production DI no longer points to `DemoCourtCatalogRepository`.
- The later shell-alignment experiment was canceled: authenticated navigation stays generic (`Mejengueros` plus `Home / Kit / Pokédex`) and the honest catalog framing lives inside `HomeScreen` instead of the shell.
- The `Crear complejo` entrypoint now stays visible as a secondary owner/admin action under the catalog filters so Home keeps the mejenguero-first catalog focus without orphaning the existing create flow.
- The create-complex flow now sets `isPublished: true` for both the created complex and its first court so the current owner flow can feed the public catalog without depending on seed/manual SQL.
- This change set does **not** ship court detail, availability lookup, or reservation-entry behavior; those remain follow-up work outside issue #15 (for example `#16`, `#49`, and `#50`).
