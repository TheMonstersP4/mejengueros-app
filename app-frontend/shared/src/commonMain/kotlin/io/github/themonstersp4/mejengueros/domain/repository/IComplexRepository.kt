package io.github.themonstersp4.mejengueros.domain.repository

import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope

interface IComplexRepository {
  suspend fun getProvinces(): List<Province>

  suspend fun getCantons(provinceId: String): List<Canton>

  suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem>

  suspend fun createComplex(request: CreateComplexRequest): CreatedComplex
}
