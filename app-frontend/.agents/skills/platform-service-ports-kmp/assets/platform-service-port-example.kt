// commonMain
package example.platform

interface CrashReporterPort {
    fun recordException(throwable: Throwable, message: String? = null)
}

class ReportUseCase(private val crashReporter: CrashReporterPort) {
    fun report(error: Throwable) {
        crashReporter.recordException(error, "Recoverable feature failure")
    }
}

// androidMain / iosMain adapter shape
class FirebaseCrashReporterAdapter : CrashReporterPort {
    override fun recordException(throwable: Throwable, message: String?) {
        // Forward to Firebase/GitLive here. Do not expose Firebase types to commonMain.
    }
}

// jvmMain fallback shape
class NoOpCrashReporterAdapter : CrashReporterPort {
    override fun recordException(throwable: Throwable, message: String?) = Unit
}

// DI shape
val platformServicesModule = module {
    single<CrashReporterPort> { FirebaseCrashReporterAdapter() } // or NoOp on unsupported targets
}
