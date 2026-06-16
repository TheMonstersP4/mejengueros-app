package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.presentation.auth.AuthViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonDetailViewModel
import io.github.themonstersp4.mejengueros.presentation.pokedex.PokemonListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
  viewModel { AuthViewModel(get(), get()) }
  viewModel { PokemonListViewModel(get()) }
  viewModel { parameters -> PokemonDetailViewModel(parameters.get(), get()) }
}
