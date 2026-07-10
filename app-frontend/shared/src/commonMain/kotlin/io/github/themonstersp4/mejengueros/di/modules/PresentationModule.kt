package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.availability.CourtAvailabilityViewModel
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtViewModel
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.courtdetail.CourtDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexViewModel
import io.github.themonstersp4.mejengueros.presentation.myreservations.MyReservationsViewModel
import io.github.themonstersp4.mejengueros.presentation.ownerreviews.OwnerReceivedReviewsViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListViewModel
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationContext
import io.github.themonstersp4.mejengueros.presentation.reservation.ReservationViewModel
import io.github.themonstersp4.mejengueros.presentation.review.ReviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
  viewModel { AuthViewModel(get(), get(), get()) }
  viewModel { CourtCatalogViewModel(get()) }
  viewModel { parameters -> CourtDetailViewModel(parameters.get(), get(), get()) }
  viewModel { CreateComplexViewModel(get(), get()) }
  viewModel { parameters ->
    AddCourtViewModel(parameters.get(), parameters.get(), get(), get(), get())
  }
  viewModel { MyComplexViewModel(get(), get(), get()) }
  viewModel { parameters ->
    CourtAvailabilityViewModel(parameters.get(), parameters.get(), parameters.get(), get())
  }
  viewModel { parameters ->
    ReservationViewModel(
        context = parameters.get<ReservationContext>(),
        repository = get(),
        errorReporter = get(),
    )
  }
  viewModel { ReviewViewModel(get(), get(), get()) }
  viewModel { OwnerReceivedReviewsViewModel(get(), get(), get()) }
  viewModel { MyReservationsViewModel(get(), get()) }
  viewModel { PokemonListViewModel(get()) }
  viewModel { parameters -> PokemonDetailViewModel(parameters.get(), get()) }
}
