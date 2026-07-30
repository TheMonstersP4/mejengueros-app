package io.github.themonstersp4.mejengueros.domain.model

data class LocalProfileImage(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
    val previewUrl: String? = null,
)
