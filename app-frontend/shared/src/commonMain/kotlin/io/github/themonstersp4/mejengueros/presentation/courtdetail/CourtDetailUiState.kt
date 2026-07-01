package io.github.themonstersp4.mejengueros.presentation.courtdetail

data class CourtDetailUiState(
    val isLoadingSlots: Boolean = true,
    val slots: List<CourtDetailSlot> = emptyList(),
    val slotsErrorMessage: String? = null,
)

data class CourtDetailSlot(
    val displayTime: String,
)
