// commonMain
package example.messaging

interface PushMessagingPort {
    suspend fun getToken(): String
    fun subscribeToTopic(topic: String)
    fun unsubscribeFromTopic(topic: String)
}

// androidMain / iosMain adapter shape
class GitLivePushMessagingAdapter : PushMessagingPort {
    override suspend fun getToken(): String = Firebase.messaging.getToken()
    override fun subscribeToTopic(topic: String) { Firebase.messaging.subscribeToTopic(topic) }
    override fun unsubscribeFromTopic(topic: String) { Firebase.messaging.unsubscribeFromTopic(topic) }
}

// Android permission boundary belongs in Activity/platform UI, not common screens.
fun requestNotificationPermissionIfNeeded(activity: Activity) {
    // Request POST_NOTIFICATIONS on Android 13+ before token-dependent user flows.
}

// jvmMain fallback shape
class UnsupportedPushMessagingAdapter : PushMessagingPort {
    override suspend fun getToken(): String = throw UnsupportedOperationException("Push messaging unsupported")
    override fun subscribeToTopic(topic: String) = Unit
    override fun unsubscribeFromTopic(topic: String) = Unit
}
