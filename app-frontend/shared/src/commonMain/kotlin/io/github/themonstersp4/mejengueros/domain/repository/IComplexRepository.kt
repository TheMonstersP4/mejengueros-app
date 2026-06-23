package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex

interface IComplexRepository {
  suspend fun createComplex(request: CreateComplexRequest): CreatedComplex
}
