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
import io.github.themonstersp4.mejengueros.data.remote.AppApiHttpClientQualifier
import io.github.themonstersp4.mejengueros.data.remote.AuthenticatedUserRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CognitoAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CognitoNativeAuthDataSource
import io.github.themonstersp4.mejengueros.data.remote.ComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CourtAvailabilityRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CourtDetailRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthenticatedUserRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICognitoNativeAuthDataSource
import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtAvailabilityRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtDetailRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.PokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.defaultAppApiConfig
import io.github.themonstersp4.mejengueros.data.repository.AuthRepository
import io.github.themonstersp4.mejengueros.data.repository.ComplexRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtAvailabilityRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtCatalogRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtDetailRepository
import io.github.themonstersp4.mejengueros.data.repository.PokemonRepository
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import org.koin.core.qualifier.named
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
  single<ICognitoNativeAuthDataSource> { CognitoNativeAuthDataSource(get(), get(), get()) }
  single<IAuthenticatedUserRemoteDataSource> {
    AuthenticatedUserRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<IAuthRepository> {
    AuthRepository(get(), get(), get(), get(), get(), get(), get(), get(), get())
  }
  single<IComplexRemoteDataSource> {
    ComplexRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<IComplexRepository> { ComplexRepository(get()) }
  single<ICourtCatalogRemoteDataSource> {
    CourtCatalogRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<ICourtCatalogRepository> { CourtCatalogRepository(get()) }
  single<ICourtAvailabilityRemoteDataSource> {
    CourtAvailabilityRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<ICourtAvailabilityRepository> { CourtAvailabilityRepository(get()) }
  single<ICourtDetailRemoteDataSource> {
    CourtDetailRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<ICourtDetailRepository> { CourtDetailRepository(get()) }
  single<IPokemonRemoteDataSource> { PokemonRemoteDataSource(get()) }
  single<IPokemonLocalDataSource> { PokemonLocalDataSource(get()) }
  single<IPokemonRepository> { PokemonRepository(get(), get()) }
}
