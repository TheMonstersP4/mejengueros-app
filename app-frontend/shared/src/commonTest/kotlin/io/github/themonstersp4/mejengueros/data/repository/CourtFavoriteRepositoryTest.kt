package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.local.ICourtFavoriteLocalDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class CourtFavoriteRepositoryTest {
  @Test
  fun readsAndWritesOnlyThroughLocalDataSource() = runTest {
    val localDataSource = RecordingCourtFavoriteLocalDataSource(favorite = true)
    val repository = CourtFavoriteRepository(localDataSource)

    assertTrue(repository.isFavorite("user-1", "court-1"))
    repository.setFavorite("user-1", "court-1", false)
    assertEquals(listOf("court-1"), repository.observeFavoriteCourtIds("user-1").first())

    assertEquals(listOf("user-1" to "court-1"), localDataSource.reads)
    assertEquals(listOf(Triple("user-1", "court-1", false)), localDataSource.writes)
  }

  @Test
  fun localFailuresArePropagated() = runTest {
    val repository =
        CourtFavoriteRepository(
            object : ICourtFavoriteLocalDataSource {
              override fun isFavorite(userId: String, courtId: String): Boolean =
                  error("local read failed")

              override fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) =
                  error("local write failed")

              override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> =
                  error("local observe failed")
            }
        )

    assertFailsWith<IllegalStateException> { repository.isFavorite("user-1", "court-1") }
    assertFailsWith<IllegalStateException> { repository.setFavorite("user-1", "court-1", true) }
    assertFailsWith<IllegalStateException> { repository.observeFavoriteCourtIds("user-1") }
  }
}

private class RecordingCourtFavoriteLocalDataSource(
    private val favorite: Boolean,
) : ICourtFavoriteLocalDataSource {
  val reads = mutableListOf<Pair<String, String>>()
  val writes = mutableListOf<Triple<String, String, Boolean>>()

  override fun isFavorite(userId: String, courtId: String): Boolean {
    reads += userId to courtId
    return favorite
  }

  override fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
    writes += Triple(userId, courtId, isFavorite)
  }

  override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> =
      flowOf(listOf("court-1"))
}
