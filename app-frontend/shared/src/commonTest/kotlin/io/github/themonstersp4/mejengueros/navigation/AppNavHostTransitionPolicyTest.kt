package io.github.themonstersp4.mejengueros.navigation

import androidx.navigationevent.NavigationEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavHostTransitionPolicyTest {

  @Test
  fun navigationTransitionOffsetUsesSubtleFractionAndNeverReturnsZero() {
    assertEquals(40, navigationTransitionOffset(320))
    assertEquals(1, navigationTransitionOffset(4))
  }

  @Test
  fun predictivePopDirectionDefaultsToLeftAndMirrorsRightEdge() {
    assertEquals(-1, predictivePopDirectionMultiplier(NavigationEvent.EDGE_NONE))
    assertEquals(-1, predictivePopDirectionMultiplier(NavigationEvent.EDGE_LEFT))
    assertEquals(1, predictivePopDirectionMultiplier(NavigationEvent.EDGE_RIGHT))
  }
}
