// gradle/libs.versions.toml
// [libraries]
// ktor-client-core = { module = "io.ktor:ktor-client-core" }
// ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp" }
// ktor-client-darwin = { module = "io.ktor:ktor-client-darwin" }

kotlin {
    android { /* Android target config */ }
    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}
