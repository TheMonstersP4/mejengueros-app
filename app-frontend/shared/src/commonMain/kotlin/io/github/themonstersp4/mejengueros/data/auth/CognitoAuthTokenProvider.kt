package io.github.themonstersp4.mejengueros.data.auth

import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource

class CognitoAuthTokenProvider(private val localDataSource: IAuthLocalDataSource) :
    IAuthTokenProvider {
  override fun getBearerToken(): String? = localDataSource.getSession()?.idToken
}
