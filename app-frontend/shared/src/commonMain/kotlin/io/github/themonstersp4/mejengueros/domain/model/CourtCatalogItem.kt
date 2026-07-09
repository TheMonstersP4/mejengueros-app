package io.github.themonstersp4.mejengueros.domain.model

data class CourtCatalogItem(
    val id: String,
    val complexId: String,
    val complexName: String,
    val courtName: String,
    val provinceId: String,
    val provinceName: String,
    val cantonId: String,
    val cantonName: String,
    val services: List<String>,
    val ratingAverage: Double?,
    val ratingCount: Int,
    val imageUrl: String?,
    val isReservableToday: Boolean,
) {
  val displayTitle: String
    get() = courtName
}

/**
 * A single page of the public court catalog together with the pagination metadata the catalog needs
 * to drive incremental (infinite scroll) loading.
 */
data class CourtCatalogPage(
    val items: List<CourtCatalogItem>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
) {
  /** True when at least one more page can be requested after this one. */
  val hasNextPage: Boolean
    get() = page < totalPages

  companion object {
    // Small page so the mobile infinite scroll loads courts in perceptible
    // increments instead of a single large batch.
    const val DEFAULT_PAGE_SIZE: Int = 10

    fun empty(pageSize: Int = DEFAULT_PAGE_SIZE): CourtCatalogPage =
        CourtCatalogPage(
            items = emptyList(),
            page = 1,
            pageSize = pageSize,
            totalItems = 0,
            totalPages = 0,
        )
  }
}
