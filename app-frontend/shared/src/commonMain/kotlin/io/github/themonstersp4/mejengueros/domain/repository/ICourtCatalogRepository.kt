package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem

interface ICourtCatalogRepository {
  suspend fun getCatalogCourts(
      searchQuery: String? = null,
      provinceId: String? = null,
      cantonId: String? = null,
      serviceIds: List<String> = emptyList(),
      minRating: Int? = null,
      page: Int = 1,
      pageSize: Int = CourtCatalogPage.DEFAULT_PAGE_SIZE,
  ): CourtCatalogPage

  /**
   * Returns the active services offered across the catalog so the search screen can present them as
   * a filter. Defaults to an empty list so fakes that only exercise court listing stay unaffected.
   */
  suspend fun getServiceCatalog(): List<ServiceCatalogItem> = emptyList()
}
