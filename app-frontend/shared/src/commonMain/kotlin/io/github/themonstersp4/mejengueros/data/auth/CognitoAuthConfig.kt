package io.github.themonstersp4.mejengueros.data.auth

data class CognitoAuthConfig(
    val clientId: String,
    val region: String,
    val domain: String,
    val redirectUri: String,
    val logoutUri: String,
    val scopes: List<String>,
) {
  val authorizationEndpoint: String = "$domain/oauth2/authorize"
  val tokenEndpoint: String = "$domain/oauth2/token"
  val logoutEndpoint: String = "$domain/logout"
}

val defaultCognitoAuthConfig =
    CognitoAuthConfig(
        clientId = "392mi2ii9l7usot25ksqj58gu6",
        region = "us-east-2",
        domain = "https://mejengueros-dev-auth.auth.us-east-2.amazoncognito.com",
        redirectUri = "com.themonsters.mejengueros://auth/callback",
        logoutUri = "com.themonsters.mejengueros://auth/logout",
        scopes = listOf("openid", "email", "profile"),
    )
