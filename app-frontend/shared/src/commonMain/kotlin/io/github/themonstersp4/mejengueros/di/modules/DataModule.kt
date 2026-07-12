package io.github.themonstersp4.mejengueros.di.modules

import io.github.themonstersp4.mejengueros.data.auth.AuthSessionExpirationNotifier
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
import io.github.themonstersp4.mejengueros.data.remote.CourtImageUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.CourtReviewsRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IAuthenticatedUserRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICognitoNativeAuthDataSource
import io.github.themonstersp4.mejengueros.data.remote.IComplexRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtAvailabilityRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtDetailRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtImageUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ICourtReviewsRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.INotificationRealtimeDataSource
import io.github.themonstersp4.mejengueros.data.remote.INotificationRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IPokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IReservationRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IReviewEvidenceUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.IReviewRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.NotificationRealtimeDataSource
import io.github.themonstersp4.mejengueros.data.remote.NotificationRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.PokemonRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ReservationRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ReviewEvidenceUploadRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.ReviewRemoteDataSource
import io.github.themonstersp4.mejengueros.data.remote.defaultAppApiConfig
import io.github.themonstersp4.mejengueros.data.repository.AuthRepository
import io.github.themonstersp4.mejengueros.data.repository.ComplexRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtAvailabilityRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtCatalogRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtDetailRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtImageUploadRepository
import io.github.themonstersp4.mejengueros.data.repository.CourtReviewsRepository
import io.github.themonstersp4.mejengueros.data.repository.NotificationRepository
import io.github.themonstersp4.mejengueros.data.repository.PokemonRepository
import io.github.themonstersp4.mejengueros.data.repository.ReservationRepository
import io.github.themonstersp4.mejengueros.data.repository.ReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.data.repository.ReviewRepository
import io.github.themonstersp4.mejengueros.domain.repository.IAuthRepository
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtDetailRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtImageUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtReviewsRepository
import io.github.themonstersp4.mejengueros.domain.repository.INotificationRepository
import io.github.themonstersp4.mejengueros.domain.repository.IPokemonRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReservationRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewEvidenceUploadRepository
import io.github.themonstersp4.mejengueros.domain.repository.IReviewRepository
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
  single { AuthSessionExpirationNotifier() }
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
  single<ICourtImageUploadRemoteDataSource> {
    CourtImageUploadRemoteDataSource(get(named(AppApiHttpClientQualifier)), get(), get())
  }
  single<ICourtImageUploadRepository> { CourtImageUploadRepository(get()) }
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
  single<IReservationRemoteDataSource> {
    ReservationRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<IReservationRepository> { ReservationRepository(get()) }
  single<IReviewRemoteDataSource> {
    ReviewRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<IReviewRepository> { ReviewRepository(get()) }
  single<INotificationRemoteDataSource> {
    NotificationRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<INotificationRealtimeDataSource> {
    NotificationRealtimeDataSource(get(), get(), get(), get())
  }
  single<INotificationRepository> { NotificationRepository(get(), get()) }
  single<IReviewEvidenceUploadRemoteDataSource> {
    ReviewEvidenceUploadRemoteDataSource(get(named(AppApiHttpClientQualifier)), get(), get())
  }
  single<IReviewEvidenceUploadRepository> { ReviewEvidenceUploadRepository(get()) }
  single<ICourtDetailRepository> { CourtDetailRepository(get()) }
  single<ICourtReviewsRemoteDataSource> {
    CourtReviewsRemoteDataSource(get(named(AppApiHttpClientQualifier)), get())
  }
  single<ICourtReviewsRepository> { CourtReviewsRepository(get()) }
  single<IPokemonRemoteDataSource> { PokemonRemoteDataSource(get()) }
  single<IPokemonLocalDataSource> { PokemonLocalDataSource(get()) }
  single<IPokemonRepository> { PokemonRepository(get(), get()) }
}
