package example.test

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class RemoteLaunchDataSourceTest {
    @Test
    fun decodesLaunches() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val engine = MockEngine {
            respond(
                content = """[{"flight_number":1,"name":"Demo"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val dataSource = KtorLaunchDataSource(client, dispatcher)

        assertEquals(1, dataSource.latestLaunches().first().size)
    }
}
