package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CreateComplexViewModelTest {
  @Test
  fun submitCreatesComplexAndResetsFormIntoSuccessState() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)

    viewModel.updateComplexName("North Sports Center")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.updateFirstCourtName("Court A")

    viewModel.submit()
    advanceUntilIdle()

    assertEquals("North Sports Center", repository.receivedRequest?.complexName)
    assertEquals(
        "Complejo y primera cancha creados correctamente.",
        viewModel.uiState.value.successMessage,
    )
    assertEquals("complex-id", viewModel.uiState.value.createdComplex?.complexId)
    assertEquals("", viewModel.uiState.value.complexName)
    scope.cancel()
  }

  @Test
  fun submitMapsOwnerForbiddenIntoActionableMessage() = runTest {
    val repository =
        FakeComplexRepository(
            failure = AppApiException(403, "Only users with the OWNER role can create complexes.")
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)

    viewModel.updateComplexName("North Sports Center")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.updateFirstCourtName("Court A")

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "Tu cuenta está autenticada, pero todavía no tiene el rol OWNER local. Pedí la provisión demo e intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertNull(viewModel.uiState.value.successMessage)
    scope.cancel()
  }

  @Test
  fun submitBlocksInvalidStateForEmptyAndWhitespaceFieldsWithoutCallingRepository() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)

    viewModel.submit()
    advanceUntilIdle()

    assertNull(repository.receivedRequest)
    assertEquals(
        "Completá nombre del complejo, dirección y nombre de la primera cancha.",
        viewModel.uiState.value.errorMessage,
    )

    viewModel.updateComplexName("   ")
    viewModel.updateComplexAddress("\t")
    viewModel.updateFirstCourtName("\n")

    viewModel.submit()
    advanceUntilIdle()

    assertNull(repository.receivedRequest)
    assertEquals(
        "Completá nombre del complejo, dirección y nombre de la primera cancha.",
        viewModel.uiState.value.errorMessage,
    )
    scope.cancel()
  }

  private class FakeComplexRepository(private val failure: Throwable? = null) : IComplexRepository {
    var receivedRequest: CreateComplexRequest? = null

    override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
      receivedRequest = request
      failure?.let { throw it }
      return CreatedComplex(
          complexId = "complex-id",
          complexName = request.complexName,
          complexAddress = request.complexAddress,
          firstCourtId = "court-id",
          firstCourtName = request.firstCourtName,
      )
    }
  }
}
