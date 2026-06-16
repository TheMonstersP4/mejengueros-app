package io.github.themonstersp4.mejengueros.data.remote

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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class PokemonRemoteDataSourceTest {

  @Test
  fun getPokemonPageMapsPagedResponse() = runTest {
    val dataSource =
        PokemonRemoteDataSource(
            mockClient(
                """
                {
                  "count": 1302,
                  "next": "https://pokeapi.co/api/v2/pokemon?limit=20&offset=20",
                  "previous": null,
                  "results": [
                    {
                      "name": "bulbasaur",
                      "url": "https://pokeapi.co/api/v2/pokemon/1/"
                    }
                  ]
                }
                """
                    .trimIndent()
            )
        )

    val page = dataSource.getPokemonPage(limit = 20, offset = 0)

    assertEquals(20, page.nextOffset)
    assertEquals(1, page.items.single().id)
    assertEquals("bulbasaur", page.items.single().name)
  }

  @Test
  fun getPokemonPageWithQueryScansRemoteListAndReturnsNextMatchOffset() = runTest {
    val dataSource =
        PokemonRemoteDataSource(
            mockClient(
                """
                {
                  "count": 1302,
                  "next": "https://pokeapi.co/api/v2/pokemon?limit=100&offset=100",
                  "previous": null,
                  "results": [
                    {
                      "name": "bulbasaur",
                      "url": "https://pokeapi.co/api/v2/pokemon/1/"
                    },
                    {
                      "name": "charmander",
                      "url": "https://pokeapi.co/api/v2/pokemon/4/"
                    }
                  ]
                }
                """
                    .trimIndent()
            )
        )

    val page = dataSource.getPokemonPage(limit = 1, offset = 0, query = "saur")

    assertEquals(1, page.nextOffset)
    assertEquals(1, page.items.single().id)
    assertEquals("bulbasaur", page.items.single().name)
  }

  @Test
  fun getPokemonPageWithQueryUsesMatchOffsetWithoutSkippingDenseMatches() = runTest {
    val denseResponse = denseSaurResponse(matchCount = 25)
    val dataSource = PokemonRemoteDataSource(mockClient(denseResponse, denseResponse))

    val firstPage = dataSource.getPokemonPage(limit = 20, offset = 0, query = "saur")
    val secondPage = dataSource.getPokemonPage(limit = 20, offset = 20, query = "saur")

    assertEquals((1..20).toList(), firstPage.items.map { it.id })
    assertEquals(20, firstPage.nextOffset)
    assertEquals((21..25).toList(), secondPage.items.map { it.id })
    assertEquals(null, secondPage.nextOffset)
  }

  @Test
  fun getPokemonPageWithQueryScansUntilEnoughMatchesOrEndReached() = runTest {
    val dataSource =
        PokemonRemoteDataSource(
            mockClient(
                """
                {
                  "count": 1302,
                  "next": "https://pokeapi.co/api/v2/pokemon?limit=100&offset=100",
                  "previous": null,
                  "results": [
                    {
                      "name": "charmander",
                      "url": "https://pokeapi.co/api/v2/pokemon/4/"
                    }
                  ]
                }
                """
                    .trimIndent(),
                """
                {
                  "count": 1302,
                  "next": null,
                  "previous": "https://pokeapi.co/api/v2/pokemon?limit=100&offset=0",
                  "results": [
                    {
                      "name": "ivysaur",
                      "url": "https://pokeapi.co/api/v2/pokemon/2/"
                    },
                    {
                      "name": "venusaur",
                      "url": "https://pokeapi.co/api/v2/pokemon/3/"
                    }
                  ]
                }
                """
                    .trimIndent(),
            )
        )

    val page = dataSource.getPokemonPage(limit = 2, offset = 0, query = "saur")

    assertEquals(null, page.nextOffset)
    assertEquals(listOf(2, 3), page.items.map { it.id })
  }

  @Test
  fun getPokemonDetailMapsDetailResponse() = runTest {
    val dataSource =
        PokemonRemoteDataSource(
            mockClient(
                """
                {
                  "id": 1,
                  "name": "bulbasaur",
                  "height": 7,
                  "weight": 69,
                  "sprites": {
                    "front_default": "https://example.com/front.png",
                    "other": {
                      "official-artwork": {
                        "front_default": "https://example.com/official.png"
                      }
                    }
                  },
                  "types": [
                    { "slot": 2, "type": { "name": "poison" } },
                    { "slot": 1, "type": { "name": "grass" } }
                  ]
                }
                """
                    .trimIndent()
            )
        )

    val detail = dataSource.getPokemonDetail(1)

    assertEquals(1, detail.id)
    assertEquals("bulbasaur", detail.name)
    assertEquals(7, detail.height)
    assertEquals(69, detail.weight)
    assertEquals("https://example.com/official.png", detail.imageUrl)
    assertEquals(listOf("grass", "poison"), detail.types)
  }

  private fun denseSaurResponse(matchCount: Int): String {
    val results =
        (1..matchCount).joinToString(separator = ",") { id ->
          """
          {
            "name": "dense-saur-$id",
            "url": "https://pokeapi.co/api/v2/pokemon/$id/"
          }
          """
              .trimIndent()
        }

    return """
      {
        "count": 1302,
        "next": null,
        "previous": null,
        "results": [$results]
      }
      """
        .trimIndent()
  }

  private fun mockClient(vararg responseBodies: String): HttpClient =
      HttpClient(MockEngine) {
        engine {
          val queuedResponses = responseBodies.toMutableList()
          addHandler {
            respond(
                content = queuedResponses.removeAt(0),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
          }
        }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      }
}
