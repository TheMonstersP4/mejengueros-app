package io.github.themonstersp4.mejengueros.screens.pokedex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListMode
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListUiState

private val SearchHeaderReservedHeight = 132.dp

@Composable
fun PokedexScreen(
    state: PokemonListUiState,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onModeChange: (PokemonListMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  PokemonListContent(
      state = state,
      onPokemonClick = onPokemonClick,
      onFavoriteClick = onFavoriteClick,
      onModeChange = onModeChange,
      onLoadMore = onLoadMore,
      onRefresh = onRefresh,
      onSearchQueryChange = onSearchQueryChange,
      contentPadding = contentPadding,
      modifier = modifier,
  )
}

@Composable
private fun PokemonListContent(
    state: PokemonListUiState,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onModeChange: (PokemonListMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  PullToRefreshBox(
      isRefreshing = state.isRefreshing,
      onRefresh = onRefresh,
      modifier = modifier.fillMaxSize(),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (state.isFavoritesMode) {
        FavoritePokemonMasonryContent(
            state = state,
            onPokemonClick = onPokemonClick,
            onFavoriteClick = onFavoriteClick,
            onRefresh = onRefresh,
            contentPadding = contentPadding,
        )
      } else {
        PokemonAllListContent(
            state = state,
            onPokemonClick = onPokemonClick,
            onFavoriteClick = onFavoriteClick,
            onLoadMore = onLoadMore,
            onRefresh = onRefresh,
            contentPadding = contentPadding,
        )
      }

      SearchHeader(
          state = state,
          onModeChange = onModeChange,
          onQueryChange = onSearchQueryChange,
          contentPadding = contentPadding,
      )
    }
  }
}

@Composable
private fun PokemonAllListContent(
    state: PokemonListUiState,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LazyColumn(
      state = listState,
      modifier = modifier.fillMaxSize(),
      contentPadding =
          PaddingValues(
              start = 16.dp,
              top = contentPadding.calculateTopPadding() + SearchHeaderReservedHeight + 16.dp,
              end = 16.dp,
              bottom = contentPadding.calculateBottomPadding() + 16.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(
          text = "Pokédex",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }
    itemsIndexed(state.visibleItems, key = { _, pokemon -> pokemon.id }) { _, pokemon ->
      PokemonSummaryRow(
          pokemon = pokemon,
          onClick = { onPokemonClick(pokemon.id) },
          onFavoriteClick = { onFavoriteClick(pokemon.id) },
      )
    }
    if (state.isLoading) {
      item(key = "loading") { LoadingRow() }
    }
    if (state.isEmpty && state.errorMessage == null) {
      item(key = "empty") {
        EmptyMessage(
            message =
                if (state.isSearching) "No Pokémon match this search." else "No Pokémon available."
        )
      }
    }
    if (!state.endReached && !state.isLoadingMore && state.errorMessage == null) {
      item(key = "load-more-trigger-${state.items.size}-${state.searchQuery}") {
        LoadMoreTrigger(
            itemCount = state.items.size,
            searchQuery = state.searchQuery,
            onLoadMore = onLoadMore,
        )
      }
    }
    if (state.isLoadingMore) {
      item { LoadingRow() }
    }
    state.errorMessage?.let { message ->
      item { ErrorMessage(message = message, onRetry = onRefresh) }
    }
  }
}

@Composable
private fun FavoritePokemonMasonryContent(
    state: PokemonListUiState,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
  val columns = remember(state.visibleItems) { distributeFavorites(state.visibleItems) }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(
                  start = 16.dp,
                  top = contentPadding.calculateTopPadding() + SearchHeaderReservedHeight + 16.dp,
                  end = 16.dp,
                  bottom = contentPadding.calculateBottomPadding() + 16.dp,
              ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
        text = "My likes",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "${state.visibleItems.size} favorite Pokémon",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when {
      state.isLoading -> LoadingRow()
      state.isEmpty && state.errorMessage == null -> EmptyMessage("No favorite Pokémon yet.")
      state.errorMessage != null -> ErrorMessage(message = state.errorMessage, onRetry = onRefresh)
      else ->
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FavoriteColumn(
                items = columns.first,
                onPokemonClick = onPokemonClick,
                onFavoriteClick = onFavoriteClick,
                modifier = Modifier.weight(1f),
            )
            FavoriteColumn(
                items = columns.second,
                onPokemonClick = onPokemonClick,
                onFavoriteClick = onFavoriteClick,
                modifier = Modifier.weight(1f),
            )
          }
    }
  }
}

@Composable
private fun FavoriteColumn(
    items: List<PokemonSummary>,
    onPokemonClick: (Int) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items.forEach { pokemon ->
      FavoritePokemonCard(
          pokemon = pokemon,
          onClick = { onPokemonClick(pokemon.id) },
          onFavoriteClick = { onFavoriteClick(pokemon.id) },
      )
    }
  }
}

