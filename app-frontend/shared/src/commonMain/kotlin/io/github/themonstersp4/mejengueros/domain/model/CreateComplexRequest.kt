package io.github.themonstersp4.mejengueros.domain.model

data class CreateComplexRequest(
    val complex: CreateComplexDetails,
    val firstCourt: CreateFirstCourtDetails,
)

data class CreateComplexDetails(
    val name: String,
    val provinceId: String,
    val cantonId: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val serviceIds: List<String>,
)

data class CreateFirstCourtDetails(
    val name: String,
    val serviceIds: List<String>,
)
