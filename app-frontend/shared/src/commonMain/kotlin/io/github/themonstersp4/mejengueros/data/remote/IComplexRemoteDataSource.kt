package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope

interface IComplexRemoteDataSource {
  suspend fun getProvinces(): List<Province>

  suspend fun getCantons(provinceId: String): List<Canton>

  suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem>

  suspend fun createComplex(request: CreateComplexRequest): CreatedComplex

  suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt

  suspend fun getMyComplexHub(): MyComplexHub
}
