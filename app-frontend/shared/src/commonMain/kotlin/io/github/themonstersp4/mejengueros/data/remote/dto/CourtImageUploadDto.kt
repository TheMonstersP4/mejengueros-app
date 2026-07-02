package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUploadUrlRequestDto(
    val purpose: String,
    val contentType: String,
    val sizeBytes: Int,
)

@Serializable
data class CreateUploadUrlEnvelopeDto(
    val success: Boolean,
    val data: CreateUploadUrlResponseDto?,
)

@Serializable
data class CreateUploadUrlResponseDto(
    val objectKey: String,
    val method: String,
    val uploadUrl: String,
    val fields: Map<String, String>,
    val expiresInSeconds: Int,
    val maxSizeBytes: Int,
)

@Serializable
data class ConfirmUploadRequestDto(
    val purpose: String,
    val objectKey: String,
)

@Serializable
data class ConfirmUploadEnvelopeDto(
    val success: Boolean,
    val data: ConfirmUploadResponseDto?,
)

@Serializable
data class ConfirmUploadResponseDto(
    val id: String,
    val objectKey: String,
    val readUrl: String,
)
