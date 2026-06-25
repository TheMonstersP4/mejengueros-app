package io.github.themonstersp4.mejengueros.presentation.complexes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexDetails
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateFirstCourtDetails
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateComplexViewModel(
    private val complexRepository: IComplexRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(CreateComplexUiState())
  val uiState: StateFlow<CreateComplexUiState> = _uiState.asStateFlow()
  private var loadCatalogsJob: Job? = null
  private var loadCantonsJob: Job? = null

  init {
    refreshCatalogs()
  }

  fun updateComplexName(value: String) {
    updateFormState { it.copy(complexName = value) }
  }

  fun selectProvince(provinceId: String) {
    val currentState = _uiState.value
    if (currentState.selectedProvinceId == provinceId) {
      return
    }

    _uiState.value =
        currentState
            .clearFormFeedback()
            .copy(
                selectedProvinceId = provinceId,
                selectedCantonId = null,
                cantons = emptyList(),
                isLoadingCantons = true,
                hasCantonLoadFailure = false,
                loadErrorMessage = null,
            )

    loadCantons(provinceId)
  }

  fun retrySelectedProvinceCantons() {
    val selectedProvinceId = _uiState.value.selectedProvinceId ?: return
    _uiState.value =
        _uiState.value
            .clearFormFeedback()
            .copy(isLoadingCantons = true, cantons = emptyList(), hasCantonLoadFailure = true)

    loadCantons(selectedProvinceId)
  }

  fun selectCanton(cantonId: String) {
    updateFormState { it.copy(selectedCantonId = cantonId) }
  }

  fun updateComplexAddress(value: String) {
    updateFormState { it.copy(complexAddress = value) }
  }

  fun updateSelectedLocation(latitude: Double, longitude: Double) {
    updateFormState { it.copy(latitude = latitude, longitude = longitude) }
  }

  fun clearSelectedLocation() {
    updateFormState { it.copy(latitude = null, longitude = null) }
  }

  fun toggleComplexService(serviceId: String) {
    updateFormState { state ->
      state.copy(selectedComplexServiceIds = state.selectedComplexServiceIds.toggle(serviceId))
    }
  }

  fun updateFirstCourtName(value: String) {
    updateFormState { it.copy(firstCourtName = value) }
  }

  fun toggleCourtService(serviceId: String) {
    updateFormState { state ->
      state.copy(selectedCourtServiceIds = state.selectedCourtServiceIds.toggle(serviceId))
    }
  }

  fun goToFirstCourtStep() {
    val currentState = _uiState.value
    if (!currentState.canGoToCourtStep) {
      _uiState.value =
          currentState.copy(
              formErrorMessage =
                  "Completá nombre del complejo, provincia, cantón y dirección para continuar.",
              successMessage = null,
              createdComplex = null,
          )
      return
    }

    _uiState.value =
        currentState.clearFormFeedback().copy(currentStep = CreateComplexStep.FirstCourt)
  }

  fun goToComplexStep() {
    _uiState.value =
        _uiState.value.clearFormFeedback().copy(currentStep = CreateComplexStep.Complex)
  }

  fun submit() {
    val currentState = _uiState.value
    if (!currentState.canSubmit) {
      _uiState.value =
          currentState.copy(
              formErrorMessage =
                  "Completá el nombre de la primera cancha y elegí al menos un servicio de cancha.",
              successMessage = null,
              createdComplex = null,
          )
      return
    }

    coroutineScope.launch {
      _uiState.value =
          currentState.copy(
              isSubmitting = true,
              hasCantonLoadFailure = false,
              formErrorMessage = null,
              successMessage = null,
          )

      runCatching {
            complexRepository.createComplex(
                CreateComplexRequest(
                    complex =
                        CreateComplexDetails(
                            name = currentState.complexName.trim(),
                            provinceId = currentState.selectedProvinceId.orEmpty(),
                            cantonId = currentState.selectedCantonId.orEmpty(),
                            address = currentState.complexAddress.trim(),
                            latitude = currentState.latitude,
                            longitude = currentState.longitude,
                            serviceIds = currentState.selectedComplexServiceIds,
                        ),
                    firstCourt =
                        CreateFirstCourtDetails(
                            name = currentState.firstCourtName.trim(),
                            serviceIds = currentState.selectedCourtServiceIds,
                        ),
                )
            )
          }
          .onSuccess { createdComplex ->
            _uiState.value =
                currentState.resetAfterSuccess(
                    successMessage = "Complejo y primera cancha creados correctamente.",
                    createdComplex = createdComplex,
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              _uiState.value = currentState.copy(isSubmitting = false)
              return@onFailure
            }

            _uiState.value =
                currentState.copy(
                    isSubmitting = false,
                    formErrorMessage = error.toSubmitUserMessage(),
                    successMessage = null,
                    createdComplex = null,
                )
          }
    }
  }

  fun refreshCatalogs() {
    loadCatalogsJob?.cancel()
    _uiState.value =
        _uiState.value.copy(
            isLoadingCatalogs = true,
            hasCantonLoadFailure = false,
            formErrorMessage = null,
            successMessage = null,
            createdComplex = null,
        )

    loadCatalogsJob =
        coroutineScope.launch {
          runCatching {
                Triple(
                    complexRepository.getProvinces(),
                    complexRepository.getServices(ServiceScope.COMPLEX),
                    complexRepository.getServices(ServiceScope.COURT),
                )
              }
              .onSuccess { (provinces, complexServices, courtServices) ->
                val refreshedState =
                    _uiState.value.revalidatedAfterCatalogRefresh(
                        provinces = provinces,
                        complexServices = complexServices,
                        courtServices = courtServices,
                    )
                _uiState.value = refreshedState
                refreshedState.selectedProvinceId?.let(::loadCantons)
              }
              .onFailure { error ->
                if (error is CancellationException) {
                  return@onFailure
                }

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingCatalogs = false,
                        hasCantonLoadFailure = false,
                        loadErrorMessage = error.toCatalogLoadUserMessage(),
                    )
              }
        }
  }

  private fun loadCantons(provinceId: String) {
    loadCantonsJob?.cancel()
    loadCantonsJob =
        coroutineScope.launch {
          runCatching { complexRepository.getCantons(provinceId) }
              .onSuccess { cantons ->
                if (_uiState.value.selectedProvinceId != provinceId) {
                  return@onSuccess
                }

                _uiState.value =
                    _uiState.value.let { state ->
                      val selectedCantonStillExists =
                          cantons.any { it.id == state.selectedCantonId }
                      state.copy(
                          cantons = cantons,
                          selectedCantonId =
                              state.selectedCantonId?.takeIf { selectedCantonStillExists },
                          isLoadingCantons = false,
                          hasCantonLoadFailure = false,
                          loadErrorMessage = null,
                      )
                    }
              }
              .onFailure { error ->
                if (error is CancellationException) {
                  return@onFailure
                }

                if (_uiState.value.selectedProvinceId != provinceId) {
                  return@onFailure
                }

                _uiState.value =
                    _uiState.value.copy(
                        cantons = emptyList(),
                        selectedCantonId = null,
                        isLoadingCantons = false,
                        hasCantonLoadFailure = true,
                        loadErrorMessage = error.toCantonLoadUserMessage(),
                    )
              }
        }
  }

  private fun updateFormState(update: (CreateComplexUiState) -> CreateComplexUiState) {
    _uiState.value = update(_uiState.value).clearFormFeedback()
  }
}

