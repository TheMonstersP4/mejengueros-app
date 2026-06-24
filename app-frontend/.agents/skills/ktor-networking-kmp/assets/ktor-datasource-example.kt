package example.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class KtorLaunchDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : RemoteLaunchDataSource {
    override fun latestLaunches(): Flow<List<LaunchDto>> =
        flow {
            emit(httpClient.get("https://example.com/launches").body<List<LaunchDto>>())
        }.flowOn(ioDispatcher)
}
