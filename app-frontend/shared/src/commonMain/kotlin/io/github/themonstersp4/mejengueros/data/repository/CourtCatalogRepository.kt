package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository

class CourtCatalogRepository(
    private val remoteDataSource: ICourtCatalogRemoteDataSource,
) : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> =
      remoteDataSource.getCatalogCourts(
          searchQuery = searchQuery,
          provinceId = provinceId,
          cantonId = cantonId,
      )
}
