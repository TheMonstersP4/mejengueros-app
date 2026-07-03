package io.github.themonstersp4.mejengueros.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CourtCatalogItemTest {
  @Test
  fun displayTitleUsesCourtNameInsteadOfComplexContext() {
    val item =
        CourtCatalogItem(
            id = "court-1",
            complexId = "complex-1",
            complexName = "Monsters inc",
            courtName = "cancha1",
            provinceId = "province-1",
            provinceName = "San José",
            cantonId = "canton-1",
            cantonName = "San José",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            isReservableToday = false,
        )

    assertEquals("cancha1", item.displayTitle)
  }

  @Test
  fun displayTitlePreservesCourtNameWhenComplexMatchesCourtPrefix() {
    val item =
        CourtCatalogItem(
            id = "court-2",
            complexId = "complex-2",
            complexName = "Cancha Central Sports",
            courtName = "Cancha Central",
            provinceId = "province-1",
            provinceName = "Heredia",
            cantonId = "canton-1",
            cantonName = "Belén",
            services = emptyList(),
            ratingAverage = 4.7,
            ratingCount = 18,
            imageUrl = null,
            isReservableToday = true,
        )

    assertEquals("Cancha Central", item.displayTitle)
  }
}
