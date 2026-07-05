package io.github.themonstersp4.mejengueros.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import io.github.themonstersp4.mejengueros.domain.repository.NoOpReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import io.github.themonstersp4.mejengueros.monitoring.NoOpErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val reviewableReservation: ReviewableReservation? = null,
    val selectedRating: Int = 0,
    val comment: String = "",
    val selectedEvidenceImage: LocalReviewEvidenceImage? = null,
    val confirmedEvidenceImageUpload: ConfirmedReviewEvidenceImageUpload? = null,
    val isEvidenceImagePickerAvailable: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
    val submittedReview: CreatedReview? = null,
) {
  val canSubmit: Boolean
    get() {
      if (reviewableReservation == null || isSubmitting) return false
      if (selectedRating !in 1..5) return false
      if (selectedRating == 1) return comment.isNotBlank() && selectedEvidenceImage != null
      return true
    }
}

class ReviewViewModel(
    private val reviewRepository: IReviewRepository,
    private val reviewEvidenceUploadRepository: IReviewEvidenceUploadRepository =
        NoOpReviewEvidenceUploadRepository(),
    private val errorReporter: ErrorReporter = NoOpErrorReporter(),
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
  private val coroutineScope = coroutineScope ?: viewModelScope
  private val _uiState = MutableStateFlow(ReviewUiState())
  private var activeSubmissionJob: Job? = null
  private var submissionGeneration: Long = 0
  private var latestReservationLoadGeneration: Long = 0
  val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

  init {
    refreshLatestReviewableReservation()
  }

  fun refreshLatestReviewableReservation() {
    val currentLoadGeneration = ++latestReservationLoadGeneration
    _uiState.value =
        _uiState.value.copy(
            isLoading = true,
            loadErrorMessage = null,
            submitErrorMessage = null,
            submittedReview = null,
        )

    coroutineScope.launch {
      runCatching { reviewRepository.getLatestReviewableReservation() }
          .onSuccess { reservation ->
            if (latestReservationLoadGeneration != currentLoadGeneration) return@onSuccess
            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    loadErrorMessage = null,
                    reviewableReservation = reservation,
                )
          }
          .onFailure { error ->
            if (error is CancellationException) return@onFailure
            if (latestReservationLoadGeneration != currentLoadGeneration) return@onFailure

            errorReporter.reportRecoverableFailure(
                name = "review_latest_reservation_load_failed",
                attributes = error.toReportAttributes(),
            )

            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    loadErrorMessage = "No pudimos cargar la reserva pendiente de reseña.",
                    reviewableReservation = null,
                )
          }
    }
  }

  fun startReview() {
    _uiState.value = _uiState.value.copy(submitErrorMessage = null)
  }

  fun startReview(reservation: ReviewableReservation) {
    invalidateActiveSubmission()
    invalidateLatestReservationLoad()
    _uiState.value =
        _uiState.value.copy(
            isLoading = false,
            loadErrorMessage = null,
            reviewableReservation = reservation,
            selectedRating = 0,
            comment = "",
            selectedEvidenceImage = null,
            confirmedEvidenceImageUpload = null,
            isSubmitting = false,
            submitErrorMessage = null,
            submittedReview = null,
        )
  }

  fun updateRating(rating: Int) {
    _uiState.value =
        _uiState.value.copy(
            selectedRating = rating,
            submitErrorMessage = null,
            submittedReview = null,
        )
  }

  fun updateComment(comment: String) {
    _uiState.value =
        _uiState.value.copy(comment = comment, submitErrorMessage = null, submittedReview = null)
  }

  fun updateSelectedEvidenceImage(image: LocalReviewEvidenceImage?) {
    _uiState.value =
        _uiState.value.copy(
            selectedEvidenceImage = image,
            confirmedEvidenceImageUpload = null,
            submitErrorMessage = null,
            submittedReview = null,
        )
  }

  fun clearSelectedEvidenceImage() {
    updateSelectedEvidenceImage(null)
  }

  fun updateEvidenceImagePickerAvailability(isAvailable: Boolean) {
    _uiState.value = _uiState.value.copy(isEvidenceImagePickerAvailable = isAvailable)
  }

  fun resetFlow(reloadLatestReservation: Boolean = true) {
    val currentState = _uiState.value
    val shouldRefreshReservation =
        reloadLatestReservation &&
            (currentState.submittedReview != null || currentState.isSubmitting)
    invalidateActiveSubmission()
    invalidateLatestReservationLoad()
    _uiState.value =
        currentState.copy(
            selectedRating = 0,
            comment = "",
            selectedEvidenceImage = null,
            confirmedEvidenceImageUpload = null,
            isSubmitting = false,
            submitErrorMessage = null,
            submittedReview = null,
        )

    if (shouldRefreshReservation) {
      refreshLatestReviewableReservation()
    }
  }

  fun submit() {
    val currentState = _uiState.value
    val reservation = currentState.reviewableReservation ?: return
    if (!currentState.canSubmit) return

    invalidateActiveSubmission()
    val currentSubmissionGeneration = ++submissionGeneration

    activeSubmissionJob =
        coroutineScope.launch {
          _uiState.value = currentState.copy(isSubmitting = true, submitErrorMessage = null)
          var confirmedEvidenceImageUpload = currentState.confirmedEvidenceImageUpload

          runCatching {
                confirmedEvidenceImageUpload =
                    confirmedEvidenceImageUpload
                        ?: currentState.selectedEvidenceImage?.let { image ->
                          try {
                            reviewEvidenceUploadRepository.uploadReviewEvidence(image)
                          } catch (error: Throwable) {
                            throw ReviewEvidenceUploadFailed(error)
                          }
                        }

                reviewRepository.createReview(
                    CreateReviewRequest(
                        reservationId = reservation.reservationId,
                        rating = currentState.selectedRating,
                        comment = currentState.comment.trim().ifBlank { null },
                        evidenceImageUploadId = confirmedEvidenceImageUpload?.id,
                    )
                )
              }
              .onSuccess { review ->
                if (!isActiveSubmission(currentSubmissionGeneration)) return@onSuccess
                clearActiveSubmissionIfCurrent(currentSubmissionGeneration)
                _uiState.value =
                    currentState.copy(
                        isSubmitting = false,
                        submitErrorMessage = null,
                        confirmedEvidenceImageUpload = confirmedEvidenceImageUpload,
                        submittedReview = review,
                    )
              }
              .onFailure { error ->
                if (!isActiveSubmission(currentSubmissionGeneration)) return@onFailure
                clearActiveSubmissionIfCurrent(currentSubmissionGeneration)

                if (error is CancellationException) {
                  _uiState.value = currentState.copy(isSubmitting = false)
                  return@onFailure
                }

                errorReporter.reportRecoverableFailure(
                    name = "review_submit_failed",
                    attributes = error.unwrapReviewSubmissionFailure().toReportAttributes(),
                )

                _uiState.value =
                    currentState.copy(
                        isSubmitting = false,
                        confirmedEvidenceImageUpload = confirmedEvidenceImageUpload,
                        submitErrorMessage = error.toSubmitUserMessage(),
                        submittedReview = null,
                    )
              }
        }
  }

  override fun onCleared() {
    invalidateActiveSubmission()
    super.onCleared()
  }

  private fun invalidateActiveSubmission() {
    submissionGeneration += 1
    activeSubmissionJob?.cancel()
    activeSubmissionJob = null
  }

  private fun invalidateLatestReservationLoad() {
    latestReservationLoadGeneration += 1
  }

  private fun isActiveSubmission(generation: Long): Boolean = submissionGeneration == generation

  private fun clearActiveSubmissionIfCurrent(generation: Long) {
    if (submissionGeneration == generation) {
      activeSubmissionJob = null
    }
  }
}

