// commonMain
package example.auth

interface AuthPort {
    fun currentUserId(): String?
    fun isAnonymousUser(): Boolean
    suspend fun signInAnonymously(): String
    suspend fun signOut()
}

// androidMain / iosMain
class GitLiveFirebaseAuthAdapter : AuthPort {
    override fun currentUserId(): String? = Firebase.auth.currentUser?.uid
    override fun isAnonymousUser(): Boolean = Firebase.auth.currentUser?.isAnonymous == true

    override suspend fun signInAnonymously(): String {
        Firebase.auth.signInAnonymously()
        return requireNotNull(Firebase.auth.currentUser?.uid)
    }

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }
}

// jvmMain prototype fallback
class InMemoryAnonymousAuthAdapter : AuthPort {
    private var userId: String? = null
    override fun currentUserId(): String? = userId
    override fun isAnonymousUser(): Boolean = userId != null
    override suspend fun signInAnonymously(): String = userId ?: "desktop-anon".also { userId = it }
    override suspend fun signOut() { userId = null }
}
