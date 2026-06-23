# Mejengueros Frontend

Aplicación Kotlin Multiplatform de Mejengueros con objetivos actuales para Android, iOS y Desktop (JVM).

## Propósito

Este subproyecto provee la base técnica del frontend del MVP: interfaz compartida con Compose Multiplatform, navegación tipada, autenticación con Cognito y primeros flujos funcionales que sirven como referencia para nuevas features.

## Estructura principal

```text
androidApp/   Punto de entrada Android
desktopApp/   Punto de entrada Desktop/JVM
iosApp/       Wrapper SwiftUI/Xcode para iOS
shared/       UI compartida, navegación y lógica común
Taskfile.yml  Superficie recomendada de comandos
```

La lógica compartida vive en `shared/`, con una organización principal como esta:

```text
shared/src/commonMain/kotlin/io/github/themonstersp4/mejengueros/
  app/
  data/
  di/
  domain/
  navigation/
  presentation/
  screens/
  theme/
```

## Decisiones técnicas base

- La UI compartida y la lógica de app viven en `shared/`; no existe un módulo `composeApp`.
- La app usa Navigation 3, Koin, Ktor, SQLDelight y Material 3.
- La autenticación usa Amazon Cognito; Firebase no forma parte de esta base.
- Las pantallas renderizan estado inmutable y delegan acciones; los `ViewModel` dependen de repositorios, no de Ktor o SQLDelight directamente.

## Flujo arquitectónico esperado

```text
Screen
  -> Route/Entry
    -> ViewModel
      -> Repository interface
        -> Repository implementation
          -> RemoteDataSource -> Ktor
          -> LocalDataSource  -> SQLDelight
```

## Funcionalidades de referencia actuales

- Shell autenticado con navegación principal.
- Flujo de autenticación con Cognito (email/password y Hosted UI).
- Feature `Pokedex` como ejemplo de arquitectura, navegación, caché local y consumo remoto.

## Prerrequisitos y preparación básica

- JDK compatible con Gradle/Kotlin Multiplatform del proyecto.
- Android SDK para compilación Android.
- Xcode si vas a ejecutar `iosApp/`.
- [Task](https://taskfile.dev/) es opcional, pero es la interfaz recomendada para comandos repetibles.

Si necesitas valores de integración, revisa `.env.example` como referencia de configuración de desarrollo. Ese archivo no se carga automáticamente en runtime.

## Comandos principales

Ejecuta estos comandos desde `app-frontend/`.

### Vía Taskfile (recomendado)

```bash
task spotless:apply
task spotless:check
task format
task check
task test
task test:auth
task android:debug
task android:host-test
task android:host-test:auth
task desktop:compile
task verify
```

### Gradle directo (fallback)

Usa `./gradlew` en Unix/macOS y `./gradlew.bat` en PowerShell/Windows.

```bash
./gradlew spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin --no-configuration-cache --console=plain
./gradlew :androidApp:assembleDebug
./gradlew :shared:jvmTest
./gradlew :shared:testAndroidHostTest
./gradlew :desktopApp:compileKotlin
```

```powershell
./gradlew.bat spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin --no-configuration-cache --console=plain
./gradlew.bat :androidApp:assembleDebug
./gradlew.bat :shared:jvmTest
./gradlew.bat :shared:testAndroidHostTest
./gradlew.bat :desktopApp:compileKotlin
```

## Alcance actual de verificación

La validación activa cubre Android, Desktop/JVM y lógica KMP compartida. El wrapper iOS sigue en el repositorio, pero hoy no forma parte del alcance principal de CI.

## Dónde profundizar

- [`shared/`](shared/): arquitectura compartida, pantallas, navegación y datos.
- [`Taskfile.yml`](Taskfile.yml): comandos repetibles para formato, compilación y pruebas.
- [`../docs/design/README.md`](../docs/design/README.md): contexto visual y funcional del producto.
- [`../README.md`](../README.md): contexto general del repositorio y alcance del MVP.
