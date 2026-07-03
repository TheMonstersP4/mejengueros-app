package io.github.themonstersp4.mejengueros.presentation.mycomplex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtImageUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.NoOpCourtImageUploadRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyComplexViewModel(
    private val repository: IComplexRepository,
    private val imageUploadRepository: ICourtImageUploadRepository =
        NoOpCourtImageUploadRepository(),
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(MyComplexUiState(isLoading = true))
  private var refreshJob: Job? = null
  val uiState: StateFlow<MyComplexUiState> = _uiState.asStateFlow()

  fun refresh() {
    if (refreshJob?.isActive == true) return

    refreshJob =
        coroutineScope.launch {
          _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

          runCatching { repository.getMyComplexHub() }
              .onSuccess { hub ->
                _uiState.value =
                    MyComplexUiState(
                        isLoading = false,
                        complexes = hub.complexes,
                        errorMessage = null,
                        isCourtImagePickerAvailable = _uiState.value.isCourtImagePickerAvailable,
                    )
              }
              .onFailure { error ->
                if (error is CancellationException) {
                  _uiState.value = _uiState.value.copy(isLoading = false)
                  return@onFailure
                }

                errorReporter.reportRecoverableFailure(
                    name = "my_complex_hub_refresh_failed",
                    attributes = error.toReportAttributes(),
                )

                _uiState.value =
                    MyComplexUiState(
                        isLoading = false,
                        complexes = emptyList(),
                        errorMessage = error.toUserMessage(),
                        isCourtImagePickerAvailable = _uiState.value.isCourtImagePickerAvailable,
                        courtImageErrorMessage = _uiState.value.courtImageErrorMessage,
                    )
              }
        }
  }

  fun updateCourtImagePickerAvailability(isAvailable: Boolean) {
    _uiState.value = _uiState.value.copy(isCourtImagePickerAvailable = isAvailable)
  }

  fun acknowledgeCourtImageSuccess() {
    _uiState.value = _uiState.value.copy(courtImageSuccessMessage = null)
  }

  fun updateCourtImage(complexId: String, courtId: String, image: LocalCourtImage) {
    if (_uiState.value.isUpdatingCourtImage) return

    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              isUpdatingCourtImage = true,
              courtImageErrorMessage = null,
              courtImageSuccessMessage = null,
          )

      runCatching {
            val uploadedImage =
                try {
                  imageUploadRepository.uploadCourtImage(image)
                } catch (error: Throwable) {
                  throw CourtImageAssociationUploadFailed(error)
                }

            repository.updateCourtImage(complexId, courtId, uploadedImage.id)
          }
          .onSuccess { court ->
            _uiState.value =
                _uiState.value.copy(
                    complexes = _uiState.value.complexes.replaceCourt(complexId, court),
                    isUpdatingCourtImage = false,
                    courtImageErrorMessage = null,
                    courtImageSuccessMessage = "La imagen de la cancha se actualizó correctamente.",
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              _uiState.value =
                  _uiState.value.copy(
                      isUpdatingCourtImage = false,
                  )
              return@onFailure
            }

            val reportingError = error.unwrapCourtImageAssociationFailure()
            _uiState.value =
                _uiState.value.copy(
                    isUpdatingCourtImage = false,
                    courtImageErrorMessage = error.toCourtImageUserMessage(),
                    courtImageSuccessMessage = null,
                )

            errorReporter.reportRecoverableFailure(
                name = "my_complex_court_image_update_failed",
                attributes = reportingError.toReportAttributes(),
            )
          }
    }
  }
}

private fun Throwable.toUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para ver tus complejos."
            else -> "No pudimos cargar tu hub de complejos. Intentá de nuevo."
          }
      else -> "No pudimos cargar tu hub de complejos. Intentá de nuevo."
    }

private fun Throwable.toReportAttributes(): Map<String, String> =
    when (this) {
      is AppApiException ->
          mapOf(
              "error_source" to "app_api",
              "status_code" to statusCode.toString(),
          )
      else -> mapOf("error_source" to "unexpected")
    }

private fun Throwable.toCourtImageUserMessage(): String =
    when (this) {
      is CourtImageAssociationUploadFailed ->
          "No pudimos subir la imagen de la cancha. Revisá el archivo e intentá de nuevo."
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para actualizar la imagen de esta cancha."
            404 -> "No encontramos la cancha seleccionada."
            else -> "No pudimos actualizar la imagen de la cancha. Intentá de nuevo."
          }
      else -> "No pudimos actualizar la imagen de la cancha. Intentá de nuevo."
    }

private fun Throwable.unwrapCourtImageAssociationFailure(): Throwable =
    if (this is CourtImageAssociationUploadFailed) {
      cause ?: this
    } else {
      this
    }

private fun List<io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex>.replaceCourt(
    complexId: String,
    updatedCourt: MyComplexHubCourt,
): List<io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex> = map { complex ->
  if (complex.id != complexId) {
    complex
  } else {
    complex.copy(
        courts =
            complex.courts.map { court -> if (court.id == updatedCourt.id) updatedCourt else court }
    )
  }
}

private class CourtImageAssociationUploadFailed(cause: Throwable) : Exception(cause)
