package io.github.themonstersp4.mejengueros.screens.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.themonstersp4.mejengueros.domain.model.UserProfile
import io.github.themonstersp4.mejengueros.presentation.profile.PlayerProfileFeedback
import io.github.themonstersp4.mejengueros.presentation.profile.PlayerProfileUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerProfileScreenBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysNameEmailAccessibleInitialsAndClickableFavoritesEntry() {
    setProfileContent(displayName = "María González", email = "maria@example.com")

    composeRule.onNodeWithContentDescription("Avatar de María González").assertExists()
    composeRule.onNodeWithText("MG").assertExists()
    composeRule.onNodeWithText("María González").assertExists()
    composeRule.onNodeWithText("maria@example.com").assertExists()
    composeRule.onNodeWithText("Mis canchas favoritas").assertExists()
    composeRule.onNodeWithTag("player_profile_favorite_courts").assertHasClickAction()
    composeRule.onNodeWithText("Consultá y administrá las canchas que guardaste.").assertExists()
  }

  @Test
  fun displaysProviderFromUserProfile() {
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileScreen(
            state =
                profileState("Fallback", "fallback@example.com", false)
                    .copy(
                        profile =
                            UserProfile(
                                id = "user-id",
                                roles = emptyList(),
                                name = "Profile Name",
                                email = "profile@example.com",
                                provider = "Google",
                            )
                    ),
            contentPadding = PaddingValues(),
        )
      }
    }

    composeRule.onNodeWithText("Profile Name").assertExists()
    composeRule.onNodeWithText("profile@example.com").assertExists()
    composeRule.onNodeWithText("Cuenta conectada con Google").assertExists()
  }

  @Test
  fun fallsBackToEmailWhenDisplayNameIsBlank() {
    setProfileContent(displayName = "   ", email = "player@example.com")

    composeRule.onNodeWithContentDescription("Avatar de player@example.com").assertExists()
    composeRule.onNodeWithText("PE").assertExists()
    composeRule.onNodeWithText("player@example.com").assertExists()
  }

  @Test
  fun supportedPickerMakesWholeAvatarActionableAndShowsEditBadge() {
    var changeRequests = 0
    setProfileContent(
        displayName = "María González",
        email = "maria@example.com",
        isImagePickerAvailable = true,
        onChangeProfileImage = { changeRequests += 1 },
    )

    composeRule
        .onNodeWithContentDescription("Cambiar foto de perfil")
        .assertHasClickAction()
        .performClick()
    composeRule.onNodeWithTag("profile_edit_badge", useUnmergedTree = true).assertIsDisplayed()
    composeRule.runOnIdle { kotlin.test.assertEquals(1, changeRequests) }
  }

  @Test
  fun unsupportedPickerExposesNoImageActionBadgeOrUnsupportedMessage() {
    setProfileContent(
        displayName = "María González",
        email = "maria@example.com",
        isImagePickerAvailable = false,
    )

    composeRule.onNodeWithContentDescription("Avatar de María González").assertExists()
    composeRule.onNodeWithContentDescription("Cambiar foto de perfil").assertDoesNotExist()
    composeRule.onNodeWithTag("profile_edit_badge").assertDoesNotExist()
    composeRule
        .onNodeWithText("no compatible", substring = true, ignoreCase = true)
        .assertDoesNotExist()
  }

  @Test
  fun uploadingKeepsImageContainerAndShowsBlockedProgressState() {
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileScreen(
            state =
                profileState("María González", "maria@example.com", true)
                    .copy(
                        profile =
                            UserProfile(
                                id = "user-id",
                                roles = emptyList(),
                                name = "María González",
                                email = "maria@example.com",
                                pictureUrl = "https://example.test/avatar.jpg",
                            ),
                        isUploadingImage = true,
                    ),
            contentPadding = PaddingValues(),
        )
      }
    }

    composeRule.onNodeWithTag("player_profile_image", useUnmergedTree = true).assertIsDisplayed()
    composeRule
        .onNodeWithTag("player_profile_uploading", useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Cambiar foto de perfil").assertIsNotEnabled()
  }

  @Test
  fun feedbackIsRenderedForAssistiveTechnology() {
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileScreen(
            state =
                profileState("María González", "maria@example.com", true)
                    .copy(feedback = PlayerProfileFeedback.ImageReadFailed),
            contentPadding = PaddingValues(),
        )
      }
    }

    composeRule
        .onNodeWithText("No pudimos leer la imagen seleccionada. Intentá con otra imagen.")
        .assertIsDisplayed()
  }

  private fun setProfileContent(
      displayName: String?,
      email: String,
      isImagePickerAvailable: Boolean = false,
      onChangeProfileImage: () -> Unit = {},
  ) {
    composeRule.setContent {
      MejenguerosTheme {
        PlayerProfileScreen(
            state = profileState(displayName, email, isImagePickerAvailable),
            contentPadding = PaddingValues(),
            onChangeProfileImage = onChangeProfileImage,
        )
      }
    }
  }

  private fun profileState(
      displayName: String?,
      email: String,
      isImagePickerAvailable: Boolean,
  ) =
      PlayerProfileUiState(
          userId = "user-id",
          fallbackDisplayName = displayName,
          fallbackEmail = email,
          isImagePickerAvailable = isImagePickerAvailable,
      )
}
