package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

interface ICourtCatalogRepository {
  suspend fun getCatalogCourts(
      searchQuery: String? = null,
      provinceId: String? = null,
      cantonId: String? = null,
  ): List<CourtCatalogItem>
}
