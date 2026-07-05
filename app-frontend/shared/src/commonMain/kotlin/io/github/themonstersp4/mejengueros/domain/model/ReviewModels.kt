package io.github.themonstersp4.mejengueros.domain.model

data class LocalReviewEvidenceImage(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
    val previewUrl: String? = null,
)

data class ConfirmedReviewEvidenceImageUpload(
    val id: String,
    val objectKey: String,
    val readUrl: String,
)

data class ReviewableReservation(
    val reservationId: String,
    val complexName: String,
    val courtName: String,
    val startsAt: String,
    val endsAt: String,
    val imageUrl: String? = null,
)

data class CreateReviewRequest(
    val reservationId: String,
    val rating: Int,
    val comment: String? = null,
    val evidenceImageUploadId: String? = null,
)

data class CreatedReview(
    val id: String,
    val reservationId: String,
    val rating: Int,
    val comment: String? = null,
    val evidenceImageUploadId: String? = null,
    val createdAt: String,
)
