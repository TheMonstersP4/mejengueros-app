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
import io.github.themonstersp4.mejengueros.domain.model.ConfirmedReviewEvidenceImageUpload
import io.github.themonstersp4.mejengueros.domain.model.CreateReviewRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedReview
import io.github.themonstersp4.mejengueros.domain.model.LocalReviewEvidenceImage
import io.github.themonstersp4.mejengueros.domain.model.ReceivedReviewPage
import io.github.themonstersp4.mejengueros.domain.model.ReviewableReservation
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
import io.github.themonstersp4.mejengueros.presentation.review.ReviewViewModel
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import io.github.themonstersp4.mejengueros.ui.components.ReviewEvidenceImagePickerController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
  fun launcherOpensFormAndBackReturnsToLauncher() = runTest {
    val navigationState = testNavigationState()
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()

    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertExists()
    composeRule.onNodeWithContentDescription("Seleccionar 3 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Comentario temporal")
    composeRule.onNodeWithTag("leave_review_submit_button").assertExists()

    composeRule.onNodeWithContentDescription("Volver").performClick()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Comentario temporal").assertDoesNotExist()
  }

  @Test
  fun successBackReturnsToLauncherAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
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

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Otro comentario temporal").assertDoesNotExist()
  }

  @Test
  fun oneStarBlankCommentStaysBlockedUntilCommentIsProvided() = runTest {
    val navigationState = testNavigationState()
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
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
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
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
    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("La cancha estaba impecable").assertDoesNotExist()
    composeRule.onNodeWithContentDescription("Volver").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("EXPLORAR CANCHAS").performClick()

    composeRule.onNodeWithText("Buscar canchas").assertExists()
    composeRule.runOnIdle {
      assertEquals(AuthenticatedTopLevelRoute.Search, navigationState.selectedRoute)
      assertEquals(listOf(SearchRoute), navigationState.currentBackStack.toList())
    }

    navigationState.selectReservations()
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()
  }

  @Test
  fun systemBackFromFormReturnsToLauncherAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 4 de 5 estrellas").performClick()
    composeRule
        .onNodeWithText("Contá tu experiencia: la cancha, la superficie, el ambiente...")
        .performTextInput("Borrador que no debe sobrevivir")

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("¿Cómo estuvo tu mejenga?").assertDoesNotExist()
    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").assertIsNotEnabled()
    composeRule.onNodeWithText("Borrador que no debe sobrevivir").assertDoesNotExist()
  }

  @Test
  fun systemBackFromSuccessReturnsToLauncherAndClearsDraft() = runTest {
    val navigationState = testNavigationState()
    val viewModel = createReviewViewModel(TestScope(StandardTestDispatcher(testScheduler)))
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("PREPARAR RESEÑA").performClick()
    composeRule.onNodeWithContentDescription("Seleccionar 5 de 5 estrellas").performClick()
    composeRule.onNodeWithTag("leave_review_submit_button").performClick()
    advanceUntilIdle()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("TU RESEÑA FUE ENVIADA").assertExists()

    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    composeRule.onNodeWithText("VISTA PREVIA DE TU RESEÑA").assertDoesNotExist()
  }

  @Test
  fun launcherShowsLoadingWhileLatestReservationIsStillResolving() = runTest {
    val navigationState = testNavigationState()
    val latestReservationResult = CompletableDeferred<ReviewableReservation?>()
    val viewModel =
        createReviewViewModel(
            coroutineScope = TestScope(StandardTestDispatcher(testScheduler)),
            reviewRepository = DelayedLatestReservationReviewRepository(latestReservationResult),
        )

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule
        .onNodeWithText("Estamos buscando tu última reserva pendiente de reseña.")
        .assertExists()

    latestReservationResult.complete(sampleReservation())
    advanceUntilIdle()
    composeRule.waitForIdle()
  }

  @Test
  fun launcherShowsEmptyStateWhenThereIsNoEligibleReservation() = runTest {
    val navigationState = testNavigationState()
    val viewModel =
        createReviewViewModel(
            coroutineScope = TestScope(StandardTestDispatcher(testScheduler)),
            reviewRepository = EmptyLatestReservationReviewRepository(),
        )
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule
        .onNodeWithText(
            "Todavía no tenés reservas completadas pendientes de reseña. Cuando terminés una mejenga, la vas a ver aquí."
        )
        .assertExists()
    composeRule.onNodeWithText("PREPARAR RESEÑA").assertDoesNotExist()
  }

  @Test
  fun launcherRetryRecoversFromLoadFailure() = runTest {
    val navigationState = testNavigationState()
    val reviewRepository = RetryableLatestReservationReviewRepository()
    val viewModel =
        createReviewViewModel(
            coroutineScope = TestScope(StandardTestDispatcher(testScheduler)),
            reviewRepository = reviewRepository,
        )
    advanceUntilIdle()

    composeRule.setContent {
      MejenguerosTheme {
        ReservationsNavigationTestHost(navigationState = navigationState, viewModel = viewModel)
      }
    }

    composeRule.onNodeWithText("No pudimos cargar la reserva pendiente de reseña.").assertExists()
    composeRule.onNodeWithText("REINTENTAR").performClick()

    advanceUntilIdle()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Tu última mejenga ya está lista para reseña").assertExists()
    assertEquals(2, reviewRepository.loadCalls)
  }

  @Composable
  private fun ReservationsNavigationTestHost(
      navigationState: AuthenticatedNavigationState,
      viewModel: ReviewViewModel,
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
              viewModel = viewModel,
              reviewEvidenceImagePickerController =
                  ReviewEvidenceImagePickerController(
                      isAvailable = true,
                      launch = {
                        viewModel.updateSelectedEvidenceImage(
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

  private fun createReviewViewModel(
      coroutineScope: TestScope,
      reviewRepository: IReviewRepository =
          object : IReviewRepository {
            override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
                sampleReservation()

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
          },
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

  private class DelayedLatestReservationReviewRepository(
      private val latestReservationResult: CompletableDeferred<ReviewableReservation?>
  ) : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? =
        latestReservationResult.await()

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create review in launcher loading test.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in launcher loading test.")
  }

  private class EmptyLatestReservationReviewRepository : IReviewRepository {
    override suspend fun getLatestReviewableReservation(): ReviewableReservation? = null

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create review in empty launcher test.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in empty launcher test.")
  }

  private class RetryableLatestReservationReviewRepository : IReviewRepository {
    var loadCalls: Int = 0

    override suspend fun getLatestReviewableReservation(): ReviewableReservation? {
      loadCalls += 1
      if (loadCalls == 1) {
        throw RuntimeException("Temporary launcher failure")
      }
      return sampleReservation()
    }

    override suspend fun createReview(request: CreateReviewRequest): CreatedReview =
        error("Should not create review in launcher retry test.")

    override suspend fun getOwnerReceivedReviews(
        courtId: String?,
        page: Int,
        pageSize: Int,
    ): ReceivedReviewPage = error("Not used in launcher retry test.")
  }
}

private fun sampleReservation() =
    ReviewableReservation(
        reservationId = "reservation-id",
        complexName = "Moravia FC",
        courtName = "Cancha A",
        startsAt = "2026-07-03T02:00:00.000Z",
        endsAt = "2026-07-03T03:00:00.000Z",
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
