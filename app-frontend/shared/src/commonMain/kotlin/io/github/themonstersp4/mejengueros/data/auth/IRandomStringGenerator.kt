package io.github.themonstersp4.mejengueros.data.auth

interface IRandomStringGenerator {
  fun generate(length: Int): String
}
