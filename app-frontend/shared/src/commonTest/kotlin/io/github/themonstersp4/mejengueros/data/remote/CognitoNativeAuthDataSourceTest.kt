package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CognitoNativeAuthDataSourceTest {

  private val serializer = Json { ignoreUnknownKeys = true }
  private val config =
      CognitoAuthConfig(
          region = "us-east-2",
          domain = "https://auth.example.com",
          clientId = "client-id",
          redirectUri = "mejengueros://auth/callback",
          logoutUri = "mejengueros://auth/logout",
          scopes = listOf("openid", "email", "profile"),
      )

  @Test
  fun signInParsesCognitoTokens() = runTest {
    val dataSource =
        CognitoNativeAuthDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "AuthenticationResult": {
                            "IdToken": "id-token",
                            "AccessToken": "access-token",
                            "RefreshToken": "refresh-token",
                            "ExpiresIn": 3600,
                            "TokenType": "Bearer"
                          }
                        }
                        """
                            .trimIndent()
                ),
            config = config,
            json = serializer,
        )

    val response = dataSource.signIn(email = "david@example.com", password = "Password123!")

    assertEquals("id-token", response.idToken)
    assertEquals("access-token", response.accessToken)
    assertEquals("refresh-token", response.refreshToken)
    assertEquals(3600, response.expiresIn)
    assertEquals("Bearer", response.tokenType)
  }

  @Test
  fun signUpUsesCognitoSignUpTarget() = runTest {
    var requestedTarget: String? = null
    val dataSource =
        CognitoNativeAuthDataSource(
            httpClient =
                mockClient(
                    responseBody = "{}",
                    captureTarget = { requestedTarget = it },
                ),
            config = config,
            json = serializer,
        )

    dataSource.signUp(email = "david@example.com", password = "Password123!")

    assertEquals("AWSCognitoIdentityProviderService.SignUp", requestedTarget)
  }

  @Test
  fun mapsCognitoErrorsToSpanishMessages() = runTest {
    val dataSource =
        CognitoNativeAuthDataSource(
            httpClient =
                mockClient(
                    responseBody =
                        """
                        {
                          "__type": "UsernameExistsException",
                          "message": "User already exists"
                        }
                        """
                            .trimIndent(),
                    status = HttpStatusCode.BadRequest,
                ),
            config = config,
            json = serializer,
        )

    val error =
        assertFailsWith<CognitoNativeAuthException> {
          dataSource.signUp(email = "david@example.com", password = "Password123!")
        }

    assertEquals("Ese correo ya está registrado.", error.message)
  }

  @Test
  fun rejectsWeakPasswordsWithMissingRules() = runTest {
    val dataSource =
        CognitoNativeAuthDataSource(
            httpClient = mockClient(responseBody = "{}"),
            config = config,
            json = serializer,
        )

    val error =
        assertFailsWith<CognitoNativeAuthException> {
          dataSource.signUp(email = "david@example.com", password = "ADcc2023")
        }

    assertEquals("La contraseña debe tener al menos 12 caracteres y un símbolo.", error.message)
  }

  private fun mockClient(
      responseBody: String,
      status: HttpStatusCode = HttpStatusCode.OK,
      captureTarget: (String?) -> Unit = {},
  ): HttpClient =
      HttpClient(MockEngine) {
        expectSuccess = true
        engine {
          addHandler { request ->
            captureTarget(request.headers["X-Amz-Target"])
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          }
        }
        install(ContentNegotiation) {
          json(serializer)
          json(serializer, contentType = CognitoJsonContentType)
        }
      }
}
