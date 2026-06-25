package io.github.themonstersp4.mejengueros.domain.model

import kotlinx.serialization.Serializable

data class ServiceCatalogItem(
    val id: String,
    val name: String,
    val scope: ServiceScope,
)

@Serializable
enum class ServiceScope {
  COMPLEX,
  COURT,
}
