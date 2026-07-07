package io.github.themonstersp4.mejengueros.screens.courtdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailSlot
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomActionBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReceivedReviewCard
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosThumbnail
import kotlin.math.roundToInt

@Composable
fun CourtDetailScreen(
    courtName: String,
    complexName: String,
    provinceName: String,
    cantonName: String,
    services: List<String>,
    ratingAverage: Double?,
    ratingCount: Int,
    imageUrl: String?,
    state: CourtDetailUiState,
    contentPadding: PaddingValues,
    onReserve: () -> Unit,
    onRetrySlots: () -> Unit,
    onRetryReviews: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
    ) {
      CourtHeroImage(
          imageUrl = imageUrl,
          contentDescription = "$complexName · $courtName",
      )

      CourtHeadSection(
          courtName = "$complexName · $courtName",
          provinceName = provinceName,
          cantonName = cantonName,
          ratingAverage = ratingAverage,
          ratingCount = ratingCount,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      DisponibilidadSection(
          state = state,
          onRetry = onRetrySlots,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      ServiciosSection(services = services)

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      UbicacionSection(
          provinceName = provinceName,
          cantonName = cantonName,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      ReseñasSection(
          state = state,
          onRetry = onRetryReviews,
      )

      Spacer(modifier = Modifier.height(8.dp))
    }

    MejenguerosBottomActionBar {
      MejenguerosFullWidthPrimaryButton(
          text = "Reservar cancha",
          onClick = onReserve,
          modifier = Modifier.testTag("court_detail_reserve_button"),
      )
    }
  }
}

@Composable
private fun CourtHeroImage(
    imageUrl: String?,
    contentDescription: String,
) {
  MejenguerosThumbnail(
      imageUrl = imageUrl,
      contentDescription = contentDescription,
      modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
      size = null,
      shape = RoundedCornerShape(0.dp),
  )
}

@Composable
private fun CourtHeadSection(
    courtName: String,
    provinceName: String,
    cantonName: String,
    ratingAverage: Double?,
    ratingCount: Int,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
        text = courtName,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("court_detail_title"),
    )

    if (ratingAverage != null && ratingCount > 0) {
      RatingRow(average = ratingAverage, count = ratingCount)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
      )
      Text(
          text = "$provinceName · $cantonName",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("court_detail_location"),
      )
    }
  }
}

@Composable
private fun RatingRow(average: Double, count: Int) {
  val rounded = (average * 10).roundToInt() / 10.0
  val averageLabel = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()

  Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
    )
    Text(
        text = "$averageLabel · $count reseñas",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("court_detail_rating"),
    )
  }
}

@Composable
private fun DisponibilidadSection(
    state: CourtDetailUiState,
    onRetry: () -> Unit,
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 20.dp)
              .testTag("court_detail_disponibilidad_section"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
        text = "Disponibilidad",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )

    when {
      state.isLoadingSlots -> {
        MejenguerosInlineLoadingState(
            text = "Cargando disponibilidad…",
            modifier = Modifier.fillMaxWidth(),
            containerTestTag = "court_detail_loading_slots",
            indicatorTestTag = "court_detail_loading_slots_indicator",
        )
      }

      state.slotsErrorMessage != null -> {
        MejenguerosStateContent(
            title = "Sin disponibilidad",
            description = state.slotsErrorMessage,
            variant = MejenguerosStateVariant.Error,
            actions = {
              MejenguerosOutlinedButton(
                  text = "Reintentar",
                  onClick = onRetry,
                  modifier = Modifier.testTag("court_detail_retry_slots_button"),
              )
            },
        )
      }

      state.slots.isEmpty() -> {
        MejenguerosStateContent(
            title = "Sin horarios próximos",
            description =
                "No encontramos horarios disponibles en los próximos días. Tocá \"Reservar cancha\" para revisar más fechas.",
            variant = MejenguerosStateVariant.Empty,
            modifier = Modifier.testTag("court_detail_no_slots_state"),
        )
      }

      else -> {
        AvailabilityPill(label = state.availabilityHeadline ?: "Próximo horario disponible")
        SlotGrid(slots = state.slots)
      }
    }
  }
}

@Composable
private fun AvailabilityPill(label: String) {
  Surface(
      shape = RoundedCornerShape(999.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
          modifier =
              Modifier.size(8.dp)
                  .background(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(999.dp),
                  )
      )
      Text(
          text = label,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotGrid(slots: List<CourtDetailSlot>) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    slots.forEach { slot -> SlotChip(label = slot.displayTime, isTaken = false) }
  }
}

@Composable
private fun SlotChip(label: String, isTaken: Boolean) {
  val containerColor =
      if (isTaken) MaterialTheme.colorScheme.surfaceContainerHigh
      else MaterialTheme.colorScheme.surface
  val contentColor =
      if (isTaken) MaterialTheme.colorScheme.onSurfaceVariant
      else MaterialTheme.colorScheme.onSurface
  val textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None

  Surface(
      shape = MaterialTheme.shapes.medium,
      color = containerColor,
      contentColor = contentColor,
      tonalElevation = 0.dp,
  ) {
    Text(
        text = label,
        modifier =
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("court_detail_slot_$label"),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        textDecoration = textDecoration,
    )
  }
}

@Composable
private fun ServiciosSection(services: List<String>) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 20.dp)
              .testTag("court_detail_servicios_section"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
        text = "Servicios",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )

    if (services.isEmpty()) {
      Text(
          text = "Servicios por confirmar.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        services.forEach { service -> ServiceRow(name = service) }
      }
    }
  }
}

@Composable
private fun ServiceRow(name: String) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
      }
    }
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun UbicacionSection(
    provinceName: String,
    cantonName: String,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
        text = "Ubicación",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )

    MapPlaceholder()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
      )
      Text(
          text = "$provinceName · $cantonName",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ReseñasSection(
    state: CourtDetailUiState,
    onRetry: () -> Unit,
) {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 20.dp)
              .testTag("court_detail_resenas_section"),
      verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
        text = "Reseñas",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )

    when {
      state.isLoadingReviews -> {
        MejenguerosInlineLoadingState(
            text = "Cargando reseñas…",
            modifier = Modifier.fillMaxWidth(),
            containerTestTag = "court_detail_loading_reviews",
            indicatorTestTag = "court_detail_loading_reviews_indicator",
        )
      }

      state.reviewsErrorMessage != null -> {
        MejenguerosStateContent(
            title = "No pudimos cargar las reseñas",
            description = state.reviewsErrorMessage,
            variant = MejenguerosStateVariant.Error,
            actions = {
              MejenguerosOutlinedButton(
                  text = "Reintentar",
                  onClick = onRetry,
                  modifier = Modifier.testTag("court_detail_retry_reviews_button"),
              )
            },
        )
      }

      state.reviews.isEmpty() -> {
        MejenguerosStateContent(
            title = "Todavía no hay reseñas",
            description =
                "Esta cancha aún no tiene reseñas. Sé el primero en dejar una después de jugar acá.",
            variant = MejenguerosStateVariant.Empty,
            modifier = Modifier.testTag("court_detail_no_reviews_state"),
        )
      }

      else -> {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          state.reviews.forEach { review ->
            MejenguerosReceivedReviewCard(
                author = review.authorName,
                date = review.dateLabel.orEmpty(),
                rating = review.rating,
                comment = review.comment.orEmpty(),
                avatarInitials = review.authorInitials,
                modifier = Modifier.testTag("court_detail_review_${review.id}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MapPlaceholder() {
  Surface(
      modifier = Modifier.fillMaxWidth().height(140.dp),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp),
      )
    }
  }
}
