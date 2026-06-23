package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class ComplexRepositoryTest {

  @Test
  fun createComplexDelegatesDirectlyToRemoteDataSource() = runTest {
    val remoteDataSource = FakeComplexRemoteDataSource()
    val repository = ComplexRepository(remoteDataSource)

    val created = repository.createComplex(createComplexRequest)

    assertEquals(listOf("createComplex"), remoteDataSource.calls)
    assertEquals(createdComplex, created)
    assertEquals(listOf(createComplexRequest), remoteDataSource.createRequests)
  }

  @Test
  fun createComplexPropagatesCreateFailures() = runTest {
    val remoteDataSource =
        FakeComplexRemoteDataSource(createFailure = IllegalStateException("create failed"))
    val repository = ComplexRepository(remoteDataSource)

    val error =
        assertFailsWith<IllegalStateException> { repository.createComplex(createComplexRequest) }

    assertEquals("create failed", error.message)
    assertEquals(listOf("createComplex"), remoteDataSource.calls)
    assertEquals(listOf(createComplexRequest), remoteDataSource.createRequests)
  }

  private class FakeComplexRemoteDataSource(
      private val createFailure: Throwable? = null,
      private val createResult: CreatedComplex = createdComplex,
  ) : IComplexRemoteDataSource {
    val calls = mutableListOf<String>()
    val createRequests = mutableListOf<CreateComplexRequest>()

    override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
      calls.add("createComplex")
      createRequests.add(request)
      createFailure?.let { throw it }
      return createResult
    }
  }

  private companion object {
    val createComplexRequest =
        CreateComplexRequest(
            complexName = "North Sports Center",
            complexAddress = "123 Main Street",
            firstCourtName = "Court A",
        )

    val createdComplex =
        CreatedComplex(
            complexId = "complex-id",
            complexName = "North Sports Center",
            complexAddress = "123 Main Street",
            firstCourtId = "court-id",
            firstCourtName = "Court A",
        )
  }
}
