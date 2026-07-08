package io.github.themonstersp4.mejengueros.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.MyReservationCard
import io.github.themonstersp4.mejengueros.domain.model.MyReservations
import io.github.themonstersp4.mejengueros.domain.model.ReservationConfirmation
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayAvailability
import io.github.themonstersp4.mejengueros.domain.model.ReservationDayDiscovery
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationsViewModel
import io.github.themonstersp4.mejengueros.presentation.review.ReviewViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import io.github.themonstersp4.mejengueros.ui.components.ReviewEvidenceImagePickerController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationsNavigationIntegrationTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun reservationsListOpensFormAndBackReturnsToList() = runTest {
    val navigationState = testNavigationState()
    val reservationsViewModel = createReservationsViewModel(this)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule
        .onNodeWithTag(FinalizedReservationActionTag)
        .assertExists()
        .performScrollTo()
        .performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("leave_review_form_root").assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertExists()
    composeRule.onNodeWithContentDescription("Seleccionar 3 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Comentario temporal")
    composeRule.onNodeWithTag("leave_review_submit_button").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Comentario temporal").assertDoesNotExist()
  }

  @Test
  fun unsupportedReservationPrimaryActionKeyDoesNotRenderDeadCta() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.success(
                    sampleMyReservations(
                        primaryActionKey = "unsupported_action",
                        primaryActionLabel = "Dejar reseña",
                    )
                )
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithTag(FinalizedReservationActionTag).assertDoesNotExist()
    composeRule.onNodeWithText("Dejar reseña").assertDoesNotExist()
  }

  @Test
  fun successBackReturnsToRefreshedReservationsListAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.success(sampleMyReservations()),
                Result.success(sampleMyReservations(reviewActionAvailable = false)),
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Otro comentario temporal")
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithText("Dejar reseña").assertDoesNotExist()
    composeRule.onNodeWithText("Reseña enviada").assertExists()
    composeRule.onNodeWithText("Otro comentario temporal").assertDoesNotExist()
  }

  @Test
  fun successBackShowsReservationsRefreshErrorWhenPostReviewReloadFails() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.success(sampleMyReservations()),
                Result.failure(AppApiException(500, "refresh failed after submit")),
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("No pudimos cargar tus reservas").assertExists()
    composeRule.onNodeWithText("REINTENTAR").assertExists()
  }

  @Test
  fun oneStarBlankCommentStaysBlockedUntilCommentIsProvided() = runTest {
    val navigationState = testNavigationState()
    val reservationsViewModel = createReservationsViewModel(this)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 1 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule
        .onNodeWithText("Si dejás 1 estrella, contanos qué pasó para revisar mejor tu experiencia.")
        .assertExists()

    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("El piso estaba resbaloso y no había buena iluminación")
    composeRule.onNodeWithTag("leave_review_pick_evidence_button").performScrollTo().performClick()

    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()
  }

  @Test
  fun validSubmitReachesSuccessAndExitPathsResetReservationsFlow() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.success(sampleMyReservations()),
                Result.success(sampleMyReservations(reviewActionAvailable = false)),
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("La cancha estaba impecable")
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()
    composeRule.onNodeWithText("VOLVER A MIS RESERVAS").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithText("Dejar reseña").assertDoesNotExist()
    composeRule.onNodeWithText("La cancha estaba impecable").assertDoesNotExist()

    composeRule.onNodeWithText("Reseña enviada").assertExists()
    composeRule.onNodeWithText("EXPLORAR CANCHAS").assertDoesNotExist()

    navigationState.selectReservations()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Reseña enviada").assertExists()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()

    composeRule.runOnIdle { navigationState.selectSearch() }
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Buscar canchas").assertExists()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }
  }

  @Test
  fun systemBackFromFormReturnsToReservationsListAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val reservationsViewModel = createReservationsViewModel(this)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Borrador que no debe sobrevivir")

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()
    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Borrador que no debe sobrevivir").assertDoesNotExist()
  }

  @Test
  fun systemBackFromSuccessReturnsToRefreshedReservationsListAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.success(sampleMyReservations()),
                Result.success(sampleMyReservations(reviewActionAvailable = false)),
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithTag(FinalizedReservationActionTag).performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    composeRule.onNodeWithText("Dejar reseña").assertDoesNotExist()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()
  }

  @Test
  fun reservationsListShowsLoadingWhileMyReservationsAreStillResolving() = runTest {
    val navigationState = testNavigationState()
    val myReservationsResult = CompletableDeferred<MyReservations>()
    val reservationsViewModel =
        createReservationsViewModel(
            this,
            DelayedReservationRepository(myReservationsResult),
        )
    val reviewViewModel = createReviewViewModel(this)

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithText("Cargando tus reservas...").assertExists()

    myReservationsResult.complete(sampleMyReservations())
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
  }

  @Test
  fun reservationsListShowsEmptyStateWhenThereAreNoReservations() = runTest {
    val navigationState = testNavigationState()
    val reservationsViewModel =
        createReservationsViewModel(
            this,
            FixedReservationRepository(MyReservations(emptyList(), emptyList())),
        )
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithText("Todavía no tenés reservas").assertExists()
    composeRule.onNodeWithText("Dejar reseña").assertDoesNotExist()
  }

  @Test
  fun reservationsRetryRecoversFromLoadFailure() = runTest {
    val navigationState = testNavigationState()
    val reservationRepository =
        SequencedReservationRepository(
            listOf(
                Result.failure(AppApiException(500, "boom")),
                Result.success(sampleMyReservations()),
            )
        )
    val reservationsViewModel = createReservationsViewModel(this, reservationRepository)
    val reviewViewModel = createReviewViewModel(this)
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(
            navigationState = navigationState,
            reservationsViewModel = reservationsViewModel,
            reviewViewModel = reviewViewModel,
        )
      }
    }

    composeRule.onNodeWithText("No pudimos cargar tus reservas").assertExists()
    composeRule.onNodeWithText("REINTENTAR").performClick()

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(FinalizedReservationCardTag).assertExists()
    assertEquals(2, reservationRepository.getMyReservationsCalls)
  }

  @Composable
  private fun ReservationsNavigationTestHost(
      navigationState: AuthenticatedNavigationState,
      reservationsViewModel: MyReservationsViewModel,
      reviewViewModel: ReviewViewModel,
  ) {
    val shellActions =
        AuthenticatedShellActions(
            selectSearch = navigationState::selectSearch,
            selectReservations = navigationState::selectReservations,
            selectNotifications = navigationState::selectNotifications,
            selectMyComplex = navigationState::selectMyComplex,
            returnToSearchRoot = navigationState::returnToSearchRoot,
            returnToMyComplexRoot = navigationState::returnToMyComplexRoot,
            openCatalogCourtDetail = navigationState::openCatalogCourtDetail,
            openCatalogReservation = navigationState::openCatalogReservation,
            openComplexDetail = navigationState::openComplexDetail,
            openAddCourt = navigationState::openAddCourt,
            openCreateComplex = navigationState::openCreateComplex,
            openCourtAvailability = navigationState::openCourtAvailability,
            closeAddCourtAfterSuccess = navigationState::closeAddCourtAfterSuccess,
            closeCurrentDetail = navigationState::closeCurrentDetail,
            signOut = {},
            refreshOwnerRole = {},
            isOwner = false,
            viewingAsPlayer = true,
        )

    when (navigationState.selectedRoute) {
      AuthenticatedTopLevelRoute.Reservations ->
          ReservationsEntryContent(
              shellActions = shellActions,
              reservationsViewModel = reservationsViewModel,
              reviewViewModel = reviewViewModel,
              reviewEvidenceImagePickerController =
                  ReviewEvidenceImagePickerController(
                      isAvailable = true,
                      launch = {
                        reviewViewModel.updateSelectedEvidenceImage(
                            LocalReviewEvidenceImage(
                                fileName = "evidence.png",
                                contentType = "image/png",
                                bytes = byteArrayOf(1, 2, 3),
                                previewUrl = "content://evidence.png",
                            )
                        )
                      },
                  ),
          )
      AuthenticatedTopLevelRoute.Search -> Text("Buscar canchas")
      else -> Text("Ruta inesperada")
    }
  }

  private fun createReservationsViewModel(
      coroutineScope: TestScope,
      reservationRepository: IReservationRepository =
          FixedReservationRepository(sampleMyReservations()),
  ): MyReservationsViewModel =
      MyReservationsViewModel(
          reservationRepository = reservationRepository,
          coroutineScope = coroutineScope,
      )

  private fun createReviewViewModel(
      coroutineScope: TestScope,
      reviewRepository: IReviewRepository = RecordingReviewRepository(),
      reviewEvidenceUploadRepository: IReviewEvidenceUploadRepository =
          object : IReviewEvidenceUploadRepository {
            override suspend fun uploadReviewEvidence(
                image: LocalReviewEvidenceImage
            ): ConfirmedReviewEvidenceImageUpload =
                ConfirmedReviewEvidenceImageUpload(
                    id = "evidence-image-id",
                    objectKey = "uploads/review-evidence-image/player-sub/2026/07/evidence.png",
                    readUrl = "https://read.example.test/evidence.png",
                )
          },
  ): ReviewViewModel =
      ReviewViewModel(
          reviewRepository = reviewRepository,
          reviewEvidenceUploadRepository = reviewEvidenceUploadRepository,
          coroutineScope = coroutineScope,
      )

  private class RecordingReviewRepository(
      private val latestReservation: ReviewableReservation? = null,
  ) : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        latestReservation

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        CreatedReview(
            id = "review-id",
            reservationId = request.reservationId,
            rating = request.rating,
            comment = request.comment,
            evidenceImageUploadId = request.evidenceImageUploadId,
            createdAt = "2026-07-03T02:00:00.000Z",
        )

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in reservations navigation tests.")
  }

  private class FixedReservationRepository(
      private val myReservations: MyReservations,
  ) : IReservationRepository {
    override suspend fun getReservableDays(
        courtId: String,
        fromUtcDate: String,
        days: Int,
    ): ReservationDayDiscovery = error("Unused in test")

    override suspend fun getReservableSlots(
        courtId: String,
        dateUtc: String,
    ): ReservationDayAvailability = error("Unused in test")

    override suspend fun createReservation(
        courtId: String,
        startsAtUtc: String,
    ): ReservationConfirmation = error("Unused in test")

    override suspend fun getMyReservations(): MyReservations = myReservations
  }

  private class SequencedReservationRepository(
      private val results: List<Result<MyReservations>>,
  ) : IReservationRepository {
    var getMyReservationsCalls: Int = 0

    override suspend fun getReservableDays(
        courtId: String,
        fromUtcDate: String,
        days: Int,
    ): ReservationDayDiscovery = error("Unused in test")

    override suspend fun getReservableSlots(
        courtId: String,
        dateUtc: String,
    ): ReservationDayAvailability = error("Unused in test")

    override suspend fun createReservation(
        courtId: String,
        startsAtUtc: String,
    ): ReservationConfirmation = error("Unused in test")

    override suspend fun getMyReservations(): MyReservations {
      val result =
          results.getOrElse(getMyReservationsCalls) {
            results.lastOrNull() ?: Result.success(sampleMyReservations())
          }
      getMyReservationsCalls += 1
      return result.getOrThrow()
    }
  }

  private class DelayedReservationRepository(
      private val myReservationsResult: CompletableDeferred<MyReservations>,
  ) : IReservationRepository {
    override suspend fun getReservableDays(
        courtId: String,
        fromUtcDate: String,
        days: Int,
    ): ReservationDayDiscovery = error("Unused in test")

    override suspend fun getReservableSlots(
        courtId: String,
        dateUtc: String,
    ): ReservationDayAvailability = error("Unused in test")

    override suspend fun createReservation(
        courtId: String,
        startsAtUtc: String,
    ): ReservationConfirmation = error("Unused in test")

    override suspend fun getMyReservations(): MyReservations = myReservationsResult.await()
  }
}