@Composable
private fun FavoritePokemonCard(
    pokemon: PokemonSummary,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AsyncImage(
          model = pokemon.imageUrl,
          contentDescription = pokemon.displayName,
          contentScale = ContentScale.Fit,
          modifier =
              Modifier.fillMaxWidth()
                  .height(if (pokemon.id % 3 == 0) 132.dp else 104.dp)
                  .clip(MaterialTheme.shapes.medium),
      )
      Text(
          text = "#${pokemon.id}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          text = pokemon.displayName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      IconButton(onClick = onFavoriteClick, modifier = Modifier.align(Alignment.End)) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Remove from favorites",
        )
      }
    }
  }
}

private fun distributeFavorites(
    items: List<PokemonSummary>
): Pair<List<PokemonSummary>, List<PokemonSummary>> {
  val left = mutableListOf<PokemonSummary>()
  val right = mutableListOf<PokemonSummary>()
  var leftScore = 0
  var rightScore = 0

  items.forEach { item ->
    val score = if (item.id % 3 == 0) 3 else 2
    if (leftScore <= rightScore) {
      left += item
      leftScore += score
    } else {
      right += item
      rightScore += score
    }
  }

  return left to right
}

@Composable
private fun LoadMoreTrigger(
    itemCount: Int,
    searchQuery: String,
    onLoadMore: () -> Unit,
) {
  LaunchedEffect(itemCount, searchQuery) { onLoadMore() }
}

@Composable
private fun SearchHeader(
    state: PokemonListUiState,
    onModeChange: (PokemonListMode) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
  Surface(
      modifier = modifier.fillMaxWidth().zIndex(1f),
      color = MaterialTheme.colorScheme.background,
      tonalElevation = 3.dp,
      shadowElevation = 2.dp,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { onModeChange(PokemonListMode.All) }) {
          if (state.mode == PokemonListMode.All) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
          }
          Text("All")
        }
        TextButton(onClick = { onModeChange(PokemonListMode.Favorites) }) {
          if (state.mode == PokemonListMode.Favorites) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
          }
          Text("My likes")
        }
      }
      if (state.mode == PokemonListMode.All) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search PokéAPI") },
        )
      }
    }
  }
}

@Composable
private fun LoadingRow(modifier: Modifier = Modifier) {
  Box(
      modifier = modifier.fillMaxWidth().padding(16.dp),
      contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun EmptyMessage(message: String, modifier: Modifier = Modifier) {
  Text(
      text = message,
      modifier = modifier.fillMaxWidth().padding(16.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
  )
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.fillMaxWidth(),
  ) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = onRetry) { Text("Retry") }
  }
}

@Composable
private fun PokemonSummaryRow(
    pokemon: PokemonSummary,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AsyncImage(
          model = pokemon.imageUrl,
          contentDescription = pokemon.displayName,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(72.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "#${pokemon.id}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = pokemon.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
      }
      IconButton(onClick = onFavoriteClick) {
        Icon(
            imageVector = if (pokemon.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
            contentDescription =
                if (pokemon.isFavorite) "Remove from favorites" else "Add to favorites",
        )
      }
    }
  }
}
