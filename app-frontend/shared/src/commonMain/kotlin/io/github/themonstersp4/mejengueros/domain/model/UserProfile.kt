package io.github.themonstersp4.mejengueros.domain.model

data class UserProfile(
    val id: String,
    val roles: List<UserRoleKind>,
    val cognitoSub: String? = null,
    val email: String? = null,
    val name: String? = null,
    val pictureUrl: String? = null,
    val provider: String? = null,
) {
  /**
   * Profiles are attributed to the authenticated session by Cognito subject because [id] is the
   * application user identifier and never matches the session subject. Responses that omit the
   * subject stay usable: the authenticated profile cache only ever holds the current session
   * profile, so they cannot belong to another user.
   */
  fun belongsToAuthenticatedUser(userId: String?): Boolean =
      userId != null && (cognitoSub == null || cognitoSub == userId)
}
