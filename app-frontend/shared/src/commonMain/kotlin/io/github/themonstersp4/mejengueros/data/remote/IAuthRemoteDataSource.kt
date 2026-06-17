package io.github.themonstersp4.mejengueros.data.remote

interface IAuthRemoteDataSource {
  suspend fun exchangeCode(code: String, codeVerifier: String): CognitoTokenResponseDto
}
