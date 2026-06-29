package io.github.themonstersp4.mejengueros.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

class AppNavHostSavedStateConfigurationTest {
  private val json = Json { serializersModule = appNavigationSerializersModule() }

  @Test
  fun serializersModuleSupportsComplexDetailRouteRoundTrip() {
    assertRoundTrip(ComplexDetailRoute(complexId = "complex-id"))
  }

  @Test
  fun serializersModuleSupportsAddCourtRouteRoundTrip() {
    assertRoundTrip(
        AddCourtRoute(
            complexId = "complex-id",
            complexName = "North Sports Center",
        )
    )
  }

  private fun assertRoundTrip(route: AppRoute) {
    val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
    val decoded = json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded)

    assertEquals(route, decoded)
  }
}
