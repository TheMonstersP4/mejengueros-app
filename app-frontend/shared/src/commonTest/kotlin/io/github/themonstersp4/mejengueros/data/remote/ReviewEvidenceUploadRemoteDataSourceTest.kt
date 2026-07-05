package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
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
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ReviewEvidenceUploadRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun uploadReviewEvidenceUsesReviewEvidencePurposeAndConfirmsUpload() = runTest {
    val apiRequests = mutableListOf<String>()
    val apiBodies = mutableListOf<String>()
    var storageMethod = HttpMethod.Get
    var storageUrl = ""
    var storageAuthorizationHeader: String? = "present"
    var storageContentType = ""
    var storageBody = ""
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
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
                                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                                        "method": "POST",
                                        "uploadUrl": "https://upload.example.test",
                                        "fields": {
                                          "key": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
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
                                        "id": "6f554321-6df0-43c4-b310-f3d7e6bf00a1",
                                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                                        "readUrl": "https://read.example.test/evidence.png"
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
                    json(this@ReviewEvidenceUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
                  }
                },
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      storageMethod = request.method
                      storageUrl = request.url.toString()
                      storageAuthorizationHeader = request.headers[HttpHeaders.Authorization]
                      storageContentType = request.body.contentType?.toString().orEmpty()
                      storageBody = request.body.readText()
                      respond(content = "", status = HttpStatusCode.NoContent)
                    }
                  }
                },
            json = json,
        )

    val confirmed =
        dataSource.uploadReviewEvidence(
            LocalReviewEvidenceImage(
                fileName = "evidence.png",
                contentType = "image/png",
                bytes = byteArrayOf(1, 2, 3),
                previewUrl = "content://evidence.png",
            )
        )

    assertEquals(listOf("/v1/files/uploads", "/v1/files/uploads/confirm"), apiRequests)
    assertEquals(HttpMethod.Post, storageMethod)
    assertEquals("https://upload.example.test", storageUrl)
    assertEquals(null, storageAuthorizationHeader)
    assertEquals(true, storageContentType.startsWith("multipart/form-data"))
    assertEquals(true, storageBody.contains("name=\"key\""))
    assertEquals(
        true,
        storageBody.contains("dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png"),
    )
    assertEquals(true, storageBody.contains("name=\"policy\""))
    assertEquals(true, storageBody.contains("name=file"))
    assertEquals(true, storageBody.contains("filename=evidence.png"))
    assertEquals(true, storageBody.contains("Content-Type: image/png"))
    assertEquals(true, apiBodies.first().contains("\"purpose\":\"review-evidence-image\""))
    assertEquals(
        true,
        apiBodies
            .last()
            .contains(
                "\"objectKey\":\"dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png\""
            ),
    )
    assertEquals("6f554321-6df0-43c4-b310-f3d7e6bf00a1", confirmed.id)
    assertEquals("https://read.example.test/evidence.png", confirmed.readUrl)
  }

  @Test
  fun uploadReviewEvidenceMapsCreateUploadFailureToAppApiException() = runTest {
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
            appApiHttpClient = failingApiClient(status = HttpStatusCode.BadRequest),
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      error("Upload client should not be called when create-upload fails.")
                    }
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> { dataSource.uploadReviewEvidence(sampleEvidenceImage()) }

    assertEquals(400, error.statusCode)
  }

  @Test
  fun uploadReviewEvidenceFailsWhenCreateUploadResponseDataIsMissing() = runTest {
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
            appApiHttpClient =
                apiClientWithResponses(
                    """
                    {
                      "success": true,
                      "data": null
                    }
                    """
                        .trimIndent(),
                ),
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      error(
                          "Upload client should not be called when create-upload data is missing."
                      )
                    }
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> { dataSource.uploadReviewEvidence(sampleEvidenceImage()) }

    assertEquals(502, error.statusCode)
    assertEquals("No se recibió la respuesta esperada del API.", error.message)
  }

  @Test
  fun uploadReviewEvidenceMapsConfirmFailureToAppApiException() = runTest {
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
            appApiHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    var requestIndex = 0
                    addHandler { request ->
                      requestIndex += 1
                      when (requestIndex) {
                        1 ->
                            respond(
                                content =
                                    """
                                    {
                                      "success": true,
                                      "data": {
                                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                                        "method": "POST",
                                        "uploadUrl": "https://upload.example.test",
                                        "fields": {
                                          "key": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png"
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
                                content = """{"success":false,"error":{"message":"Conflict"}}""",
                                status = HttpStatusCode.Conflict,
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
                    json(this@ReviewEvidenceUploadRemoteDataSourceTest.json)
                  }
                  install(DefaultRequest) {
                    url("https://api.example.test")
                    header(HttpHeaders.Authorization, "Bearer player-token")
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
        assertFailsWith<AppApiException> { dataSource.uploadReviewEvidence(sampleEvidenceImage()) }

    assertEquals(409, error.statusCode)
  }

  @Test
  fun uploadReviewEvidenceFailsWhenConfirmUploadResponseDataIsMissing() = runTest {
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
            appApiHttpClient =
                apiClientWithResponses(
                    """
                    {
                      "success": true,
                      "data": {
                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                        "method": "POST",
                        "uploadUrl": "https://upload.example.test",
                        "fields": {
                          "key": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png"
                        },
                        "expiresInSeconds": 300,
                        "maxSizeBytes": 5242880
                      }
                    }
                    """
                        .trimIndent(),
                    """
                    {
                      "success": true,
                      "data": null
                    }
                    """
                        .trimIndent(),
                ),
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine { addHandler { respond(content = "", status = HttpStatusCode.NoContent) } }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> { dataSource.uploadReviewEvidence(sampleEvidenceImage()) }

    assertEquals(502, error.statusCode)
    assertEquals("No se recibió la respuesta esperada del API.", error.message)
  }

  @Test
  fun uploadReviewEvidencePropagatesStorageUploadFailure() = runTest {
    val dataSource =
        ReviewEvidenceUploadRemoteDataSource(
            appApiHttpClient =
                apiClientWithResponses(
                    """
                    {
                      "success": true,
                      "data": {
                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                        "method": "POST",
                        "uploadUrl": "https://upload.example.test",
                        "fields": {
                          "key": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png"
                        },
                        "expiresInSeconds": 300,
                        "maxSizeBytes": 5242880
                      }
                    }
                    """
                        .trimIndent(),
                    """
                    {
                      "success": true,
                      "data": {
                        "id": "6f554321-6df0-43c4-b310-f3d7e6bf00a1",
                        "objectKey": "dev/uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                        "readUrl": "https://read.example.test/evidence.png"
                      }
                    }
                    """
                        .trimIndent(),
                ),
            uploadHttpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler {
                      throw IllegalStateException("Simulated raw storage upload failure")
                    }
                  }
                },
            json = json,
        )

    val error =
        assertFailsWith<IllegalStateException> {
          dataSource.uploadReviewEvidence(sampleEvidenceImage())
        }

    assertContains(error.message.orEmpty(), "Simulated raw storage upload failure")
  }

  private fun failingApiClient(status: HttpStatusCode): HttpClient =
      HttpClient(MockEngine) {
        expectSuccess = true
        engine {
          addHandler {
            respond(
                content = """{"success":false,"error":{"message":"HTTP ${status.value}"}}""",
                status = status,
                headers =
                    headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
            )
          }
        }
        install(ContentNegotiation) { json(this@ReviewEvidenceUploadRemoteDataSourceTest.json) }
        install(DefaultRequest) {
          url("https://api.example.test")
          header(HttpHeaders.Authorization, "Bearer player-token")
        }
      }

  private fun apiClientWithResponses(vararg responseBodies: String): HttpClient =
      HttpClient(MockEngine) {
        expectSuccess = true
        engine {
          var requestIndex = 0
          addHandler {
            val body =
                responseBodies.getOrNull(requestIndex)
                    ?: error("Unexpected API request index: $requestIndex")
            requestIndex += 1
            respond(
                content = body,
                status = HttpStatusCode.Created,
                headers =
                    headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
            )
          }
        }
        install(ContentNegotiation) { json(this@ReviewEvidenceUploadRemoteDataSourceTest.json) }
        install(DefaultRequest) {
          url("https://api.example.test")
          header(HttpHeaders.Authorization, "Bearer player-token")
        }
      }

  private fun sampleEvidenceImage() =
      LocalReviewEvidenceImage(
          fileName = "evidence.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://evidence.png",
      )

  private suspend fun OutgoingContent.readText(): String =
      when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readText()
        is OutgoingContent.WriteChannelContent ->
            ByteChannel(autoFlush = true)
                .also { channel ->
                  writeTo(channel)
                  channel.close()
                }
                .readRemaining()
                .readText()
        is OutgoingContent.NoContent -> ""
        else -> error("Unsupported request body type in test: ${this::class}")
      }
}
