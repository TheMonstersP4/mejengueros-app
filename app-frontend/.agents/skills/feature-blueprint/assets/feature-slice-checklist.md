# Feature Slice Checklist

1. Route: add Navigation 2 route for current repo, or Navigation 3 `NavKey` for target project.
2. UI contract: create `FeatureRoute` for wiring and `FeatureScreen` for stateless rendering.
3. State: define `FeatureUiState` and explicit ViewModel actions.
4. Data: add repository/datasource/cache seams only when the feature needs data.
5. DI: register interfaces and implementations in Koin modules.
6. Tests: cover datasource decoding, repository fallback, mapper behavior, and ViewModel actions.
7. Validation: run focused module tests and formatting checks.
