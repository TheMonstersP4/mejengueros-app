// commonMain
package example.monitoring

interface AnalyticsPort {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

interface CrashPort {
    fun recordException(throwable: Throwable, message: String? = null)
}

// androidMain / iosMain
class GitLiveAnalyticsAdapter : AnalyticsPort {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        runCatching {
            Firebase.analytics.logEvent(name) {
                params.forEach { (key, value) -> value?.let { param(key, it.toString()) } }
            }
        }
    }
}

class GitLiveCrashAdapter : CrashPort {
    override fun recordException(throwable: Throwable, message: String?) {
        runCatching {
            message?.let { Firebase.crashlytics.log(it) }
            Firebase.crashlytics.recordException(throwable)
        }
    }
}

// jvmMain
class NoOpAnalyticsAdapter : AnalyticsPort { override fun logEvent(name: String, params: Map<String, Any?>) = Unit }
class NoOpCrashAdapter : CrashPort { override fun recordException(throwable: Throwable, message: String?) = Unit }
