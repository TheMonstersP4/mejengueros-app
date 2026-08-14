package io.github.themonstersp4.mejengueros.presentation.mycomplex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.ReactivatedCourt
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
  private var loadedSessionKey: String? = null
  val uiState: StateFlow<MyComplexUiState> = _uiState.asStateFlow()

  /**
   * Loads the hub for the authenticated session, reloading whenever [sessionKey] changes so a
   * previous user's complex never leaks into a freshly signed-in account. This ViewModel outlives a
   * single navigation entry (it is resolved from the ambient store, not scoped per entry), so the
   * session key is what tells apart "same user re-entering the screen" from "a different user
   * signed in".
   */
  fun onSessionChanged(sessionKey: String?) {
    if (sessionKey == loadedSessionKey) return
    reset()
    loadedSessionKey = sessionKey
    refresh()
  }

  /** Discards owner/complex state held from a previous session. */
  fun reset() {
    refreshJob?.cancel()
    refreshJob = null
    loadedSessionKey = null
    _uiState.value =
        MyComplexUiState(
            isLoading = true,
            isCourtImagePickerAvailable = _uiState.value.isCourtImagePickerAvailable,
        )
  }

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
                        courtStatusErrorMessage = _uiState.value.courtStatusErrorMessage,
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

  fun acknowledgeCourtStatusSuccess() {
    _uiState.value = _uiState.value.copy(courtStatusSuccessMessage = null)
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

  fun reactivateCourt(complexId: String, courtId: String) {
    if (_uiState.value.reactivatingCourtId != null) return

    coroutineScope.launch {
      _uiState.value =
          _uiState.value.copy(
              reactivatingCourtId = courtId,
              courtStatusErrorMessage = null,
              courtStatusSuccessMessage = null,
          )

      runCatching { repository.reactivateCourt(courtId) }
          .onSuccess { reactivatedCourt ->
            _uiState.value =
                _uiState.value.copy(
                    complexes =
                        _uiState.value.complexes.replaceCourtStatus(complexId, reactivatedCourt),
                    reactivatingCourtId = null,
                    courtStatusErrorMessage = null,
                    courtStatusSuccessMessage = "La cancha se reactivó correctamente.",
                )
          }
          .onFailure { error ->
            if (error is CancellationException) {
              _uiState.value = _uiState.value.copy(reactivatingCourtId = null)
              return@onFailure
            }

            _uiState.value =
                _uiState.value.copy(
                    reactivatingCourtId = null,
                    courtStatusErrorMessage = error.toCourtStatusUserMessage(),
                    courtStatusSuccessMessage = null,
                )

            errorReporter.reportRecoverableFailure(
                name = "my_complex_court_reactivate_failed",
                attributes = error.toReportAttributes(),
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

private fun Throwable.toCourtStatusUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            401,
            403 -> "No tenés permisos para reactivar esta cancha."
            404 -> "No encontramos la cancha seleccionada."
            else -> "No pudimos reactivar la cancha. Intentá de nuevo."
          }
      else -> "No pudimos reactivar la cancha. Intentá de nuevo."
    }

private fun Throwable.unwrapCourtImageAssociationFailure(): Throwable =
    if (this is CourtImageAssociationUploadFailed) {
      cause ?: this
    } else {
      this
    }

private fun List<MyComplexHubComplex>.replaceCourt(
    complexId: String,
    updatedCourt: MyComplexHubCourt,
): List<MyComplexHubComplex> = map { complex ->
  if (complex.id != complexId) {
    complex
  } else {
    complex.copy(
        courts =
            complex.courts.map { court -> if (court.id == updatedCourt.id) updatedCourt else court }
    )
  }
}

private fun List<MyComplexHubComplex>.replaceCourtStatus(
    complexId: String,
    reactivatedCourt: ReactivatedCourt,
): List<MyComplexHubComplex> = map { complex ->
  if (complex.id != complexId) {
    complex
  } else {
    complex.copy(
        courts =
            complex.courts.map { court ->
              if (court.id == reactivatedCourt.id) {
                court.copy(status = reactivatedCourt.status)
              } else {
                court
              }
            }
    )
  }
}

private class CourtImageAssociationUploadFailed(cause: Throwable) : Exception(cause)
