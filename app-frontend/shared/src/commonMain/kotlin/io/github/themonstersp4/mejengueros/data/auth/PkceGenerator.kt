package io.github.themonstersp4.mejengueros.data.auth

data class PkcePair(val codeVerifier: String, val codeChallenge: String)

class PkceGenerator(private val randomStringGenerator: IRandomStringGenerator) {
  fun generate(): PkcePair {
    val codeVerifier = randomStringGenerator.generate(CodeVerifierLength)
    val codeChallenge = Base64Url.encode(Sha256.digest(codeVerifier.encodeToByteArray()))
    return PkcePair(codeVerifier = codeVerifier, codeChallenge = codeChallenge)
  }

  private companion object {
    const val CodeVerifierLength = 64
  }
}
