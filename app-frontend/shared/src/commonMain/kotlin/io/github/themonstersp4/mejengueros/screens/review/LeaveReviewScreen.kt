package io.github.themonstersp4.mejengueros.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomActionBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosRating
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReviewCommentField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReviewContextCard
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

data class LeaveReviewReservationContext(
    val title: String,
    val reservationLabel: String,
    val imageUrl: String? = null,
    val imageContentDescription: String? = null,
)

data class LeaveReviewUiState(
    val reservationContext: LeaveReviewReservationContext,
    val selectedRating: Int = 0,
    val comment: String = "",
    val mode: LeaveReviewUiMode = LeaveReviewUiMode.Form,
) {
  val canSubmit: Boolean
    get() =
        selectedRating in 1..MaxReviewRating &&
            mode == LeaveReviewUiMode.Form &&
            (!requiresComment || hasValidComment)
}

internal const val MinReviewRating = 1
internal const val MaxReviewRating = 5

sealed interface LeaveReviewUiMode {
  data object Form : LeaveReviewUiMode

  data object Success : LeaveReviewUiMode
}

data class LeaveReviewScreenActions(
    val onRatingSelected: (Int) -> Unit,
    val onCommentChanged: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onReturnToReservations: () -> Unit,
    val onExploreCourts: () -> Unit,
)

@Composable
fun LeaveReviewScreen(
    state: LeaveReviewUiState,
    contentPadding: PaddingValues,
    actions: LeaveReviewScreenActions,
    modifier: Modifier = Modifier,
) {
  when (state.mode) {
    LeaveReviewUiMode.Form ->
        LeaveReviewFormContent(
            state = state,
            contentPadding = contentPadding,
            actions = actions,
            modifier = modifier,
        )
    LeaveReviewUiMode.Success ->
        LeaveReviewSuccessContent(
            contentPadding = contentPadding,
            onReturnToReservations = actions.onReturnToReservations,
            onExploreCourts = actions.onExploreCourts,
            modifier = modifier,
        )
  }
}

@Composable
fun ReservationsReviewLauncherScreen(
    reservationContext: LeaveReviewReservationContext,
    contentPadding: PaddingValues,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Tu última mejenga ya está lista para reseña",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text =
              "Mientras llega el historial completo de reservas, dejamos este acceso controlado para validar el flujo visual de calificación.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    MejenguerosReviewContextCard(
        title = reservationContext.title,
        supportingText = reservationContext.reservationLabel,
        imageUrl = reservationContext.imageUrl,
        imageContentDescription = reservationContext.imageContentDescription,
    )

    MejenguerosFullWidthPrimaryButton(text = "DEJAR RESEÑA", onClick = onStartReview)
  }
}

@Composable
private fun LeaveReviewFormContent(
    state: LeaveReviewUiState,
    contentPadding: PaddingValues,
    actions: LeaveReviewScreenActions,
    modifier: Modifier = Modifier,
) {
  val requiresComment = state.requiresComment
  val missingRequiredComment = requiresComment && !state.hasValidComment

  Surface(
      modifier = modifier.fillMaxSize().clearFocusOnTap().testTag("leave_review_form_root"),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding()) {
      Column(
          modifier =
              Modifier.weight(1f)
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 20.dp, vertical = 24.dp),
          verticalArrangement = Arrangement.spacedBy(28.dp),
      ) {
        MejenguerosReviewContextCard(
            title = state.reservationContext.title,
            supportingText = state.reservationContext.reservationLabel,
            imageUrl = state.reservationContext.imageUrl,
            imageContentDescription = state.reservationContext.imageContentDescription,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
              text = "¿Cómo estuvo tu mejenga?",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
          )
          MejenguerosRating(
              value = state.selectedRating,
              onValueChange = actions.onRatingSelected,
              modifier = Modifier.testTag("leave_review_rating"),
              showValueLabel = false,
              buttonSize = 56.dp,
              iconSize = 44.dp,
              itemSpacing = 8.dp,
          )
          Text(
              text = ratingHelperText(state.selectedRating),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color =
                  if (state.selectedRating == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  },
              textAlign = TextAlign.Center,
          )
        }

        MejenguerosReviewCommentField(
            value = state.comment,
            onValueChange = actions.onCommentChanged,
            label = "COMENTARIO",
            optionalLabel = if (requiresComment) "· obligatoria con 1 estrella" else "· opcional",
            isError = missingRequiredComment,
            supportingText = oneStarCommentSupportingText(requiresComment),
            placeholderLabel = "Contá tu experiencia: la cancha, la superficie, el ambiente...",
        )
      }

      MejenguerosBottomActionBar {
        MejenguerosFullWidthPrimaryButton(
            text = "ENVIAR RESEÑA",
            onClick = actions.onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.testTag("leave_review_submit_button"),
        )
      }
    }
  }
}

@Composable
private fun LeaveReviewSuccessContent(
    contentPadding: PaddingValues,
    onReturnToReservations: () -> Unit,
    onExploreCourts: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Surface(
            modifier = Modifier.testTag("leave_review_success_indicator"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
          Box(
              modifier = Modifier.padding(26.dp),
              contentAlignment = Alignment.Center,
          ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.testTag("leave_review_success_icon").size(56.dp),
            )
          }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
              text = "¡GRACIAS POR TU RESEÑA!",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
          )
          Text(
              text =
                  "Tu calificación quedó lista en este flujo. Muy pronto vas a poder publicarla para que otros la vean.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
      }
    }

    MejenguerosBottomActionBar {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MejenguerosFullWidthPrimaryButton(
            text = "VOLVER A MIS RESERVAS",
            onClick = onReturnToReservations,
        )
        MejenguerosOutlinedButton(
            text = "EXPLORAR CANCHAS",
            onClick = onExploreCourts,
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

private fun ratingHelperText(value: Int): String =
    when (value) {
      MinReviewRating -> "$MinReviewRating de $MaxReviewRating · Muy mala"
      2 -> "2 de 5 · Regular"
      3 -> "3 de 5 · Buena"
      4 -> "4 de 5 · Muy buena"
      MaxReviewRating -> "$MaxReviewRating de $MaxReviewRating · Excelente"
      else -> "Seleccioná de $MinReviewRating a $MaxReviewRating estrellas"
    }

private val LeaveReviewUiState.requiresComment: Boolean
  get() = selectedRating == MinReviewRating && mode == LeaveReviewUiMode.Form

private val LeaveReviewUiState.hasValidComment: Boolean
  get() = comment.isNotBlank()

private fun oneStarCommentSupportingText(requiresComment: Boolean): String? =
    if (requiresComment) {
      "Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia."
    } else {
      null
    }
