package io.github.themonstersp4.mejengueros.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json

class CourtDetailFavoriteIdentityTest {
  @Test
  fun viewModelKeyIsScopedByAuthenticatedUserAndCourt() {
    assertNotEquals(
        courtDetailViewModelKey("user-a", "court-1"),
        courtDetailViewModelKey("user-b", "court-1"),
    )
    assertNotEquals(
        courtDetailViewModelKey("user-a", "court-1"),
        courtDetailViewModelKey("user-a", "court-2"),
    )
  }

  @Test
  fun courtDetailRouteDoesNotSerializeAuthenticatedUserId() {
    val route =
        CatalogCourtDetailRoute(
            courtId = "court-1",
            complexId = "complex-1",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
        )

    val serialized = Json.encodeToString(CatalogCourtDetailRoute.serializer(), route)

    assertFalse("userId" in serialized)
    assertFalse("user-a" in serialized)
  }
}
