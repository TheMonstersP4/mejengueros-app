package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository

class CourtCatalogRepository(
    private val remoteDataSource: ICourtCatalogRemoteDataSource,
) : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage =
      remoteDataSource.getCatalogCourts(
          searchQuery = searchQuery,
          provinceId = provinceId,
          cantonId = cantonId,
          serviceIds = serviceIds,
          page = page,
          pageSize = pageSize,
      )

  override suspend fun getServiceCatalog(): List<ServiceCatalogItem> =
      remoteDataSource.getServiceCatalog()
}
