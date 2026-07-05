package io.github.themonstersp4.mejengueros.navigation

import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaDateLabel
import io.github.themonstersp4.mejengueros.domain.time.toCostaRicaTimeLabel
import io.github.themonstersp4.mejengueros.screens.review.LeaveReviewReservationContext

internal fun ReviewableReservation.toLeaveReviewReservationContext():
    LeaveReviewReservationContext =
    LeaveReviewReservationContext(
        title = "$complexName · $courtName",
        reservationLabel =
            "Reserva del ${startsAt.toCostaRicaDateLabel()} · ${startsAt.toCostaRicaTimeLabel()} – ${endsAt.toCostaRicaTimeLabel()}",
        imageUrl = imageUrl,
        imageContentDescription = courtName,
    )
