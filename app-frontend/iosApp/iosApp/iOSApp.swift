import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if url.host == "auth" && url.path == "/callback" {
                        AuthCallbackBusKt.handleOAuthCallback(url: url.absoluteString)
                    }
                }
        }
    }
}
