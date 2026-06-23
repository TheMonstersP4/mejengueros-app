package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CognitoNativeAuthDataSource(
    private val httpClient: HttpClient,
    private val config: CognitoAuthConfig,
    private val json: Json,
) : ICognitoNativeAuthDataSource {
  private val endpoint = "https://cognito-idp.${config.region}.amazonaws.com"

  override suspend fun signUp(email: String, password: String) {
    validatePasswordPolicy(password)
    callWithoutBody(
        target = "AWSCognitoIdentityProviderService.SignUp",
        body =
            SignUpRequest(
                clientId = config.clientId,
                username = email,
                password = password,
                userAttributes = listOf(CognitoUserAttribute(name = "email", value = email)),
            ),
    )
  }

  override suspend fun confirmSignUp(email: String, code: String) {
    callWithoutBody(
        target = "AWSCognitoIdentityProviderService.ConfirmSignUp",
        body =
            ConfirmSignUpRequest(
                clientId = config.clientId,
                username = email,
                confirmationCode = code,
            ),
    )
  }

  override suspend fun resendConfirmationCode(email: String) {
    callWithoutBody(
        target = "AWSCognitoIdentityProviderService.ResendConfirmationCode",
        body = UsernameRequest(clientId = config.clientId, username = email),
    )
  }

  override suspend fun signIn(email: String, password: String): CognitoTokenResponseDto {
    val response =
        call<InitiateAuthRequest, InitiateAuthResponse>(
            target = "AWSCognitoIdentityProviderService.InitiateAuth",
            body =
                InitiateAuthRequest(
                    authFlow = UserPasswordAuthFlow,
                    clientId = config.clientId,
                    authParameters = mapOf("USERNAME" to email, "PASSWORD" to password),
                ),
        )

    return CognitoTokenResponseDto(
        idToken = response.authenticationResult.idToken,
        accessToken = response.authenticationResult.accessToken,
        refreshToken = response.authenticationResult.refreshToken,
        expiresIn = response.authenticationResult.expiresIn,
        tokenType = response.authenticationResult.tokenType,
    )
  }

  override suspend fun forgotPassword(email: String) {
    callWithoutBody(
        target = "AWSCognitoIdentityProviderService.ForgotPassword",
        body = UsernameRequest(clientId = config.clientId, username = email),
    )
  }

  override suspend fun confirmForgotPassword(email: String, code: String, newPassword: String) {
    validatePasswordPolicy(newPassword)
    callWithoutBody(
        target = "AWSCognitoIdentityProviderService.ConfirmForgotPassword",
        body =
            ConfirmForgotPasswordRequest(
                clientId = config.clientId,
                username = email,
                confirmationCode = code,
                password = newPassword,
            ),
    )
  }

  private suspend inline fun <reified TRequest : Any> callWithoutBody(
      target: String,
      body: TRequest,
  ) {
    send(target, body)
  }

  private suspend inline fun <reified TRequest : Any, reified TResponse : Any> call(
      target: String,
      body: TRequest,
  ): TResponse =
      try {
        send(target, body).body()
      } catch (error: ClientRequestException) {
        throw CognitoNativeAuthException.from(error.response.bodyAsText(), json)
      }

  private suspend inline fun <reified TRequest : Any> send(
      target: String,
      body: TRequest,
  ): HttpResponse =
      try {
        httpClient.post(endpoint) {
          contentType(CognitoJsonContentType)
          header("X-Amz-Target", target)
          setBody(body)
        }
      } catch (error: ClientRequestException) {
        throw CognitoNativeAuthException.from(error.response.bodyAsText(), json)
      }

  private fun validatePasswordPolicy(password: String) {
    val missingRules =
        listOfNotNull(
            "al menos $PasswordMinimumLength caracteres"
                .takeIf { password.length < PasswordMinimumLength },
            "una letra minúscula".takeIf { password.none(Char::isLowerCase) },
            "una letra mayúscula".takeIf { password.none(Char::isUpperCase) },
            "un número".takeIf { password.none(Char::isDigit) },
            "un símbolo".takeIf { password.none { it in PasswordSymbols } },
        )

    if (missingRules.isNotEmpty()) {
      throw CognitoNativeAuthException(
          "La contraseña debe tener ${missingRules.joinToNaturalText()}."
      )
    }
  }

  private fun List<String>.joinToNaturalText(): String =
      when (size) {
        1 -> first()
        2 -> joinToString(" y ")
        else -> dropLast(1).joinToString(", ") + " y " + last()
      }

  private companion object {
    const val PasswordMinimumLength = 12
    const val PasswordSymbols = "^${'$'}*.[]{}()?\"!@#%&/\\,><':;|_~`+=-"
    const val UserPasswordAuthFlow = "USER_PASSWORD_AUTH"
  }
}

