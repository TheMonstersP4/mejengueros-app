package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex

interface IComplexRemoteDataSource {
  suspend fun createComplex(request: CreateComplexRequest): CreatedComplex
}
