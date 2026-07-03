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
