package io.github.themonstersp4.mejengueros.presentation.availability

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday

data class CourtAvailabilityUiState(
    val courtId: String = "",
    val courtName: String = "",
    val complexName: String = "",
    val selectedDays: Set<CourtAvailabilityWeekday> = emptySet(),
    val startTime: String = "06:00",
    val endTime: String = "22:00",
    val previewSlots: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
  val appBarTitle: String
    get() = if (courtName.isBlank()) "Disponibilidad" else "Disponibilidad · $courtName"

  val canSave: Boolean
    get() = !isLoading && !isSaving && selectedDays.isNotEmpty() && previewSlots.isNotEmpty()
}
