# Local Development Setup

This document describes how to use local recipe files for development instead of Google Drive.

## Quick Start

### 1. Push Recipe Files to Device

```bash
./scripts/push-recipes-to-app.sh
```

This will copy all YAML files from `no-git/kukbuk/` to the app's internal storage.

### 2. Run the App

- Open the project in Android Studio
- Click Run (or press `Shift+F10`)
- The app will automatically use local files in debug mode

### 3. Verify It's Working

Check the logs:
```bash
adb logcat | grep -E "(AndroidApp|LocalFileDataSource|RecipeRepo)"
```

You should see:
```
D AndroidApp: Using LOCAL data source (local files)
D LocalFileDataSource: Found XX recipe files in /data/data/net.shamansoft.kukbuk/files/recipes
```

---

## How It Works

### Automatic Mode Selection

The app automatically chooses the data source based on:

1. **Build Type**: Debug builds default to LOCAL mode
2. **Platform**: Each platform can have different paths
3. **Configuration**: Can be overridden programmatically

### Architecture

```
┌─────────────────────────────────────┐
│     DataSourceConfig.mode           │
│  (LOCAL or PRODUCTION)              │
└──────────────┬──────────────────────┘
               │
       ┌───────┴────────┐
       │                │
  LOCAL mode       PRODUCTION mode
       │                │
       ▼                ▼
┌──────────────┐  ┌─────────────────┐
│ LocalFile    │  │ GoogleDrive     │
│ DataSource   │  │ DataSource      │
└──────┬───────┘  └────────┬────────┘
       │                   │
       └─────────┬─────────┘
                 ▼
        ┌────────────────┐
        │ RecipeRepository│
        └────────────────┘
```

---

## File Locations

### Android
- **Path**: `/data/data/net.shamansoft.kukbuk/files/recipes/`
- **Access**: App's internal storage (no permissions required)
- **Push command**: `./scripts/push-recipes-to-app.sh`

### iOS (Future)
- **Path**: `Documents/kukbuk/`
- **Access**: App's documents directory

### WASM (Not Supported)
- Local files not available on web platform
- Always uses Google Drive

---

## Development Workflows

### Adding/Updating Recipes

1. Add or edit YAML files in `no-git/kukbuk/`
2. Run `./scripts/push-recipes-to-app.sh`
3. Pull down to refresh in the app (or restart)

### Testing Google Drive Integration

**Option 1**: Override in code
```kotlin
// In App.kt or AndroidApp.kt
DataSourceConfig.mode = DataSourceMode.PRODUCTION
```

**Option 2**: Build release variant
```bash
./gradlew composeApp:assembleRelease
```

### Switching Between Modes

The app defaults to LOCAL mode in debug builds. To force a specific mode:

```kotlin
// Before Koin initialization
DataSourceConfig.mode = DataSourceMode.LOCAL    // Use local files
// or
DataSourceConfig.mode = DataSourceMode.PRODUCTION  // Use Google Drive
```

---

## Dependency Injection

### Koin Modules

**Common Modules**:
- `recipeModule` - RecipeRepository, YamlRecipeParser
- `viewModelModule` - All ViewModels

**Platform-Specific**:
- `androidAuthModule` - Android auth with Context/Activity
- `productionDataModule` - GoogleDriveRecipeDataSource
- `localDevDataModule` - LocalFileRecipeDataSource

**Android-Specific Initialization**:
```kotlin
AndroidApp() {
    // Provides MainActivity to Koin
    // Initializes with appropriate modules
    // Calls common AppContent()
}
```

---

## Key Files

### Data Source Abstraction
- `RecipeDataSource.kt` - Interface for all data sources
- `GoogleDriveRecipeDataSource.kt` - Google Drive implementation
- `LocalFileRecipeDataSource.kt` - Local files implementation (uses Okio)

### Dependency Injection
- `di/AppModule.kt` - Common Koin modules + expect/actual
- `di/AndroidModule.kt` - Android-specific modules
- `di/IosModule.kt` - iOS modules
- `di/WasmModule.kt` - WASM modules

### Platform Configuration
- `PlatformConfig.kt` - expect declarations
- `PlatformConfig.android.kt` - Android implementations
- `PlatformConfig.ios.kt` - iOS implementations
- `PlatformConfig.wasmJs.kt` - WASM implementations

### Configuration
- `DataSourceConfig.kt` - Mode selection logic

### Android Entry Points
- `AndroidApp.kt` - Android-specific Koin initialization
- `MainActivity.kt` - Initializes platform config

---

## Troubleshooting

### "Found 0 recipe files"

**Solution**: Re-push the files
```bash
./scripts/push-recipes-to-app.sh
```

**Verify files exist**:
```bash
adb shell "run-as net.shamansoft.kukbuk ls /data/data/net.shamansoft.kukbuk/files/recipes"
```

### "Directory does not exist"

The app needs to be installed first (the directory is created on first run).

**Solution**:
1. Install the app
2. Run the push script
3. Restart the app

### Koin crashes on startup

Make sure:
- MainActivity is calling `initPlatformConfig(applicationContext)`
- AndroidApp is being used (not common App())
- All platform actuals are implemented

### Files not updating

The app caches recipes. To force refresh:
1. Pull down to refresh in the app
2. Or restart the app completely

---

## Future Enhancements

- [ ] Add iOS local file support
- [ ] Runtime toggle in debug menu
- [ ] File watching for auto-reload during development
- [ ] Gradle task to automatically push files after build
- [ ] Support for both local + cloud (hybrid mode)

---

## Technical Details

### Why Internal Storage?

Android's scoped storage (API 30+) restricts access to external storage. Using the app's internal storage (`filesDir`) provides:
- ✅ No permissions required
- ✅ Works on all Android versions
- ✅ Secure (only app can access)
- ✅ Automatically cleaned up on uninstall

### Why Okio?

Okio provides:
- ✅ Multiplatform file I/O
- ✅ Better performance than java.io
- ✅ Modern, idiomatic API
- ✅ Used by Square ecosystem (Ktor, etc.)

### Why Koin?

Koin provides:
- ✅ Pure Kotlin DI (no code generation)
- ✅ Multiplatform support
- ✅ Compose integration
- ✅ Easy platform-specific modules
- ✅ Lightweight and performant

---

**Happy local development!** 🚀
