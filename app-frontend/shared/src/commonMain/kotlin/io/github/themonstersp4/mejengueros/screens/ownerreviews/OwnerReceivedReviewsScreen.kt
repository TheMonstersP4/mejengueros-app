package io.github.themonstersp4.mejengueros.screens.ownerreviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.OwnerReceivedCourtFilter
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReview
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewsSummary
import io.github.themonstersp4.mejengueros.domain.time.formatRelativeDateLabel
import io.github.themonstersp4.mejengueros.presentation.ownerreviews.OwnerReceivedReviewsUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOptionChip
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReviewSummary
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

data class OwnerReceivedReviewsScreenActions(
    val onCourtSelected: (String?) -> Unit,
    val onLoadMore: () -> Unit,
    val onRetryLoadMore: () -> Unit,
    val onRetry: () -> Unit,
    val onAcknowledgeError: () -> Unit,
)

private const val ScrollToTopThreshold = 6
internal const val OwnerReceivedReviewsScrollToTopTag = "owner_reviews_scroll_to_top_fab"
private const val MaxRatingStars = 5

@Composable
fun OwnerReceivedReviewsScreen(
    state: OwnerReceivedReviewsUiState,
    contentPadding: PaddingValues,
    actions: OwnerReceivedReviewsScreenActions,
    modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val showScrollToTop by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex >= ScrollToTopThreshold ||
          listState.firstVisibleItemScrollOffset > 240
    }
  }

  LaunchedEffect(state.selectedCourtId) { listState.scrollToItem(0) }

  LaunchedEffect(listState, state.canLoadMore) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
        .distinctUntilChanged()
        .filter { lastVisible ->
          val total = listState.layoutInfo.totalItemsCount
          total > 0 && lastVisible >= total - LoadMoreLookahead
        }
        .collect { actions.onLoadMore() }
  }

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .padding(contentPadding)
              .testTag("owner_reviews_root"),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      OwnerReceivedReviewsFilters(
          availableCourts = state.availableCourts,
          selectedCourtId = state.selectedCourtId,
          onCourtSelected = actions.onCourtSelected,
      )
      OwnerReceivedReviewsSummary(
          summary = state.summary,
          modifier = Modifier.testTag("owner_reviews_summary"),
      )

      when {
        state.isLoading && state.items.isEmpty() ->
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("owner_reviews_loading"),
                contentAlignment = Alignment.TopCenter,
            ) {
              MejenguerosInlineLoadingState(text = "Cargando reseñas…")
            }
        state.loadErrorMessage != null && state.items.isEmpty() ->
            OwnerReceivedReviewsErrorState(
                message = state.loadErrorMessage,
                onRetry = actions.onRetry,
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("owner_reviews_error"),
            )
        state.isEmpty ->
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .testTag("owner_reviews_empty"),
                contentAlignment = Alignment.TopCenter,
            ) {
              OwnerReceivedReviewsEmptyState()
            }
        else ->
            OwnerReceivedReviewsList(
                state = state,
                listState = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                onLoadMore = actions.onLoadMore,
                onRetryLoadMore = actions.onRetryLoadMore,
                onAcknowledgeError = actions.onAcknowledgeError,
            )
      }
    }

    if (showScrollToTop && state.items.isNotEmpty()) {
      FloatingActionButton(
          onClick = { scope.launch { listState.animateScrollToItem(0) } },
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(end = 20.dp, bottom = 24.dp)
                  .testTag(OwnerReceivedReviewsScrollToTopTag),
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Volver arriba",
        )
      }
    }
  }
}

@Composable
private fun OwnerReceivedReviewsFilters(
    availableCourts: List<OwnerReceivedCourtFilter>,
    selectedCourtId: String?,
    onCourtSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp)
              .testTag("owner_reviews_filters"),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = "Reseñas recibidas",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      MejenguerosOptionChip(
          text = "Todas",
          selected = selectedCourtId == null,
          onClick = { onCourtSelected(null) },
          modifier = Modifier.testTag("owner_reviews_filter_all"),
      )
      availableCourts.forEachIndexed { index, court ->
        MejenguerosOptionChip(
            text = court.name,
            selected = selectedCourtId == court.courtId,
            onClick = { onCourtSelected(court.courtId) },
            modifier = Modifier.testTag("owner_reviews_filter_chip_$index"),
        )
      }
    }
  }
}

