package example.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

interface LaunchRepository {
    val latestLaunches: Flow<List<Launch>>
}

class DefaultLaunchRepository(
    private val remote: RemoteLaunchDataSource,
    private val local: LocalLaunchDataSource,
    private val defaultDispatcher: CoroutineDispatcher,
) : LaunchRepository {
    override val latestLaunches: Flow<List<Launch>> =
        remote.latestLaunches()
            .onEach { launches -> local.replaceAll(launches) }
            .flowOn(defaultDispatcher)
            .catch {
                val cached = local.getAll()
                if (cached.isNotEmpty()) emit(cached) else throw it
            }
}
