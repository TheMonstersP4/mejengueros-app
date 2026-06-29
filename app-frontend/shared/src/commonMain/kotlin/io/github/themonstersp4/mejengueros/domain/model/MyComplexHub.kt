package io.github.themonstersp4.mejengueros.domain.model

enum class CourtAvailabilitySetupStatus {
  CONFIGURED,
  PENDING,
}

data class MyComplexHub(
    val complexes: List<MyComplexHubComplex>,
)

data class MyComplexHubComplex(
    val id: String,
    val name: String,
    val address: String,
    val provinceId: String?,
    val cantonId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String,
    val courts: List<MyComplexHubCourt>,
)

data class MyComplexHubCourt(
    val id: String,
    val name: String,
    val status: String,
    val availabilityStatus: CourtAvailabilitySetupStatus,
)
