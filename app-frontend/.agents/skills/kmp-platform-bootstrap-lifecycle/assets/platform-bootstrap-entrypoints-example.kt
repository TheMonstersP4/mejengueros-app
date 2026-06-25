// androidApp: Application owns app-wide service initialization.
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        initKoin { androidContext(this@MainApplication) }
    }
}

// androidApp: Activity owns permissions and Compose content.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent { App() }
    }
}

// composeApp iosMain: bridge config initializes shared Kotlin services before App.
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
        Firebase.initialize()
    },
) { App() }

// desktopApp: initialize shared services before creating the window.
fun main() {
    initKoin()
    application { Window(onCloseRequest = ::exitApplication) { App() } }
}
