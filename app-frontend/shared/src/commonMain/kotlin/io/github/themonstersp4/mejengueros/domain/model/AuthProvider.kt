package io.github.themonstersp4.mejengueros.domain.model

enum class AuthProvider(val cognitoIdentityProvider: String, val displayName: String) {
  Google(cognitoIdentityProvider = "Google", displayName = "Google"),
  Microsoft(cognitoIdentityProvider = "Microsoft", displayName = "Microsoft"),
}
