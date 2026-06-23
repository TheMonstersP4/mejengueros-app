package io.github.themonstersp4.mejengueros.data.remote

interface ICognitoNativeAuthDataSource {
  suspend fun signUp(email: String, password: String)

  suspend fun confirmSignUp(email: String, code: String)

  suspend fun resendConfirmationCode(email: String)

  suspend fun signIn(email: String, password: String): CognitoTokenResponseDto

  suspend fun forgotPassword(email: String)

  suspend fun confirmForgotPassword(email: String, code: String, newPassword: String)
}
