package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository

class ComplexRepository(private val remoteDataSource: IComplexRemoteDataSource) :
    IComplexRepository {
  override suspend fun getProvinces(): List<Province> {
    return remoteDataSource.getProvinces()
  }

  override suspend fun getCantons(provinceId: String): List<Canton> {
    return remoteDataSource.getCantons(provinceId)
  }

  override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> {
    return remoteDataSource.getServices(scope)
  }

  override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
    return remoteDataSource.createComplex(request)
  }

  override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt {
    return remoteDataSource.addCourt(complexId, request)
  }

  override suspend fun updateCourtImage(
      complexId: String,
      courtId: String,
      imageUploadId: String,
  ) = remoteDataSource.updateCourtImage(complexId, courtId, imageUploadId)

  override suspend fun getMyComplexHub(): MyComplexHub {
    return remoteDataSource.getMyComplexHub()
  }
}
