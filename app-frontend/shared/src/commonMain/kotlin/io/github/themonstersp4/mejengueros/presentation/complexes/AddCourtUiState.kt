package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem

data class AddCourtUiState(
    val complexName: String,
    val courtName: String = "",
    val courtServices: List<ServiceCatalogItem> = emptyList(),
    val selectedCourtServiceIds: List<String> = emptyList(),
    val selectedCourtImage: LocalCourtImage? = null,
    val isCourtImagePickerAvailable: Boolean = false,
    val isLoadingServices: Boolean = true,
    val isSubmitting: Boolean = false,
    val loadErrorMessage: String? = null,
    val formErrorMessage: String? = null,
    val createdCourt: CreatedCourt? = null,
) {
  val errorMessage: String?
    get() = loadErrorMessage ?: formErrorMessage

  val hasCatalogLoadFailure: Boolean
    get() = loadErrorMessage != null && courtServices.isEmpty()

  val canSubmit: Boolean
    get() =
        !isLoadingServices &&
            !isSubmitting &&
            courtName.isNotBlank() &&
            selectedCourtServiceIds.isNotEmpty()
}
