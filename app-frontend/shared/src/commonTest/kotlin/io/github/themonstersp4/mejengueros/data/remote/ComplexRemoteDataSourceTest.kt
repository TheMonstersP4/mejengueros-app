package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CreateComplexRequestDto
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class ComplexRemoteDataSourceTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun createComplexPostsPayloadAndMapsEnvelope() = runTest {
    var requestedPath = ""
    var requestedMethod = HttpMethod.Get
    var requestBody = ""
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
                HttpClient(MockEngine) {
                  expectSuccess = true
                  engine {
                    addHandler { request ->
                      requestedPath = request.url.encodedPath
                      requestedMethod = request.method
                      requestBody = request.body.readText()
                      respond(
                          content =
                              """
                              {
                                "success": true,
                                "data": {
                                  "complex": {
                                    "id": "complex-id",
                                    "name": "North Sports Center",
                                    "address": "123 Main Street"
                                  },
                                  "firstCourt": {
                                    "id": "court-id",
                                    "complexId": "complex-id",
                                    "name": "Court A"
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
                  install(ContentNegotiation) { json(this@ComplexRemoteDataSourceTest.json) }
                  install(DefaultRequest) { url("https://api.example.test") }
                },
            json = json,
        )

    val created =
        dataSource.createComplex(
            CreateComplexRequest(
                complexName = "North Sports Center",
                complexAddress = "123 Main Street",
                firstCourtName = "Court A",
            )
        )

    assertEquals(HttpMethod.Post, requestedMethod)
    assertEquals("/v1/complexes", requestedPath)
    val requestDto = json.decodeFromString<CreateComplexRequestDto>(requestBody)
    assertEquals("North Sports Center", requestDto.complex.name)
    assertEquals("123 Main Street", requestDto.complex.address)
    assertEquals("Court A", requestDto.firstCourt.name)
    assertEquals("complex-id", created.complexId)
    assertEquals("Court A", created.firstCourtName)
  }

  @Test
  fun createComplexMapsForbiddenEnvelopeIntoAppApiException() = runTest {
    val dataSource =
        ComplexRemoteDataSource(
            httpClient =
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
                                    "code": "FORBIDDEN",
                                    "message": "Only users with the OWNER role can create complexes.",
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
                  install(ContentNegotiation) { json(this@ComplexRemoteDataSourceTest.json) }
                  install(DefaultRequest) { url("https://api.example.test") }
                },
            json = json,
        )

    val error =
        assertFailsWith<AppApiException> {
          dataSource.createComplex(
              CreateComplexRequest(
                  complexName = "North Sports Center",
                  complexAddress = "123 Main Street",
                  firstCourtName = "Court A",
              )
          )
        }

    assertEquals(403, error.statusCode)
    assertEquals("Only users with the OWNER role can create complexes.", error.message)
  }

  private fun OutgoingContent.readText(): String =
      when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().toString(Charsets.UTF_8)
        is OutgoingContent.ReadChannelContent ->
            error("Unsupported request body type in test: ${this::class}")
        is OutgoingContent.WriteChannelContent ->
            error("Unsupported request body type in test: ${this::class}")
        is OutgoingContent.NoContent -> ""
        is OutgoingContent.ProtocolUpgrade ->
            error("Unsupported request body type in test: ${this::class}")
        else -> error("Unsupported request body type in test: ${this::class}")
      }
}