class CognitoNativeAuthException(message: String) : IllegalStateException(message) {
  companion object {
    fun from(responseBody: String, json: Json): CognitoNativeAuthException {
      val error =
          runCatching { json.decodeFromString(CognitoErrorResponse.serializer(), responseBody) }
              .getOrNull()

      return CognitoNativeAuthException(
          error?.safeMessage() ?: "No se pudo completar la solicitud de autenticación."
      )
    }
  }
}

@Serializable
private data class CognitoErrorResponse(
    @SerialName("__type") val type: String? = null,
    @SerialName("message") val message: String? = null,
) {
  fun safeMessage(): String =
      when (type?.substringAfterLast('#')) {
        "UsernameExistsException" -> "Ese correo ya está registrado."
        "InvalidPasswordException" ->
            "La contraseña debe tener al menos 12 caracteres, una letra minúscula, una letra mayúscula, un número y un símbolo."
        "CodeMismatchException" -> "El código de verificación no es válido."
        "ExpiredCodeException" -> "El código de verificación expiró."
        "UserNotConfirmedException" -> "Confirmá tu correo antes de iniciar sesión."
        "NotAuthorizedException" -> "El correo o la contraseña no son válidos."
        "InvalidParameterException" ->
            "No se pudo iniciar sesión. Revisá el correo y la contraseña e intentá de nuevo."
        "TooManyRequestsException",
        "LimitExceededException" -> "Demasiados intentos. Esperá un momento e intentá de nuevo."
        else -> message ?: "No se pudo completar la solicitud de autenticación."
      }
}

@Serializable
private data class SignUpRequest(
    @SerialName("ClientId") val clientId: String,
    @SerialName("Username") val username: String,
    @SerialName("Password") val password: String,
    @SerialName("UserAttributes") val userAttributes: List<CognitoUserAttribute>,
)

@Serializable
private data class CognitoUserAttribute(
    @SerialName("Name") val name: String,
    @SerialName("Value") val value: String,
)

@Serializable
private data class ConfirmSignUpRequest(
    @SerialName("ClientId") val clientId: String,
    @SerialName("Username") val username: String,
    @SerialName("ConfirmationCode") val confirmationCode: String,
)

@Serializable
private data class UsernameRequest(
    @SerialName("ClientId") val clientId: String,
    @SerialName("Username") val username: String,
)

@Serializable
private data class InitiateAuthRequest(
    @SerialName("AuthFlow") val authFlow: String,
    @SerialName("ClientId") val clientId: String,
    @SerialName("AuthParameters") val authParameters: Map<String, String>,
)

@Serializable
private data class InitiateAuthResponse(
    @SerialName("AuthenticationResult") val authenticationResult: AuthenticationResultResponse,
)

@Serializable
private data class AuthenticationResultResponse(
    @SerialName("IdToken") val idToken: String,
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("RefreshToken") val refreshToken: String,
    @SerialName("ExpiresIn") val expiresIn: Long,
    @SerialName("TokenType") val tokenType: String,
)

@Serializable
private data class ConfirmForgotPasswordRequest(
    @SerialName("ClientId") val clientId: String,
    @SerialName("Username") val username: String,
    @SerialName("ConfirmationCode") val confirmationCode: String,
    @SerialName("Password") val password: String,
)
