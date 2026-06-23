package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex

data class CreateComplexUiState(
    val complexName: String = "",
    val complexAddress: String = "",
    val firstCourtName: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val createdComplex: CreatedComplex? = null,
) {
  val canSubmit: Boolean
    get() =
        !isSubmitting &&
            complexName.isNotBlank() &&
            complexAddress.isNotBlank() &&
            firstCourtName.isNotBlank()
}
