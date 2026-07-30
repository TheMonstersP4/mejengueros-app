package io.github.themonstersp4.mejengueros.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.profile.PlayerProfileFeedback
import io.github.themonstersp4.mejengueros.presentation.profile.PlayerProfileUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosMessageDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosThumbnail

@Composable
fun PlayerProfileScreen(
    state: PlayerProfileUiState,
    contentPadding: PaddingValues,
    onChangeProfileImage: () -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    onFavoriteCourtsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
  val identity = profileIdentity(displayName = state.displayName, email = state.email)

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
    ProfileAvatar(
        identity = identity,
        pictureUrl = state.pictureUrl,
        canChangeImage = state.isImagePickerAvailable,
        isUploading = state.isUploadingImage,
        onChangeImage = onChangeProfileImage,
    )

    if (state.feedback == PlayerProfileFeedback.ImageUpdated) {
      Text(
          text = PlayerProfileFeedback.ImageUpdated.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier =
              Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                  .testTag("player_profile_feedback"),
      )
    }

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
      state.provider?.trim()?.takeIf(String::isNotEmpty)?.let { provider ->
        Text(
            text = "Cuenta conectada con $provider",
            style = MaterialTheme.typography.labelMedium,
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

  val errorFeedback = state.feedback?.takeUnless { it == PlayerProfileFeedback.ImageUpdated }
  MejenguerosMessageDialog(
      visible = errorFeedback != null,
      title = errorFeedback?.dialogTitle.orEmpty(),
      message = errorFeedback?.message.orEmpty(),
      onDismissRequest = onDismissFeedback,
  )
}

@Composable
private fun ProfileAvatar(
    identity: PlayerProfileIdentity,
    pictureUrl: String?,
    canChangeImage: Boolean,
    isUploading: Boolean,
    onChangeImage: () -> Unit,
) {
  val accessibilityLabel =
      if (canChangeImage) "Cambiar foto de perfil" else "Avatar de ${identity.primaryLabel}"
  Box(
      modifier = Modifier.size(96.dp).testTag("player_profile_avatar_container"),
      contentAlignment = Alignment.Center,
  ) {
    val avatarModifier =
        Modifier.size(88.dp).clip(CircleShape).semantics { contentDescription = accessibilityLabel }
    val interactiveAvatarModifier =
        if (canChangeImage) {
          avatarModifier.clickable(
              enabled = !isUploading,
              role = Role.Button,
              onClickLabel = "Cambiar foto de perfil",
              onClick = onChangeImage,
          )
        } else {
          avatarModifier
        }

    Box(modifier = interactiveAvatarModifier.testTag("player_profile_avatar")) {
      MejenguerosThumbnail(
          imageUrl = pictureUrl,
          contentDescription = null,
          size = null,
          shape = CircleShape,
          modifier = Modifier.matchParentSize().testTag("player_profile_image"),
      ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Box(contentAlignment = Alignment.Center) {
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
    }

    if (canChangeImage) {
      Surface(
          modifier = Modifier.align(Alignment.BottomEnd).size(30.dp).testTag("profile_edit_badge"),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shadowElevation = 2.dp,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    if (isUploading) {
      Surface(
          modifier = Modifier.size(88.dp).alpha(0.72f),
          shape = CircleShape,
          color = Color.Black,
      ) {}
      CircularProgressIndicator(
          modifier = Modifier.size(30.dp).testTag("player_profile_uploading"),
          color = Color.White,
          strokeWidth = 3.dp,
      )
    }
  }
}

private val PlayerProfileFeedback.message: String
  get() =
      when (this) {
        PlayerProfileFeedback.ImageUpdated -> "Tu foto de perfil se actualizó."
        PlayerProfileFeedback.ImageReadFailed ->
            "No pudimos leer la imagen seleccionada. Intentá con otra imagen."
        PlayerProfileFeedback.ImageUploadFailed ->
            "No pudimos actualizar tu foto de perfil. Intentá nuevamente."
      }

private val PlayerProfileFeedback.dialogTitle: String
  get() =
      when (this) {
        PlayerProfileFeedback.ImageReadFailed -> "No pudimos usar esa imagen"
        PlayerProfileFeedback.ImageUploadFailed -> "No pudimos cambiar la foto"
        PlayerProfileFeedback.ImageUpdated -> ""
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
