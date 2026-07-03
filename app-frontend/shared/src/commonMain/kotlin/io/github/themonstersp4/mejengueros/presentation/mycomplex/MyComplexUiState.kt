package io.github.themonstersp4.mejengueros.presentation.mycomplex

import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex

data class MyComplexUiState(
    val isLoading: Boolean = false,
    val complexes: List<MyComplexHubComplex> = emptyList(),
    val errorMessage: String? = null,
    val isCourtImagePickerAvailable: Boolean = false,
    val isUpdatingCourtImage: Boolean = false,
    val courtImageErrorMessage: String? = null,
    val courtImageSuccessMessage: String? = null,
) {
  val isEmpty: Boolean
    get() = !isLoading && errorMessage == null && complexes.isEmpty()
}
