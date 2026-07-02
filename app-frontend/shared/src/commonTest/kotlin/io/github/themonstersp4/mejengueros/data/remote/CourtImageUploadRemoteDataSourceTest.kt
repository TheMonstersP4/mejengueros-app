package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CourtImageUploadRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun uploadCourtImageCreatesUploadUrlUploadsBinaryAndConfirmsUpload() = runTest {
    val apiRequests = mutableListOf<String>()
    val apiBodies = mutableListOf<String>()
    var storageMethod = HttpMethod.Get
    var storageUrl = ""
    var storageAuthorizationHeader = "present"
    var storageContentType = ""
    val dataSource =
        CourtImageUploadRemoteDataSource(
            appApiHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    var requestIndex = 0
                    addHandler { request ->
                      apiRequests += request.url.encodedPath
                      apiBodies += request.body.readText()
                      requestIndex += 1

                      when (requestIndex) {
                        1 ->
                            respond(
                                content =
                                    """
                                    {
                                      "success": true,
                                      "data": {
                                        "objectKey": "dev/uploads/court-image/owner-sub/2026/06/court.png",
                                        "method": "POST",
                                        "uploadUrl": "https://upload.example.test",
                                        "fields": {
                                          "key": "dev/uploads/court-image/owner-sub/2026/06/court.png",
                                          "policy": "policy"
                                        },
                                        "expiresInSeconds": 300,
                                        "maxSizeBytes": 5242880
                                      }
                                    }
                                    """
                                        .trimIndent(),
                                status = HttpStatusCode.Created,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        ContentType.Application.Json.toString(),
                                    ),
                            )
                        else ->
                            respond(
                                content =
                                    """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": "court-image-id",
                                        "objectKey": "dev/uploads/court-image/owner-sub/2026/06/court.png",
                                        "purpose": "court-image",
                                        "status": "ready",
                                        "contentType": "image/png",
                                        "sizeBytes": 3,
                                        "readUrl": "https://read.example.test/court.png",
                                        "createdAt": "2026-07-02T17:00:00.000Z",
                                        "uploadedBy": {
                                          "sub": "owner-sub"
                                        }
                                      }
                                    }
                                    """
                                        .trimIndent(),
                                status = HttpStatusCode.Created,
                                headers =
                                    headersOf(
                                        HttpHeaders.ContentType,
                                        ContentType.Application.Json.toString(),
                                    ),
                            )
                      }
                    }
                  }
                  install(ContentNegotiation) {
                    json(this@CourtImageUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      storageMethod = request.method
                      storageUrl = request.url.toString()
                      storageAuthorizationHeader =
                          request.headers[HttpHeaders.Authorization].orEmpty()
                      storageContentType = request.body.contentType?.toString().orEmpty()
                      respond(content = "", status = HttpStatusCode.NoContent)
                    }
                  }
                },
            json = json,
        )

    val confirmed =
        dataSource.uploadCourtImage(
            LocalCourtImage(
                fileName = "court.png",
                contentType = "image/png",
                bytes = byteArrayOf(1, 2, 3),
                previewUrl = "content://court.png",
            )
        )

    assertEquals(listOf("/v1/files/uploads", "/v1/files/uploads/confirm"), apiRequests)
    assertEquals(HttpMethod.Post, storageMethod)
    assertEquals("https://upload.example.test", storageUrl)
    assertEquals("", storageAuthorizationHeader)
    assertEquals(true, storageContentType.startsWith("multipart/form-data"))
    assertEquals(true, apiBodies.first().contains("\"purpose\":\"court-image\""))
    assertEquals(
        true,
        apiBodies
            .last()
            .contains("\"objectKey\":\"dev/uploads/court-image/owner-sub/2026/06/court.png\""),
    )
    assertEquals("court-image-id", confirmed.id)
    assertEquals("https://read.example.test/court.png", confirmed.readUrl)
  }

  @Test
  fun uploadCourtImageMapsConfirmFailureIntoAppApiException() = runTest {
    val dataSource =
        CourtImageUploadRemoteDataSource(
            appApiHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    var requestIndex = 0
                    addHandler {
                      requestIndex += 1
                      if (requestIndex == 1) {
                        respond(
                            content =
                                """
                                {
                                  "success": true,
                                  "data": {
                                    "objectKey": "dev/uploads/court-image/owner-sub/2026/06/court.png",
                                    "method": "POST",
                                    "uploadUrl": "https://upload.example.test",
                                    "fields": { "key": "dev/uploads/court-image/owner-sub/2026/06/court.png" },
                                    "expiresInSeconds": 300,
                                    "maxSizeBytes": 5242880
                                  }
                                }
                                """
                                    .trimIndent(),
                            status = HttpStatusCode.Created,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString(),
                                ),
                        )
                      } else {
                        respond(
                            content =
                                """
                                {
                                  "success": false,
                                  "errors": [
                                    {
                                      "code": "UNSUPPORTED_MEDIA_TYPE",
                                      "message": "Unsupported media type",
                                      "status": 415
                                    }
                                  ]
                                }
                                """
                                    .trimIndent(),
                            status = HttpStatusCode.UnsupportedMediaType,
                            headers =
                                headersOf(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString(),
                                ),
                        )
                      }
                    }
                  }
                  install(ContentNegotiation) {
                    json(this@CourtImageUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine { addHandler { respond(content = "", status = HttpStatusCode.NoContent) } }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.uploadCourtImage(
              LocalCourtImage(
                  fileName = "court.png",
                  contentType = "image/png",
                  bytes = byteArrayOf(1, 2, 3),
                  previewUrl = "content://court.png",
              )
          )
        }

    assertEquals(415, error.statusCode)
    assertEquals("Unsupported media type", error.message)
  }

  @Test
  fun uploadCourtImageMapsCreateUploadUrlFailureIntoAppApiException() = runTest {
    val dataSource =
        CourtImageUploadRemoteDataSource(
            appApiHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content =
                              """
                              {
                                "success": false,
                                "errors": [
                                  {
                                    "code": "VALIDATION_FAILED",
                                    "message": "Could not create upload URL",
                                    "status": 422
                                  }
                                ]
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.UnprocessableEntity,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) {
                    json(this@CourtImageUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine { addHandler { respond(content = "", status = HttpStatusCode.NoContent) } }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.uploadCourtImage(
              LocalCourtImage(
                  fileName = "court.png",
                  contentType = "image/png",
                  bytes = byteArrayOf(1, 2, 3),
                  previewUrl = "content://court.png",
              )
          )
        }

    assertEquals(422, error.statusCode)
    assertEquals("Could not create upload URL", error.message)
  }

  @Test
  fun uploadCourtImageMapsBinaryUploadFailureIntoAppApiException() = runTest {
    val dataSource =
        CourtImageUploadRemoteDataSource(
            appApiHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "objectKey": "dev/uploads/court-image/owner-sub/2026/06/court.png",
                                  "method": "POST",
                                  "uploadUrl": "https://upload.example.test",
                                  "fields": { "key": "dev/uploads/court-image/owner-sub/2026/06/court.png" },
                                  "expiresInSeconds": 300,
                                  "maxSizeBytes": 5242880
                                }
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.Created,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                  install(ContentNegotiation) {
                    json(this@CourtImageUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer owner-token")
                  }
                },
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      respond(
                          content =
                              """
                              {
                                "success": false,
                                "errors": [
                                  {
                                    "code": "UPLOAD_FAILED",
                                    "message": "Storage rejected upload",
                                    "status": 403
                                  }
                                ]
                              }
                              """
                                  .trimIndent(),
                          status = HttpStatusCode.Forbidden,
                          headers =
                              headersOf(
                                  HttpHeaders.ContentType,
                                  ContentType.Application.Json.toString(),
                              ),
                      )
                    }
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.uploadCourtImage(
              LocalCourtImage(
                  fileName = "court.png",
                  contentType = "image/png",
                  bytes = byteArrayOf(1, 2, 3),
                  previewUrl = "content://court.png",
              )
          )
        }

    assertEquals(403, error.statusCode)
    assertEquals("Storage rejected upload", error.message)
  }

  private fun OutgoingContent.readText(): String =
      when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.NoContent -> ""
        else -> error("Unsupported request body type in test: ${this::class}")
      }
}