@Composable
private fun OwnerReceivedReviewsSummary(
    summary: ReceivedReviewsSummary,
    modifier: Modifier = Modifier,
) {
  val reviewCountText =
      when (summary.totalReviews) {
        0 -> "0 reseñas"
        1 -> "1 reseña"
        else -> "${summary.totalReviews} reseñas"
      }
  val averageText = formatAverageRating(summary.averageRating)
  Box(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
  ) {
    MejenguerosReviewSummary(
        reviewCountText = reviewCountText,
        averageText = averageText,
        modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun OwnerReceivedReviewsList(
    state: OwnerReceivedReviewsUiState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onAcknowledgeError: () -> Unit,
) {
  LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().testTag("owner_reviews_list"),
      contentPadding = contentPadding,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    itemsIndexed(state.items, key = { _, review -> review.reviewId }) { index, review ->
      OwnerReceivedReviewCard(review = review, index = index)
    }
    if (state.isLoadingMore) {
      item(key = "loading_more") {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("owner_reviews_loading_more"),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        }
      }
    }
    state.loadMoreErrorMessage?.let { message ->
      item(key = "load_more_error") {
        OwnerReceivedReviewsLoadMoreError(
            message = message,
            onRetry = onRetryLoadMore,
            onAcknowledge = onAcknowledgeError,
        )
      }
    }
  }
}

@Composable
private fun OwnerReceivedReviewsLoadMoreError(
    message: String,
    onRetry: () -> Unit,
    onAcknowledge: () -> Unit,
) {
  Column(
      modifier =
          Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("owner_reviews_load_more_error"),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    MejenguerosErrorText(text = message)
    MejenguerosFullWidthOutlinedButton(text = "Reintentar", onClick = onRetry)
    MejenguerosFullWidthOutlinedButton(
        text = "Cerrar",
        onClick = onAcknowledge,
        modifier = Modifier.testTag("owner_reviews_load_more_dismiss"),
    )
  }
}

@Composable
private fun OwnerReceivedReviewCard(
    review: ReceivedReview,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
  Surface(
      modifier = modifier.fillMaxWidth().testTag("owner_reviews_card_$index"),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        ReceivedReviewAvatar(initials = review.reviewer.initials)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
              text = review.reviewer.displayName,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Text(
              text = formatRelativeDateLabel(review.createdAt),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
        OwnerReceivedReviewStars(value = review.rating)
      }
      review.comment
          ?.takeIf { it.isNotBlank() }
          ?.let { comment ->
            Text(
                text = comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
          }
      CourtTag(name = review.court.name)
    }
  }
}

@Composable
private fun ReceivedReviewAvatar(initials: String) {
  Surface(
      modifier = Modifier.size(38.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
          text = initials,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun OwnerReceivedReviewStars(value: Int) {
  val safeValue = value.coerceIn(0, MaxRatingStars)
  Row(
      modifier = Modifier.testTag("owner_reviews_card_stars_$safeValue"),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(MaxRatingStars) { index ->
      val filled = index < safeValue
      Icon(
          imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint =
              if (filled) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.outlineVariant
              },
      )
    }
  }
}

@Composable
private fun CourtTag(name: String) {
  Surface(
      modifier = Modifier.testTag("owner_reviews_card_court_tag"),
      shape = RoundedCornerShape(50),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun OwnerReceivedReviewsEmptyState() {
  Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
        text = "Todavía no tenés reseñas.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = "Cuando un mejenguero deje una reseña sobre tus canchas, la vas a ver acá.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    MejenguerosFullWidthPrimaryButton(
        text = "Entendido",
        onClick = {},
        enabled = false,
    )
  }
}

@Composable
private fun OwnerReceivedReviewsErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    MejenguerosErrorText(text = message)
    MejenguerosFullWidthOutlinedButton(text = "Reintentar", onClick = onRetry)
  }
}

private fun formatAverageRating(average: Double?): String {
  if (average == null || average <= 0.0) return "0.0 promedio"
  val rounded = (average * 10.0).roundToInt() / 10.0
  return "$rounded promedio"
}

private const val LoadMoreLookahead = 4
