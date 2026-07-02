package io.github.themonstersp4.mejengueros.domain.model

data class UserProfile(
    val id: String,
    val roles: List<UserRoleKind>,
)
