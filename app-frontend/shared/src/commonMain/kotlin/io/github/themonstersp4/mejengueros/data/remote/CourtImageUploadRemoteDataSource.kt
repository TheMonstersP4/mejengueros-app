package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.ConfirmUploadEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ConfirmUploadRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateUploadUrlEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateUploadUrlRequestDto
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedCourtImageUpload
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

private const val CourtImagePurpose = "court-image"

class CourtImageUploadRemoteDataSource(
    private val appApiHttpClient: HttpClient,
    private val uploadHttpClient: HttpClient,
    private val json: Json,
) : ICourtImageUploadRemoteDataSource {
  override suspend fun uploadCourtImage(image: LocalCourtImage): ConfirmedCourtImageUpload {
    try {
      val uploadResponse =
          appApiHttpClient
              .post("/v1/files/uploads") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    CreateUploadUrlRequestDto(
                        purpose = CourtImagePurpose,
                        contentType = image.contentType,
                        sizeBytes = image.bytes.size,
                    )
                )
              }
              .body<CreateUploadUrlEnvelopeDto>()

      val uploadData =
          uploadResponse.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      uploadHttpClient.post(uploadData.uploadUrl) {
        setBody(
            MultiPartFormDataContent(
                formData {
                  uploadData.fields.forEach { (key, value) -> append(key, value) }
                  append(
                      key = "file",
                      value = image.bytes,
                      headers =
                          Headers.build {
                            append(HttpHeaders.ContentType, image.contentType)
                            append(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.File.withParameter(
                                        ContentDisposition.Parameters.Name,
                                        "file",
                                    )
                                    .withParameter(
                                        ContentDisposition.Parameters.FileName,
                                        image.fileName,
                                    )
                                    .toString(),
                            )
                          },
                  )
                }
            )
        )
      }

      val confirmResponse =
          appApiHttpClient
              .post("/v1/files/uploads/confirm") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ConfirmUploadRequestDto(
                        purpose = CourtImagePurpose,
                        objectKey = uploadData.objectKey,
                    )
                )
              }
              .body<ConfirmUploadEnvelopeDto>()

      val confirmData =
          confirmResponse.data
              ?: throw AppApiException(
                  statusCode = 502,
                  message = "No se recibió la respuesta esperada del API.",
              )

      return ConfirmedCourtImageUpload(
          id = confirmData.id,
          objectKey = confirmData.objectKey,
          readUrl = confirmData.readUrl,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
