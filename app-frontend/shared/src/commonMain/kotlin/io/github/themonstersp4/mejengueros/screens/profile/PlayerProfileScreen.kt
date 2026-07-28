package io.github.themonstersp4.mejengueros.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PlayerProfileScreen(
    displayName: String?,
    email: String,
    contentPadding: PaddingValues,
    onFavoriteCourtsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
  val identity = profileIdentity(displayName = displayName, email = email)

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .testTag("player_profile_root"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    ProfileAvatar(identity = identity)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
          text = identity.primaryLabel,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
      )
      identity.email?.let { accountEmail ->
        Text(
            text = accountEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
      }
    }

    Card(
        onClick = onFavoriteCourtsClick,
        modifier = Modifier.fillMaxWidth().testTag("player_profile_favorite_courts"),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
      androidx.compose.foundation.layout.Row(
          modifier = Modifier.padding(20.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Favorite, contentDescription = null)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
              "Mis canchas favoritas",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              "Consultá y administrá las canchas que guardaste.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Abrir mis canchas favoritas",
        )
      }
    }
  }
}

@Composable
private fun ProfileAvatar(identity: PlayerProfileIdentity) {
  Surface(
      modifier =
          Modifier.size(88.dp)
              .semantics { contentDescription = "Avatar de ${identity.primaryLabel}" }
              .testTag("player_profile_avatar"),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  ) {
    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
      if (identity.initials == null) {
        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
      } else {
        Text(
            text = identity.initials,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

internal data class PlayerProfileIdentity(
    val primaryLabel: String,
    val email: String?,
    val initials: String?,
)

internal fun profileIdentity(displayName: String?, email: String): PlayerProfileIdentity {
  val normalizedName = displayName?.trim()?.takeIf(String::isNotEmpty)
  val normalizedEmail = email.trim().takeIf(String::isNotEmpty)
  val primaryLabel = normalizedName ?: normalizedEmail ?: "Tu cuenta"
  val initials = initialsFor(normalizedName ?: normalizedEmail)

  return PlayerProfileIdentity(
      primaryLabel = primaryLabel,
      email = normalizedEmail?.takeIf { it != primaryLabel },
      initials = initials,
  )
}

private fun initialsFor(value: String?): String? =
    value
        ?.split(Regex("[\\s@._-]+"))
        ?.filter(String::isNotEmpty)
        ?.take(2)
        ?.joinToString(separator = "") { it.first().uppercase() }
        ?.takeIf(String::isNotEmpty)
