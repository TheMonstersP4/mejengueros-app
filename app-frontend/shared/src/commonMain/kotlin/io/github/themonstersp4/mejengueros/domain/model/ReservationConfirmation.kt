package io.github.themonstersp4.mejengueros.domain.model

data class ReservationConfirmation(
    val id: String,
    val courtId: String,
    val startsAtUtc: String,
    val endsAtUtc: String,
    val status: String,
)
