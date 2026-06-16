package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.CognitoAuthTokenProvider
import io.github.themonstersp4.mejengueros.data.auth.CognitoOAuthRequestFactory
import io.github.themonstersp4.mejengueros.data.auth.IAuthSecureStorage
import io.github.themonstersp4.mejengueros.data.auth.IAuthTokenProvider
import io.github.themonstersp4.mejengueros.data.auth.JwtIdTokenDecoder
import io.github.themonstersp4.mejengueros.data.auth.OAuthCallbackParser
import io.github.themonstersp4.mejengueros.data.auth.PkceGenerator
import io.github.themonstersp4.mejengueros.data.auth.defaultCognitoAuthConfig
import io.github.themonstersp4.mejengueros.data.local.AppDatabase
import io.github.themonstersp4.mejengueros.data.local.DriverFactory
import io.github.themonstersp4.mejengueros.data.local.IPokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.local.PokemonLocalDataSource
import io.github.themonstersp4.mejengueros.data.remote.CognitoAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.PokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.defaultAppApiConfig
import io.github.themonstersp4.mejengueros.data.repository.AuthRepository
import io.github.themonstersp4.mejengueros.data.repository.PokemonRepository
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import org.koin.dsl.module

val dataModule = module {
  single { get<DriverFactory>().createDriver() }
  single { AppDatabase(get()) }
  single { get<AppDatabase>().pokemonCacheQueries }
  single { defaultAppApiConfig }
  single { defaultCognitoAuthConfig }
  single { PkceGenerator(get()) }
  single { CognitoOAuthRequestFactory(get()) }
  single { OAuthCallbackParser() }
  single { JwtIdTokenDecoder(get()) }
  single<IAuthTokenProvider> { CognitoAuthTokenProvider(get<IAuthSecureStorage>()) }
  single<IAuthRemoteDataSource> { CognitoAuthRemoteDataSource(get(), get()) }
  single<IAuthRepository> { AuthRepository(get(), get(), get(), get(), get(), get(), get()) }
  single<IPokemonRemoteDataSource> { PokemonRemoteDataSource(get()) }
  single<IPokemonLocalDataSource> { PokemonLocalDataSource(get()) }
  single<IPokemonRepository> { PokemonRepository(get(), get()) }
}