private fun CreateComplexUiState.clearFormFeedback(): CreateComplexUiState =
    copy(
        formErrorMessage = null,
        successMessage = null,
        createdComplex = null,
    )

private fun CreateComplexUiState.resetAfterSuccess(
    successMessage: String,
    createdComplex: io.github.themonstersp4.mejengueros.domain.model.CreatedComplex,
): CreateComplexUiState =
    CreateComplexUiState(
        provinces = provinces,
        complexServices = complexServices,
        courtServices = courtServices,
        isLoadingCatalogs = false,
        successMessage = successMessage,
        createdComplex = createdComplex,
    )

private fun CreateComplexUiState.revalidatedAfterCatalogRefresh(
    provinces: List<io.github.themonstersp4.mejengueros.domain.model.Province>,
    complexServices: List<io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem>,
    courtServices: List<io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem>,
): CreateComplexUiState {
  val selectedProvinceStillExists = provinces.any { it.id == selectedProvinceId }
  val nextProvinceId = selectedProvinceId?.takeIf { selectedProvinceStillExists }
  val nextCantons = if (selectedProvinceStillExists) cantons else emptyList()
  val selectedCantonStillExists = nextCantons.any { it.id == selectedCantonId }
  val nextCantonId = selectedCantonId?.takeIf { selectedCantonStillExists }
  val availableComplexServiceIds = complexServices.mapTo(mutableSetOf()) { it.id }
  val availableCourtServiceIds = courtServices.mapTo(mutableSetOf()) { it.id }

  return copy(
      provinces = provinces,
      selectedProvinceId = nextProvinceId,
      selectedCantonId = if (nextProvinceId == null) null else nextCantonId,
      cantons = if (nextProvinceId == null) emptyList() else nextCantons,
      complexServices = complexServices,
      selectedComplexServiceIds =
          selectedComplexServiceIds.filter { it in availableComplexServiceIds },
      courtServices = courtServices,
      selectedCourtServiceIds = selectedCourtServiceIds.filter { it in availableCourtServiceIds },
      isLoadingCatalogs = false,
      isLoadingCantons = nextProvinceId != null,
      hasCantonLoadFailure = false,
      loadErrorMessage = null,
      formErrorMessage = null,
  )
}

private fun List<String>.toggle(serviceId: String): List<String> =
    if (contains(serviceId)) {
      filterNot { it == serviceId }
    } else {
      this + serviceId
    }

private fun Throwable.toCatalogLoadUserMessage(): String =
    "No pudimos cargar los catálogos del complejo. Intentá de nuevo."

private fun Throwable.toCantonLoadUserMessage(): String =
    "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo."

private fun Throwable.toSubmitUserMessage(): String =
    when (this) {
      is AppApiException ->
          when {
            statusCode == 401 || statusCode == 403 ->
                "No tenés permisos para crear complejos en este momento."
            statusCode in 400..499 ->
                "No pudimos crear el complejo con la información enviada. Revisá los datos e intentá de nuevo."
            else -> "No pudimos crear el complejo en este momento. Intentá de nuevo."
          }
      else -> "No pudimos crear el complejo en este momento. Intentá de nuevo."
    }
