package io.github.themonstersp4.mejengueros.presentation.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceNamePresentationTest {
  @Test
  fun mapsUnaccentedSurfaceNamesToCanonicalLabels() {
    assertEquals("Sintético", serviceDisplayName("Sintetico"))
    assertEquals("Híbrido", serviceDisplayName("Hibrido"))
    assertEquals("Iluminación", serviceDisplayName("Iluminacion"))
  }

  @Test
  fun keepsAlreadyAccentedNamesUnchanged() {
    assertEquals("Sintético", serviceDisplayName("Sintético"))
    assertEquals("Híbrido", serviceDisplayName("Híbrido"))
  }

  @Test
  fun isCaseInsensitive() {
    assertEquals("Sintético", serviceDisplayName("SINTETICO"))
    assertEquals("Híbrido", serviceDisplayName("hibrido"))
  }

  @Test
  fun leavesCorrectlySpelledNamesWithoutAccentsIntact() {
    assertEquals("Natural", serviceDisplayName("Natural"))
    assertEquals("Parqueo", serviceDisplayName("Parqueo"))
  }

  @Test
  fun returnsUnknownNamesUnchangedAsSafeFallback() {
    assertEquals("Cafetería", serviceDisplayName("Cafetería"))
    assertEquals("Some Custom Service", serviceDisplayName("Some Custom Service"))
  }
}
