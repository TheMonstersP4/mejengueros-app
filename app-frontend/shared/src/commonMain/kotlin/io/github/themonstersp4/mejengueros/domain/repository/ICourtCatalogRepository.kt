package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage

interface ICourtCatalogRepository {
  suspend fun getCatalogCourts(
      searchQuery: String? = null,
      provinceId: String? = null,
      cantonId: String? = null,
      page: Int = 1,
      pageSize: Int = CourtCatalogPage.DEFAULT_PAGE_SIZE,
  ): CourtCatalogPage
}
