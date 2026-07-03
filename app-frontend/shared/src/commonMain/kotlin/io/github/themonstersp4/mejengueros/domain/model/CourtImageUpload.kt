package io.github.themonstersp4.mejengueros.domain.model

data class LocalCourtImage(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
    val previewUrl: String? = null,
)

data class ConfirmedCourtImageUpload(
    val id: String,
    val objectKey: String,
    val readUrl: String,
)
