package io.github.themonstersp4.mejengueros.domain.model

data class CourtCatalogItem(
    val id: String,
    val complexName: String,
    val courtName: String,
    val province: String,
    val canton: String,
    val surface: String,
    val courtType: String,
    val imageUrl: String?,
    val isReservableToday: Boolean,
    val isPublished: Boolean,
    val isActive: Boolean,
) {
  val displayName: String
    get() = "$complexName · $courtName"
}
