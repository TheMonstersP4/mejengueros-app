package io.github.themonstersp4.mejengueros.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.favorites.FavoriteCourtsUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosCourtCard
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant

@Composable
fun FavoriteCourtsScreen(
    state: FavoriteCourtsUiState,
    contentPadding: PaddingValues,
    onOpenCourt: (CourtCatalogItem) -> Unit,
    onRemoveCourt: (String) -> Unit,
    onRetry: () -> Unit,
    onExploreCourts: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(contentPadding)) {
    when {
      state.isLoading ->
          MejenguerosInlineLoadingState(
              "Cargando canchas favoritas…",
              Modifier.align(Alignment.Center).testTag("favorite_courts_loading"),
          )
      state.errorMessage != null ->
          MejenguerosStateContent(
              "No pudimos cargar tus favoritas",
              state.errorMessage,
              Modifier.align(Alignment.Center).testTag("favorite_courts_error"),
              MejenguerosStateVariant.Error,
              actions = {
                MejenguerosOutlinedButton(
                    "Reintentar",
                    onRetry,
                    Modifier.testTag("favorite_courts_retry"),
                )
              },
          )
      state.courts.isEmpty() ->
          MejenguerosStateContent(
              "Todavía no tenés canchas favoritas",
              "Explorá canchas y guardá las que querés volver a encontrar.",
              Modifier.align(Alignment.Center).testTag("favorite_courts_empty"),
              MejenguerosStateVariant.Empty,
              actions = {
                MejenguerosFullWidthPrimaryButton(
                    "Explorar canchas",
                    onExploreCourts,
                    Modifier.testTag("favorite_courts_explore"),
                )
              },
          )
      else ->
          LazyColumn(
              modifier = Modifier.fillMaxSize().testTag("favorite_courts_content"),
              contentPadding = PaddingValues(20.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            item {
              state.removalErrorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("favorite_courts_removal_error"),
                )
              }
            }
            items(state.courts, key = { it.id }) { court ->
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MejenguerosCourtCard(
                    court.displayTitle,
                    listOf(court.complexName, court.provinceName, court.cantonName)
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    court.imageUrl,
                    imageContentDescription = court.displayTitle,
                    metadata = court.services.take(2),
                    onClick = { onOpenCourt(court) },
                    enabled = court.id !in state.removingCourtIds,
                    modifier = Modifier.testTag("favorite_court_card_${court.id}"),
                )
                IconButton(
                    onClick = { onRemoveCourt(court.id) },
                    enabled = court.id !in state.removingCourtIds,
                    modifier =
                        Modifier.align(Alignment.End)
                            .testTag("favorite_court_remove_${court.id}")
                            .semantics {
                              contentDescription = "Quitar ${court.displayTitle} de favoritas"
                            },
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = null)
                }
              }
            }
          }
    }
  }
}
