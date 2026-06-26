package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem

interface ICourtCatalogRepository {
  suspend fun getCatalogCourts(): List<CourtCatalogItem>
}
