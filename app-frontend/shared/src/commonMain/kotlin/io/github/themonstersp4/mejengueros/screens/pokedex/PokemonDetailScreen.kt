package io.github.themonstersp4.mejengueros.screens.pokedex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonDetailUiState

@Composable
fun PokemonDetailScreen(
    state: PokemonDetailUiState,
    onFavoriteClick: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  when {
    state.isLoading -> LoadingContent(contentPadding = contentPadding, modifier = modifier)
    state.pokemon != null ->
        PokemonDetailContent(
            pokemon = state.pokemon,
            onFavoriteClick = onFavoriteClick,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    else ->
        ErrorContent(
            message = state.errorMessage ?: "Unable to load Pokémon detail.",
            onRetry = onRetry,
            contentPadding = contentPadding,
            modifier = modifier,
        )
  }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Loading Pokémon detail…",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun PokemonDetailContent(
    pokemon: PokemonDetail,
    onFavoriteClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .verticalScroll(rememberScrollState())
              .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AsyncImage(
        model = pokemon.imageUrl,
        contentDescription = pokemon.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(180.dp),
    )
    Text(
        text = pokemon.displayName,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = "#${pokemon.id}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = onFavoriteClick) {
      Icon(
          imageVector = if (pokemon.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
      )
      Text("Favorite")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      pokemon.types.forEach { type -> AssistChip(onClick = {}, label = { Text(type) }) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      PokemonMetric(label = "Height", value = pokemon.displayHeight)
      PokemonMetric(label = "Weight", value = pokemon.displayWeight)
    }
  }
}

@Composable
private fun PokemonMetric(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "We could not load this Pokémon.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    TextButton(onClick = onRetry) { Text("Retry") }
  }
}
