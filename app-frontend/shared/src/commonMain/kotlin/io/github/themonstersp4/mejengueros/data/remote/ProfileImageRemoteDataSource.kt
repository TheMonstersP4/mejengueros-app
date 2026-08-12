package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.ConfirmUploadEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ConfirmUploadRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateUploadUrlEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.CreateUploadUrlRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.UpdateProfileImageRequestDto
import io.github.themonstersp4.mejengueros.data.remote.dto.UserProfileEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.domain.model.UserRoleKind
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val ProfileImagePurpose = "profile-image"

class ProfileImageRemoteDataSource(
    private val appApiHttpClient: HttpClient,
    private val uploadHttpClient: HttpClient,
    private val json: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : IProfileImageRemoteDataSource {
  override suspend fun updateProfileImage(image: LocalProfileImage): UserProfile =
      withContext(dispatcher) {
        try {
          val upload = createUpload(image)
          uploadBinary(upload.uploadUrl, upload.fields, image)
          val confirmedUploadId = confirmUpload(upload.objectKey)
          associateProfileImage(confirmedUploadId)
        } catch (error: ResponseException) {
          throw error.toAppApiException(json)
        }
      }

  private suspend fun createUpload(
      image: LocalProfileImage
  ): io.github.themonstersp4.mejengueros.data.remote.dto.CreateUploadUrlResponseDto {
    val response =
        appApiHttpClient
            .post("/v1/files/uploads") {
              header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
              setBody(
                  CreateUploadUrlRequestDto(
                      purpose = ProfileImagePurpose,
                      contentType = image.contentType,
                      sizeBytes = image.bytes.size,
                  )
              )
            }
            .body<CreateUploadUrlEnvelopeDto>()

    return response.data ?: missingApiData()
  }

  private suspend fun uploadBinary(
      uploadUrl: String,
      fields: Map<String, String>,
      image: LocalProfileImage,
  ) {
    uploadHttpClient.post(uploadUrl) {
      setBody(
          MultiPartFormDataContent(
              formData {
                fields.forEach { (key, value) -> append(key, value) }
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
  }

  private suspend fun confirmUpload(objectKey: String): String {
    val response =
        appApiHttpClient
            .post("/v1/files/uploads/confirm") {
              header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
              setBody(ConfirmUploadRequestDto(purpose = ProfileImagePurpose, objectKey = objectKey))
            }
            .body<ConfirmUploadEnvelopeDto>()

    return response.data?.id ?: missingApiData()
  }

  private suspend fun associateProfileImage(imageUploadId: String): UserProfile {
    val response =
        appApiHttpClient
            .put("/v1/users/me/profile-image") {
              header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
              setBody(UpdateProfileImageRequestDto(imageUploadId))
            }
            .body<UserProfileEnvelopeDto>()
    val profile = response.data ?: missingApiData()

    return UserProfile(
        id = profile.id,
        roles =
            profile.roles.mapNotNull { rawRole ->
              UserRoleKind.entries.find { it.name == rawRole }
            },
        cognitoSub = profile.cognitoSub,
        email = profile.email,
        name = profile.name,
        pictureUrl = profile.pictureUrl,
        provider = profile.provider,
    )
  }

  private fun missingApiData(): Nothing =
      throw AppApiException(
          statusCode = 502,
          message = "No se recibió la respuesta esperada del API.",
      )
}
