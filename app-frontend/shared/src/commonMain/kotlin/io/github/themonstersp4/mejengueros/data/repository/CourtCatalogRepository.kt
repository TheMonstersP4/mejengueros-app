package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
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
      courtIds: List<String>,
      minRating: Int?,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage =
      remoteDataSource.getCatalogCourts(
          searchQuery = searchQuery,
          provinceId = provinceId,
          cantonId = cantonId,
          serviceIds = serviceIds,
          courtIds = courtIds,
          minRating = minRating,
          page = page,
          pageSize = pageSize,
      )

  override suspend fun getServiceCatalog(): List<ServiceCatalogItem> =
      remoteDataSource.getServiceCatalog()

  override suspend fun getFavoriteCourts(courtIds: List<String>): List<CourtCatalogItem> {
    val uniqueCourtIds = courtIds.distinct()
    if (uniqueCourtIds.isEmpty()) return emptyList()

    val courtsById =
        uniqueCourtIds
            .chunked(FAVORITE_COURTS_BATCH_SIZE)
            .flatMap { batch ->
              remoteDataSource
                  .getCatalogCourts(courtIds = batch, page = 1, pageSize = batch.size)
                  .items
            }
            .associateBy { it.id }

    return uniqueCourtIds.mapNotNull(courtsById::get)
  }

  private companion object {
    const val FAVORITE_COURTS_BATCH_SIZE = 20
  }
}
