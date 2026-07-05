package io.github.themonstersp4.mejengueros.presentation.reservation

data class ReservationContext(
    val courtId: String,
    val complexId: String,
    val complexName: String,
    val courtName: String,
    val provinceName: String = "",
    val cantonName: String = "",
) {
  val locationLabel: String
    get() = listOf(provinceName, cantonName).filter { it.isNotBlank() }.joinToString(" · ")
}