private const val FinalizedReservationId = "reservation-id"
private const val FinalizedReservationCardTag = "my_reservation_card_$FinalizedReservationId"
private const val FinalizedReservationActionTag = "my_reservation_action_$FinalizedReservationId"

private fun sampleMyReservations(
    reviewActionAvailable: Boolean = true,
    primaryActionKey: String? = if (reviewActionAvailable) "leave_review" else null,
    primaryActionLabel: String? = if (reviewActionAvailable) "Dejar reseña" else null,
): MyReservations =
    MyReservations(
        upcoming =
            listOf(
                reservationCard(
                    id = "upcoming-id",
                    section = "UPCOMING",
                    reviewStatus = "NOT_APPLICABLE",
                    canReview = false,
                    hasReview = false,
                )
            ),
        finalized =
            listOf(
                reservationCard(
                    id = FinalizedReservationId,
                    section = "FINALIZED",
                    reviewStatus = if (reviewActionAvailable) "PENDING_REVIEW" else "REVIEWED",
                    canReview = reviewActionAvailable,
                    hasReview = !reviewActionAvailable,
                    primaryActionKey = primaryActionKey,
                    primaryActionLabel = primaryActionLabel,
                    indicatorKey =
                        if (reviewActionAvailable) "leave_review" else "already_reviewed",
                    indicatorLabel =
                        if (reviewActionAvailable) "Pendiente de reseña" else "Reseña enviada",
                )
            ),
    )

