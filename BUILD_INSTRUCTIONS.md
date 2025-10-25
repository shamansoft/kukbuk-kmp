# Build Instructions

## Prerequisites

- Android Studio Ladybug or later
- JDK 17 or later (note: if using JDK 21 with GraalVM, configuration cache is disabled)
- Android SDK with API 35
- Xcode 15+ (for iOS development)

## Building the Project

### Android

1. **Open in Android Studio:**
   ```bash
   # Open the project root directory in Android Studio
   ```

2. **Build APK:**
   ```bash
   ./gradlew composeApp:assembleDebug
   ```

3. **Install on device:**
   ```bash
   ./gradlew composeApp:installDebug
   ```

### iOS

1. **Build iOS framework:**
   ```bash
   ./gradlew composeApp:embedAndSignAppleFrameworkForXcode
   ```

2. **Open iOS project in Xcode:**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

### Web (WASM)

1. **Run development server:**
   ```bash
   ./gradlew composeApp:wasmJsBrowserDevelopmentRun
   ```

2. **Build for production:**
   ```bash
   ./gradlew composeApp:wasmJsBrowserDistribution
   ```

## Configuration Issues Fixed

### JDK Compatibility
- **Issue:** Configuration cache conflicts with JDK 21 (GraalVM) and Android tooling
- **Solution:** Configuration cache disabled in `gradle.properties`
- **File:** `gradle.properties` - `org.gradle.configuration-cache=false`

### Material Icons
- **Issue:** Material Icons not available across all platforms (iOS, WASM)
- **Solution:** Replaced with emoji icons for cross-platform compatibility
- **Affected:** Search (🔍), Refresh (🔄), Close (✕)

## Google Drive Integration Setup

To use the recipe collection features, you need:

1. **Google Cloud Console Setup:**
   - Enable Google Drive API
   - Create OAuth 2.0 credentials for Android/iOS/Web
   - Update `google_web_client_id` in `strings.xml`

2. **Authentication Setup:**
   - Follow instructions in `OAUTH_SETUP.md`
   - Configure OAuth clients for each platform

## Testing

### Unit Tests
```bash
./gradlew test
```

### Platform-specific Tests
```bash
# Android
./gradlew composeApp:testDebug

# iOS Simulator
./gradlew composeApp:iosSimulatorArm64Test

# WASM
./gradlew composeApp:wasmJsBrowserTest
```

## Troubleshooting

### Build Failures
1. **Clean and rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew composeApp:assembleDebug
   ```

2. **Configuration cache issues:**
   - Already disabled in `gradle.properties`
   - If issues persist, use `--no-configuration-cache` flag

3. **JDK version issues:**
   - Ensure using JDK 17 or compatible version
   - Check `JAVA_HOME` environment variable

### Authentication Issues
1. **Google Drive API not working:**
   - Verify OAuth setup in `OAUTH_SETUP.md`
   - Check API keys and client IDs
   - Ensure Google Drive API is enabled

2. **Sign-in failures:**
   - Verify SHA-1 fingerprints match
   - Check package names in OAuth configuration

## Architecture

### Multiplatform Structure
- `commonMain` - Shared business logic and UI
- `androidMain` - Android-specific implementations
- `iosMain` - iOS-specific implementations  
- `wasmJsMain` - Web-specific implementations

### Key Components
- **Authentication:** Modern Credential Manager API (Android), platform-specific auth
- **Recipe Data:** Google Drive API integration with YAML parsing
- **UI:** Material 3 with Compose Multiplatform
- **State Management:** Kotlin StateFlow with repository pattern

## Performance Considerations

- **Recipe List:** Uses LazyColumn for efficient scrolling with 100+ items
- **Caching:** Repository-level caching for downloaded recipes
- **Pull-to-refresh:** Material 3 PullToRefreshBox for smooth UX
- **Search:** Real-time filtering with debounced queries

## Next Steps

1. **Add proper image loading** for recipe thumbnails
2. **Implement recipe detail screen** for full recipe viewing
3. **Add offline support** with local database caching
4. **Enhance YAML parser** for more complex recipe formats
5. **Add recipe editing** capabilities