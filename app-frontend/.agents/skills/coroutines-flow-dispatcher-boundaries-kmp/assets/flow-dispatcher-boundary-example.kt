package example.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteSource(private val ioDispatcher: CoroutineDispatcher) {
    fun load(): Flow<List<Dto>> =
        flow { emit(fetchDtos()) }
            .flowOn(ioDispatcher) // owns network I/O upstream
}

class Repository(
    private val remote: RemoteSource,
    private val local: LocalSource,
    private val defaultDispatcher: CoroutineDispatcher,
) {
    fun items(): Flow<List<Item>> =
        remote.load()
            .map { dtos -> dtos.map { it.toDomain() } }
            .flowOn(defaultDispatcher) // owns mapping/cache coordination upstream
            .catch {
                val cached = local.readAll()
                if (cached.isNotEmpty()) emit(cached) else throw it
            }
}

class RepositoryTest {
    @Test
    fun usesDeterministicDispatcher() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = Repository(FakeRemote(dispatcher), FakeLocal(), dispatcher)
        assertEquals(1, repository.items().first().size)
    }
}