private fun Throwable.toSubmitUserMessage(): String =
    when (this) {
      is ReviewEvidenceUploadFailed -> cause.toReviewEvidenceUploadUserMessage()
      is AppApiException ->
          when (statusCode) {
            400 -> "Revisá la calificación, el comentario y la evidencia antes de enviar la reseña."
            401,
            403 -> "Necesitás volver a iniciar sesión para enviar la reseña."
            404 -> "No encontramos una reserva válida para registrar esta reseña."
            409 -> "Esta reserva ya tenía una reseña registrada."
            else -> "No pudimos enviar la reseña. Intentá de nuevo."
          }
      else -> "No pudimos enviar la reseña. Intentá de nuevo."
    }

private fun Throwable?.toReviewEvidenceUploadUserMessage(): String =
    when (this) {
      is AppApiException ->
          when (statusCode) {
            400,
            413,
            415,
            422 -> "Revisá la imagen de evidencia e intentá de nuevo."
            401,
            403 -> "Necesitás volver a iniciar sesión para subir la imagen de evidencia."
            else -> "No pudimos subir la imagen de evidencia. Revisá el archivo e intentá de nuevo."
          }
      else -> "No pudimos subir la imagen de evidencia. Revisá el archivo e intentá de nuevo."
    }

private fun Throwable.unwrapReviewSubmissionFailure(): Throwable =
    if (this is ReviewEvidenceUploadFailed) {
      cause ?: this
    } else {
      this
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

private class ReviewEvidenceUploadFailed(cause: Throwable) : Exception(cause)
