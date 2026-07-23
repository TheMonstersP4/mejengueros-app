package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem

interface ICourtCatalogRemoteDataSource {
  suspend fun getCatalogCourts(
      searchQuery: String? = null,
      provinceId: String? = null,
      cantonId: String? = null,
      serviceIds: List<String> = emptyList(),
      page: Int = 1,
      pageSize: Int = CourtCatalogPage.DEFAULT_PAGE_SIZE,
  ): CourtCatalogPage

  /** Fetches every active service so the catalog can offer them as a filter option. */
  suspend fun getServiceCatalog(): List<ServiceCatalogItem>
}
