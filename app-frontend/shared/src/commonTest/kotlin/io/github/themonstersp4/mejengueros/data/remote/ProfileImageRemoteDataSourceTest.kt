package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.LocalProfileImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileImageRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun updateProfileImageRequestsUploadsConfirmsAndAssociatesInOrder() = runTest {
    val events = mutableListOf<String>()
    val bodies = mutableListOf<String>()
    val dataSource =
        dataSource(
            events = events,
            bodies = bodies,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

    val profile = dataSource.updateProfileImage(sampleImage())

    assertEquals(listOf("create", "upload", "confirm", "associate"), events)
    assertEquals(true, bodies[0].contains("\"purpose\":\"profile-image\""))
    assertEquals(true, bodies[1].contains("filename=profile.png"))
    assertEquals(true, bodies[2].contains("\"purpose\":\"profile-image\""))
    assertEquals(true, bodies[2].contains("\"objectKey\":\"pending-profile-key\""))
    assertEquals(true, bodies[3].contains("\"imageUploadId\":\"profile-upload-id\""))
    assertEquals("player@example.test", profile.email)
    assertEquals("Player One", profile.name)
    assertEquals("https://read.example.test/profile.jpg", profile.pictureUrl)
    assertEquals("Google", profile.provider)
  }

  @Test
  fun updateProfileImageStopsAtEachFailedStage() = runTest {
    FailureStage.entries.forEach { stage ->
      val events = mutableListOf<String>()
      val dataSource =
          dataSource(
              events = events,
              failureStage = stage,
              dispatcher = UnconfinedTestDispatcher(testScheduler),
          )

      if (stage == FailureStage.Upload) {
        assertFailsWith<IllegalStateException> { dataSource.updateProfileImage(sampleImage()) }
      } else {
        assertFailsWith<AppApiException> { dataSource.updateProfileImage(sampleImage()) }
      }

      assertEquals(stage.expectedEvents, events)
    }
  }

  @Test
  fun updateProfileImageDoesNotSwallowCoroutineCancellation() = runTest {
    val dataSource =
        dataSource(
            cancellationAtUpload = true,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

    assertFailsWith<CancellationException> { dataSource.updateProfileImage(sampleImage()) }
  }

  private fun dataSource(
      events: MutableList<String> = mutableListOf(),
      bodies: MutableList<String> = mutableListOf(),
      failureStage: FailureStage? = null,
      cancellationAtUpload: Boolean = false,
      dispatcher: kotlinx.coroutines.CoroutineDispatcher,
  ): ProfileImageRemoteDataSource {
    val appApiClient =
        HttpClient(MockEngine) {
          expectSuccess = true
          engine {
            addHandler { request ->
              val stage =
                  when (request.url.encodedPath) {
                    "/v1/files/uploads" -> FailureStage.Create
                    "/v1/files/uploads/confirm" -> FailureStage.Confirm
                    "/v1/users/me/profile-image" -> FailureStage.Associate
                    else -> error("Unexpected API path: ${request.url.encodedPath}")
                  }
              events += stage.event
              bodies += request.body.readText()

              if (failureStage == stage) {
                respond(
                    content = """{"success":false,"errors":[{"message":"stage failed"}]}""",
                    status = HttpStatusCode.Conflict,
                    headers = jsonHeaders(),
                )
              } else {
                respond(
                    content = successBody(stage),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
              }
            }
          }
          install(ContentNegotiation) { json(this@ProfileImageRemoteDataSourceTest.json) }
          install(DefaultRequest) {
            url("https://api.example.test")
            header(HttpHeaders.Authorization, "Bearer player-token")
          }
        }
    val uploadClient =
        HttpClient(MockEngine) {
          expectSuccess = true
          engine {
            addHandler { request ->
              events += FailureStage.Upload.event
              bodies += request.body.readText()
              if (cancellationAtUpload) throw CancellationException("cancelled")
              if (failureStage == FailureStage.Upload) error("storage upload failed")
              respond(content = "", status = HttpStatusCode.NoContent)
            }
          }
        }

    return ProfileImageRemoteDataSource(appApiClient, uploadClient, json, dispatcher)
  }

  private fun successBody(stage: FailureStage): String =
      when (stage) {
        FailureStage.Create ->
            """{"success":true,"data":{"objectKey":"pending-profile-key","method":"POST","uploadUrl":"https://upload.example.test","fields":{"key":"pending-profile-key"},"expiresInSeconds":300,"maxSizeBytes":5242880}}"""
        FailureStage.Confirm ->
            """{"success":true,"data":{"id":"profile-upload-id","objectKey":"confirmed-profile-key","readUrl":"https://read.example.test/profile.jpg"}}"""
        FailureStage.Associate ->
            """{"success":true,"data":{"id":"player-id","email":"player@example.test","name":"Player One","pictureUrl":"https://read.example.test/profile.jpg","provider":"Google","roles":["PLAYER"]}}"""
        FailureStage.Upload -> error("Storage does not return an API body.")
      }

  private fun jsonHeaders() =
      headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

  private fun sampleImage() =
      LocalProfileImage(
          fileName = "profile.png",
          contentType = "image/png",
          bytes = byteArrayOf(1, 2, 3),
          previewUrl = "content://profile.png",
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

  private enum class FailureStage(
      val event: String,
      val expectedEvents: List<String>,
  ) {
    Create("create", listOf("create")),
    Upload("upload", listOf("create", "upload")),
    Confirm("confirm", listOf("create", "upload", "confirm")),
    Associate("associate", listOf("create", "upload", "confirm", "associate")),
  }
}
