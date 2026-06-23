package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository

class ComplexRepository(private val remoteDataSource: IComplexRemoteDataSource) :
    IComplexRepository {
  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
    return remoteDataSource.createComplex(request)
  }
}
