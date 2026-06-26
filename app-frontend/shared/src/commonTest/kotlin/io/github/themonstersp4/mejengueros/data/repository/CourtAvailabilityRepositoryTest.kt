package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtAvailabilityRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class CourtAvailabilityRepositoryTest {
  @Test
  fun getCourtAvailabilityDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtAvailabilityRemoteDataSource()
    val repository = CourtAvailabilityRepository(remoteDataSource)

    val context = repository.getCourtAvailability("court-id")

    assertEquals(listOf("get:court-id"), remoteDataSource.calls)
    assertEquals(fakeContext, context)
  }

  @Test
  fun saveCourtAvailabilityDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtAvailabilityRemoteDataSource()
    val repository = CourtAvailabilityRepository(remoteDataSource)

    val context = repository.saveCourtAvailability("court-id", fakeAvailability)

    assertEquals(listOf("save:court-id"), remoteDataSource.calls)
    assertEquals(listOf(fakeAvailability), remoteDataSource.savedAvailabilities)
    assertEquals(fakeContext, context)
  }

  @Test
  fun saveCourtAvailabilityPropagatesRemoteFailures() = runTest {
    val remoteDataSource =
        FakeCourtAvailabilityRemoteDataSource(saveError = IllegalStateException("save failed"))
    val repository = CourtAvailabilityRepository(remoteDataSource)

    val error =
        assertFailsWith<IllegalStateException> {
          repository.saveCourtAvailability("court-id", fakeAvailability)
        }

    assertEquals("save failed", error.message)
    assertEquals(listOf("save:court-id"), remoteDataSource.calls)
  }
}

private class FakeCourtAvailabilityRemoteDataSource(
    private val saveError: Throwable? = null,
) : ICourtAvailabilityRemoteDataSource {
  val calls = mutableListOf<String>()
  val savedAvailabilities = mutableListOf<CourtAvailabilityConfig>()

  override suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext {
    calls.add("get:$courtId")
    return fakeContext
  }

  override suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext {
    calls.add("save:$courtId")
    savedAvailabilities.add(availability)
    saveError?.let { throw it }
    return fakeContext
  }
}

private val fakeAvailability =
    CourtAvailabilityConfig(
        days = listOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.FRIDAY),
        startTime = "07:00",
        endTime = "10:00",
    )

private val fakeContext =
    CourtAvailabilityContext(
        courtId = "court-id",
        courtName = "Cancha 1",
        complexName = "Mejengas CR",
        availability = fakeAvailability,
    )
