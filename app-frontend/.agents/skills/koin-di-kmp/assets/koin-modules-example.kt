package example.di

import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val dataModule = module {
    single { get<DriverFactory>().createDriver() }
    single { AppDatabase(get()) }
    single<LocalLaunchDataSource> { SqlDelightLaunchDataSource(get()) }
    single<RemoteLaunchDataSource> { KtorLaunchDataSource(get(), Dispatchers.IO) }
    single<LaunchRepository> { DefaultLaunchRepository(get(), get(), Dispatchers.Default) }
}

val presentationModule = module {
    factory { LaunchListViewModel(get()) }
}

val sharedModule = module {
    includes(networkModule, dataModule, presentationModule, platformModule())
}
