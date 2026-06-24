package io.github.themonstersp4.mejengueros.data.remote

interface IAuthenticatedUserRemoteDataSource {
  suspend fun syncCurrentUser()
}
