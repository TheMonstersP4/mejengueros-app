package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem

data class CreateComplexUiState(
    val currentStep: CreateComplexStep = CreateComplexStep.Complex,
    val complexName: String = "",
    val selectedProvinceId: String? = null,
    val provinces: List<Province> = emptyList(),
    val selectedCantonId: String? = null,
    val cantons: List<Canton> = emptyList(),
    val complexAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val complexServices: List<ServiceCatalogItem> = emptyList(),
    val selectedComplexServiceIds: List<String> = emptyList(),
    val firstCourtName: String = "",
    val courtServices: List<ServiceCatalogItem> = emptyList(),
    val selectedCourtServiceIds: List<String> = emptyList(),
    val isLoadingCatalogs: Boolean = true,
    val isLoadingCantons: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasCantonLoadFailure: Boolean = false,
    val loadErrorMessage: String? = null,
    val formErrorMessage: String? = null,
    val successMessage: String? = null,
    val createdComplex: CreatedComplex? = null,
) {
  val errorMessage: String?
    get() = loadErrorMessage ?: formErrorMessage

  val hasCatalogLoadFailure: Boolean
    get() =
        loadErrorMessage != null &&
            provinces.isEmpty() &&
            complexServices.isEmpty() &&
            courtServices.isEmpty()

  val selectedProvince: Province?
    get() = provinces.firstOrNull { it.id == selectedProvinceId }

  val selectedCanton: Canton?
    get() = cantons.firstOrNull { it.id == selectedCantonId }

  val canGoToCourtStep: Boolean
    get() =
        !isLoadingCatalogs &&
            !isLoadingCantons &&
            !isSubmitting &&
            complexName.isNotBlank() &&
            selectedProvinceId != null &&
            selectedCantonId != null &&
            complexAddress.isNotBlank()

  val canSubmit: Boolean
    get() = canGoToCourtStep && firstCourtName.isNotBlank() && selectedCourtServiceIds.isNotEmpty()
}

enum class CreateComplexStep {
  Complex,
  FirstCourt,
}
