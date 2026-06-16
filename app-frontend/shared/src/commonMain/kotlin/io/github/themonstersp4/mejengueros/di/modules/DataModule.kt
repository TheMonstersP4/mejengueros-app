package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.local.AppDatabase
import io.github.themonstersp4.mejengueros.data.local.AuthLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.data.local.IAuthLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.IPokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.PokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.PokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.repository.AuthRepository
import io.github.themonstersp4.mejengueros.data.repository.PokemonRepository
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import org.koin.dsl.module

val dataModule = module {
  single { get<DriverFactory>().createDriver() }
  single { AppDatabase(get()) }
  single { get<AppDatabase>().authSessionQueries }
  single { get<AppDatabase>().pokemonCacheQueries }
  single<IAuthLocalDataSource> { AuthLocalDataSource(get()) }
  single<IAuthRepository> { AuthRepository(get()) }
  single<IPokemonRemoteDataSource> { PokemonRemoteDataSource(get()) }
  single<IPokemonLocalDataSource> { PokemonLocalDataSource(get()) }
  single<IPokemonRepository> { PokemonRepository(get(), get()) }
}
