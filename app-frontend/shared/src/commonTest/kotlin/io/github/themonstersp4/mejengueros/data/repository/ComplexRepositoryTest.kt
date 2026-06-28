package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexDetails
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateFirstCourtDetails
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class ComplexRepositoryTest {

  @Test
  fun getProvincesDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeComplexRemoteDataSource()
    val repository = ComplexRepository(remoteDataSource)

    val provinces = repository.getProvinces()

    assertEquals(listOf("getProvinces"), remoteDataSource.calls)
    assertEquals(fakeProvinces, provinces)
  }

  @Test
  fun getCantonsDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeComplexRemoteDataSource()
    val repository = ComplexRepository(remoteDataSource)

    val cantons = repository.getCantons("province-1")

    assertEquals(listOf("getCantons"), remoteDataSource.calls)
    assertEquals(listOf("province-1"), remoteDataSource.cantonRequests)
    assertEquals(fakeCantons, cantons)
  }

  @Test
  fun getServicesDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeComplexRemoteDataSource()
    val repository = ComplexRepository(remoteDataSource)

    val services = repository.getServices(ServiceScope.COURT)

    assertEquals(listOf("getServices"), remoteDataSource.calls)
    assertEquals(listOf(ServiceScope.COURT), remoteDataSource.serviceRequests)
    assertEquals(fakeServices, services)
  }

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

  @Test
  fun getMyComplexHubDelegatesDirectlyToRemoteDataSource() = runTest {
    val remoteDataSource = FakeComplexRemoteDataSource()
    val repository = ComplexRepository(remoteDataSource)

    val hub = repository.getMyComplexHub()

    assertEquals(listOf("getMyComplexHub"), remoteDataSource.calls)
    assertEquals(myComplexHub, hub)
  }

  @Test
  fun getMyComplexHubPropagatesRemoteFailures() = runTest {
    val remoteDataSource =
        FakeComplexRemoteDataSource(getMyComplexHubFailure = IllegalStateException("hub failed"))
    val repository = ComplexRepository(remoteDataSource)

    val error = assertFailsWith<IllegalStateException> { repository.getMyComplexHub() }

    assertEquals("hub failed", error.message)
    assertEquals(listOf("getMyComplexHub"), remoteDataSource.calls)
  }

  private class FakeComplexRemoteDataSource(
      private val createFailure: Throwable? = null,
      private val createResult: CreatedComplex = createdComplex,
      private val getMyComplexHubFailure: Throwable? = null,
      private val myComplexHubResult: MyComplexHub = myComplexHub,
  ) : IComplexRemoteDataSource {
    val calls = mutableListOf<String>()
    val cantonRequests = mutableListOf<String>()
    val serviceRequests = mutableListOf<ServiceScope>()
    val createRequests = mutableListOf<CreateComplexRequest>()

    override suspend fun getProvinces(): List<Province> {
      calls.add("getProvinces")
      return fakeProvinces
    }

    override suspend fun getCantons(provinceId: String): List<Canton> {
      calls.add("getCantons")
      cantonRequests.add(provinceId)
      return fakeCantons
    }

    override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> {
      calls.add("getServices")
      serviceRequests.add(scope)
      return fakeServices
    }

    override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
      calls.add("createComplex")
      createRequests.add(request)
      createFailure?.let { throw it }
      return createResult
    }

    override suspend fun getMyComplexHub(): MyComplexHub {
      calls.add("getMyComplexHub")
      getMyComplexHubFailure?.let { throw it }
      return myComplexHubResult
    }
  }

  private companion object {
    val fakeProvinces = listOf(Province(id = "province-1", code = "SJ", name = "San José"))
    val fakeCantons =
        listOf(Canton(id = "canton-1", provinceId = "province-1", code = "SJ-ESC", name = "Escazú"))
    val fakeServices =
        listOf(
            ServiceCatalogItem(
                id = "court-service-id",
                name = "Lighting",
                scope = ServiceScope.COURT,
            )
        )

    val createComplexRequest =
        CreateComplexRequest(
            complex =
                CreateComplexDetails(
                    name = "North Sports Center",
                    provinceId = "province-1",
                    cantonId = "canton-1",
                    address = "123 Main Street",
                    latitude = 9.935,
                    longitude = -84.091,
                    serviceIds = listOf("complex-service-id"),
                ),
            firstCourt =
                CreateFirstCourtDetails(
                    name = "Court A",
                    serviceIds = listOf("court-service-id"),
                ),
        )

    val createdComplex =
        CreatedComplex(
            complexId = "complex-id",
            complexName = "North Sports Center",
            complexAddress = "123 Main Street",
            firstCourtId = "court-id",
            firstCourtName = "Court A",
        )

    val myComplexHub =
        MyComplexHub(
            complexes =
                listOf(
                    MyComplexHubComplex(
                        id = "complex-id",
                        name = "North Sports Center",
                        address = "123 Main Street",
                        provinceId = "province-1",
                        cantonId = "canton-1",
                        latitude = 9.935,
                        longitude = -84.091,
                        status = "ACTIVE",
                        courts =
                            listOf(
                                MyComplexHubCourt(
                                    id = "court-id",
                                    name = "Court A",
                                    status = "ACTIVE",
                                    availabilityStatus = CourtAvailabilitySetupStatus.CONFIGURED,
                                )
                            ),
                    )
                )
        )
  }
}