private fun reservationCard(
    id: String,
    section: String,
    reviewStatus: String,
    canReview: Boolean,
    hasReview: Boolean,
    primaryActionKey: String? = null,
    primaryActionLabel: String? = null,
    indicatorKey: String? = null,
    indicatorLabel: String? = null,
) =
    MyReservationCard(
        id = id,
        complexName = "Moravia FC",
        courtName = if (id == FinalizedReservationId) "Cancha A" else "Cancha 1",
        startsAt = "2026-07-10T18:00:00.000Z",
        endsAt = "2026-07-10T19:00:00.000Z",
        status = if (section == "UPCOMING") "CONFIRMED" else "COMPLETED",
        section = section,
        reviewStatus = reviewStatus,
        canReview = canReview,
        hasReview = hasReview,
        primaryActionKey = primaryActionKey,
        primaryActionLabel = primaryActionLabel,
        indicatorKey = indicatorKey,
        indicatorLabel = indicatorLabel,
    )

private fun testNavigationState(): AuthenticatedNavigationState =
    AuthenticatedNavigationState(
        selectedRoute = mutableStateOf(AuthenticatedTopLevelRoute.Reservations),
        searchBackStack = NavBackStack<NavKey>(SearchRoute),
        reservationsBackStack = NavBackStack<NavKey>(ReservationsRoute),
        notificationsBackStack = NavBackStack<NavKey>(NotificationsRoute),
        myComplexBackStack = NavBackStack<NavKey>(MyComplexRoute),
        ownerCourtAvailabilityEntrypointState = mutableStateOf(null),
        myComplexHubReloadRequestKeyState = mutableStateOf(0),
        catalogReloadRequestKeyState = mutableStateOf(0),
        viewingAsPlayerState = mutableStateOf(true),
        hydratedOwnerPreferenceUserIdState = mutableStateOf(null),
    )
