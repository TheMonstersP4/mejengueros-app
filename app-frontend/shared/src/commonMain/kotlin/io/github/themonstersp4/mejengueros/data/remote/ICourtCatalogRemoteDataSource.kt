package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

interface ICourtCatalogRemoteDataSource {
  suspend fun getCatalogCourts(
      searchQuery: String? = null,
      provinceId: String? = null,
      cantonId: String? = null,
  ): List<CourtCatalogItem>
}
