package io.github.themonstersp4.mejengueros.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MejenguerosStaticLocationMapBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun rendersPlatformMapCenteredOnTheProvidedLocation() {
    var renderedLocation: SelectedLocation? = null

    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosStaticLocationMap(
            location = SelectedLocation(latitude = 9.935, longitude = -84.091),
            modifier = Modifier.testTag("static_location_map"),
            map = { location, mapModifier ->
              renderedLocation = location
              Box(modifier = mapModifier.testTag("fake_platform_map"))
            },
        )
      }
    }

    composeRule.onNodeWithTag("static_location_map").assertExists()
    composeRule.onNodeWithTag("fake_platform_map").assertExists()
    assert(renderedLocation == SelectedLocation(latitude = 9.935, longitude = -84.091))
  }
}
