package io.github.themonstersp4.mejengueros.domain.model

data class UserProfile(
    val id: String,
    val roles: List<UserRoleKind>,
    val email: String? = null,
    val name: String? = null,
    val pictureUrl: String? = null,
    val provider: String? = null,
)
