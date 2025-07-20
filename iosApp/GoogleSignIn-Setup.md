# iOS Google Sign-In Setup

## Required Dependencies

Add the following to your `iosApp/Podfile`:

```ruby
target 'iosApp' do
  use_frameworks!
  platform :ios, '12.0'
  
  # Google Sign-In
  pod 'GoogleSignIn'
end
```

## Configuration Steps

### 1. Google Cloud Console Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing project
3. Enable Google Sign-In API
4. Create OAuth 2.0 client IDs:
   - Web client (for server-side verification)
   - iOS client (bundle ID: `net.shamansoft.kukbuk`)

### 2. Configure Info.plist
Add the following to `iosApp/iosApp/Info.plist`:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string></string>
        <key>CFBundleURLSchemes</key>
        <array>
            <!-- Replace with your iOS OAuth client's reversed client ID -->
            <string>YOUR_REVERSED_CLIENT_ID</string>
        </array>
    </dict>
</array>
```

### 3. Update iOS App Delegate
In `iosApp/iosApp/iOSApp.swift`, configure Google Sign-In:

```swift
import SwiftUI
import GoogleSignIn

@main
struct iOSApp: App {
    init() {
        // Configure Google Sign-In
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let plist = NSDictionary(contentsOfFile: path),
           let clientId = plist["CLIENT_ID"] as? String {
            GoogleSignIn.GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientId)
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GoogleSignIn.GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
```

### 4. Add GoogleService-Info.plist
1. Download `GoogleService-Info.plist` from Firebase Console
2. Add it to your iOS project root
3. Ensure it's included in the target

## Implementation Notes

The current iOS implementation is a placeholder. To complete it:

1. Install dependencies via CocoaPods
2. Configure the project as described above
3. Update `IOSAuthenticationService.kt` to use Google Sign-In native APIs
4. Complete the Keychain storage implementation in `IOSSecureStorage.kt`