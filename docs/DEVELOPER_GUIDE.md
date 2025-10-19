# Kukbuk - Developer Guide

**For Java Backend Developers Supporting This Kotlin Multiplatform Mobile App**

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Project Structure](#project-structure)
3. [Architecture & Patterns](#architecture--patterns)
4. [Package Guide](#package-guide)
5. [Key Entry Points](#key-entry-points)
6. [Data Flow](#data-flow)
7. [Authentication Flow](#authentication-flow)
8. [Build & Run](#build--run)
9. [Common Tasks](#common-tasks)
10. [Important Concepts for Java Developers](#important-concepts-for-java-developers)

---

## Project Overview

**Kukbuk** is a recipe management app that syncs with Google Drive. It's built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting:
- Android (primary platform)
- iOS
- Web (WASM)

**Package name**: `net.shamansoft.kukbuk`

**Tech Stack**:
- Kotlin 2.2.0
- Compose Multiplatform 1.8.2 (similar to Jetpack Compose)
- Ktor (HTTP client, like OkHttp/Retrofit)
- Kotlinx Serialization (like Jackson/Gson)
- Coroutines (async/await pattern)
- StateFlow (like RxJava/LiveData)

---

## Project Structure

```
sar-kmp/
├── composeApp/                    # Main application module
│   ├── src/
│   │   ├── commonMain/            # Shared code (95% of app logic)
│   │   │   ├── kotlin/
│   │   │   │   └── net/shamansoft/kukbuk/
│   │   │   │       ├── auth/      # Authentication logic
│   │   │   │       ├── drive/     # Google Drive integration
│   │   │   │       ├── recipe/    # Recipe business logic
│   │   │   │       ├── navigation/# App navigation
│   │   │   │       ├── util/      # Utilities (Logger, etc.)
│   │   │   │       ├── App.kt     # Main UI entry (iOS/Web)
│   │   │   │       ├── RecipeListScreen.kt
│   │   │   │       └── RecipeDetailScreen.kt
│   │   │   └── composeResources/  # Images, strings, etc.
│   │   ├── androidMain/           # Android-specific implementations
│   │   │   └── kotlin/net/shamansoft/kukbuk/
│   │   │       ├── auth/          # Android OAuth (Credential Manager)
│   │   │       ├── util/          # Android Logger
│   │   │       ├── AndroidApp.kt  # Android UI entry point ⚠️
│   │   │       ├── MainActivity.kt# Android Activity
│   │   │       └── Platform.android.kt
│   │   ├── iosMain/               # iOS-specific implementations
│   │   │   └── kotlin/net/shamansoft/kukbuk/
│   │   │       ├── auth/          # iOS OAuth (GoogleSignIn SDK)
│   │   │       ├── util/          # iOS Logger
│   │   │       └── MainViewController.kt
│   │   ├── wasmJsMain/            # Web-specific implementations
│   │   └── commonTest/            # Shared unit tests
│   └── build.gradle.kts           # Module build config
├── iosApp/                        # iOS app wrapper (Swift)
│   ├── iosApp.xcodeproj
│   └── GoogleSignIn-Setup.md
├── gradle/
│   └── libs.versions.toml         # Dependency versions
├── docs/
│   ├── phase-1.md                 # User stories
│   ├── us-5.md                    # Recipe detail implementation plan
│   └── OAUTH_SETUP.md             # Google OAuth setup guide
├── build.gradle.kts               # Root build config
└── settings.gradle.kts
```

---

## Architecture & Patterns

### MVVM (Model-View-ViewModel)

Similar to Spring MVC but UI-focused:

```
View (Composable)
  ↕
ViewModel (StateFlow)
  ↕
Repository (suspend functions)
  ↕
Service (HTTP/API)
```

### Key Patterns

1. **Repository Pattern**: `RecipeRepository`, `AuthenticationRepository`
   - Similar to Spring Data repositories
   - Handles data fetching, caching, state management

2. **Dependency Injection**: Manual (Factory functions)
   - `createRecipeListViewModel()` in `RecipeServiceFactory.kt`
   - No Dagger/Hilt/Koin (yet)

3. **Sealed Classes**: Type-safe state machines
   ```kotlin
   sealed class RecipeListState {
       object Loading : RecipeListState()
       data class Success(val recipes: List<Recipe>) : RecipeListState()
       data class Error(val message: String) : RecipeListState()
   }
   ```
   Similar to Java enums but can carry data

4. **Coroutines**: Async operations (like CompletableFuture)
   ```kotlin
   suspend fun loadRecipes() { ... }  // suspend = async method
   viewModelScope.launch { ... }      // launch coroutine (like async{})
   ```

5. **StateFlow**: Reactive state (like RxJava BehaviorSubject)
   ```kotlin
   private val _state = MutableStateFlow(Loading)
   val state: StateFlow = _state.asStateFlow()
   ```

---

## Package Guide

### 📦 `commonMain/kotlin/net/shamansoft/kukbuk/`

#### Root Package
| File | Purpose |
|------|---------|
| `App.kt` | Main UI entry point for iOS/Web |
| `RecipeListScreen.kt` | Recipe list UI (home screen) |
| `RecipeDetailScreen.kt` | Recipe detail UI |
| `Platform.kt` | Platform info interface |
| `Greeting.kt` | Legacy demo code (can be ignored) |

#### `auth/` - Authentication
| File | Purpose | Similar to |
|------|---------|-----------|
| `AuthenticationRepository.kt` | Manages auth state, token storage | Spring Security UserRepository |
| `AuthenticationService.kt` | Interface for platform-specific auth | Service interface |
| `AuthenticationState.kt` | Sealed class: Unauthenticated/Authenticated/Error | Enum with data |
| `AuthViewModel.kt` | UI state for login screen | ViewModel/Controller |
| `AuthenticationScreen.kt` | Login UI | Login JSP/Thymeleaf |
| `AuthServiceFactory.kt` | Creates platform-specific auth service | Factory pattern |

**Platform Implementations**:
- `androidMain/auth/AndroidAuthenticationService.kt` - Uses Credential Manager API
- `iosMain/auth/IOSAuthenticationService.kt` - Uses GoogleSignIn SDK

#### `drive/` - Google Drive Integration
| File | Purpose |
|------|---------|
| `GoogleDriveService.kt` | Interface for Drive operations |
| `HttpGoogleDriveService.kt` | HTTP implementation using Ktor |

**Key Operations**:
- `listFilesInKukbukFolder()` - Find all recipes in Drive
- `downloadFileContent(fileId)` - Download recipe YAML

#### `recipe/` - Recipe Business Logic
| File | Purpose | Similar to |
|------|---------|-----------|
| `Recipe.kt` | Data classes: `Recipe`, `RecipeMetadata`, `Ingredient`, `Instruction` | JPA Entity |
| `RecipeRepository.kt` | Loads recipes, manages cache, search | Spring Data Repository |
| `RecipeListViewModel.kt` | List screen state (loading/success/error) | Controller |
| `RecipeDetailViewModel.kt` | Detail screen state | Controller |
| `YamlRecipeParser.kt` | Parses YAML recipe files | Jackson ObjectMapper |
| `RecipeServiceFactory.kt` | Creates ViewModels | Factory/Builder |

**Key Classes**:
```kotlin
data class Recipe(
    val id: String,
    val title: String,
    val author: String?,
    val description: String?,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val tags: List<String>,
    val sourceUrl: String?,
    val imageUrl: String?,
    val notes: String?,
    val lastModified: String,
    val driveFileId: String
)

data class Ingredient(
    val name: String,
    val quantity: String?,
    val unit: String?
)

data class Instruction(
    val step: String,
    val order: Int
)
```

#### `navigation/` - App Navigation
| File | Purpose |
|------|---------|
| `Screen.kt` | Sealed class for navigation routes |

```kotlin
sealed class Screen {
    object RecipeList : Screen()
    data class RecipeDetail(
        val recipeId: String,
        val recipeTitle: String
    ) : Screen()
}
```

#### `util/` - Utilities
| File | Purpose |
|------|---------|
| `Logger.kt` | Platform-agnostic logging (uses `println` on Android, `NSLog` on iOS) |

---

### 📦 `androidMain/kotlin/net/shamansoft/kukbuk/`

**Android-Specific Implementations**

| File | Purpose | ⚠️ Important |
|------|---------|--------------|
| `MainActivity.kt` | Android Activity, app entry point | Calls `AndroidApp()` not `App()` |
| `AndroidApp.kt` | **Android UI root** - THIS is what Android users see | **Critical for Android** |
| `Platform.android.kt` | Android platform info | |
| `auth/AndroidAuthenticationService.kt` | Google Sign-In using Credential Manager | Uses modern Android API |
| `auth/AndroidSecureStorage.kt` | Token storage using DataStore | Like SharedPreferences but encrypted |
| `auth/AndroidAuthFactory.kt` | Creates Android auth service | |
| `util/Logger.android.kt` | Android `Log.d/e` implementation | |

**⚠️ CRITICAL**: When debugging Android, changes must be made in `AndroidApp.kt`, NOT `App.kt`!

---

### 📦 `iosMain/kotlin/net/shamansoft/kukbuk/`

| File | Purpose |
|------|---------|
| `MainViewController.kt` | iOS UI controller (bridges to Swift) |
| `auth/IOSAuthenticationService.kt` | Google Sign-In using GoogleSignIn pod |
| `auth/IOSSecureStorage.kt` | Token storage using NSUserDefaults |
| `util/Logger.ios.kt` | iOS `NSLog` implementation |

---

## Key Entry Points

### How the App Starts

#### Android
```
User launches app
  ↓
MainActivity.onCreate()
  ↓
setContent { AndroidApp() }  ← AndroidApp.kt (androidMain)
  ↓
AuthViewModel checks auth state
  ↓
If authenticated → RecipeListScreen
If not → AuthenticationScreen
```

**File**: `androidMain/kotlin/net/shamansoft/kukbuk/MainActivity.kt:34`

#### iOS
```
User launches app
  ↓
Swift iosApp (iosApp/iosApp/ContentView.swift)
  ↓
MainViewController (iosMain)
  ↓
App.kt (commonMain)
  ↓
AuthViewModel checks auth state
```

#### Web
```
Browser loads app
  ↓
main() function (wasmJsMain/main.kt)
  ↓
App.kt (commonMain)
```

---

## Data Flow

### Loading Recipes (Full Flow)

```
RecipeListScreen
  ↓
RecipeListViewModel.loadRecipes()
  ↓
RecipeRepository.loadRecipes()
  ↓
GoogleDriveService.listFilesInKukbukFolder()
  ↓
HttpGoogleDriveService (Ktor HTTP call)
  ↓
GET https://www.googleapis.com/drive/v3/files?q=...
  ↓
For each file:
  ↓
  GoogleDriveService.downloadFileContent(fileId)
  ↓
  GET https://www.googleapis.com/drive/v3/files/{fileId}?alt=media
  ↓
  YamlRecipeParser.parseRecipeMetadata(yamlContent)
  ↓
  Return RecipeMetadata
  ↓
RecipeRepository emits RecipeListState.Success(recipes)
  ↓
ViewModel.recipeListState StateFlow updates
  ↓
RecipeListScreen recomposes with new data
  ↓
LazyColumn displays recipe cards
```

**Key Files**:
1. `RecipeListScreen.kt:34` - `val recipeListState by viewModel.recipeListState.collectAsState()`
2. `RecipeListViewModel.kt:20` - `recipeRepository.loadRecipes()`
3. `RecipeRepository.kt:27` - `driveService.listFilesInKukbukFolder()`
4. `HttpGoogleDriveService.kt` - HTTP calls
5. `YamlRecipeParser.kt` - YAML parsing

### Viewing Recipe Detail

```
User taps recipe card
  ↓
RecipeListScreen.onRecipeClick(recipe) callback
  ↓
AndroidApp.kt: currentScreen = Screen.RecipeDetail(recipeId, title)
  ↓
AndroidApp when(screen) { is Screen.RecipeDetail → ... }
  ↓
RecipeDetailViewModel created (remember(recipeId) { ... })
  ↓
RecipeDetailViewModel.init { loadRecipe() }
  ↓
RecipeRepository.getRecipe(recipeId)
  ↓
Check cache → if miss, download from Drive
  ↓
YamlRecipeParser.parseRecipeYaml(yamlContent)
  ↓
Return Recipe
  ↓
ViewModel emits RecipeDetailState.Success(recipe)
  ↓
RecipeDetailScreen displays full recipe
```

**Key Files**:
1. `RecipeListScreen.kt:49` - `onRecipeClick = { recipe -> currentScreen = ... }`
2. `AndroidApp.kt:49-68` - Navigation logic
3. `RecipeDetailViewModel.kt:19` - `loadRecipe()`
4. `RecipeDetailScreen.kt:23` - UI rendering

---

## Authentication Flow

### Google Sign-In (Android)

```
User taps "Sign in with Google"
  ↓
AuthenticationScreen.kt:90 - authViewModel.signIn()
  ↓
AuthViewModel.kt:29 - authRepository.signIn()
  ↓
AuthenticationRepository.kt:37 - authService.signIn()
  ↓
AndroidAuthenticationService.kt:40 - Credential Manager API
  ↓
Shows Google account picker (system UI)
  ↓
User selects account
  ↓
Returns Google ID token
  ↓
AndroidAuthenticationService.exchangeGoogleToken(idToken)
  ↓
POST https://oauth2.googleapis.com/token (exchange for refresh token)
  ↓
AndroidSecureStorage.saveRefreshToken(token) - Saves to DataStore
  ↓
AuthenticationRepository emits AuthenticationState.Authenticated(user)
  ↓
AndroidApp.kt:37 - when(authState) { is Authenticated → RecipeListScreen }
```

**Configuration**:
- Android: `composeApp/src/androidMain/res/values/strings.xml` - `google_web_client_id`
- Setup docs: `docs/OAUTH_SETUP.md`

---

## Build & Run

### Gradle Commands

```bash
# Build everything
./gradlew build

# Android
./gradlew composeApp:assembleDebug          # Build APK
./gradlew composeApp:installDebug           # Install on device
adb logcat -s "YourTag:D"                   # View logs

# iOS (requires macOS + Xcode)
./gradlew composeApp:embedAndSignAppleFrameworkForXcode
# Then open iosApp/iosApp.xcodeproj in Xcode and run

# Web
./gradlew composeApp:wasmJsBrowserDevelopmentRun  # Runs dev server
# Open http://localhost:8080

# Tests
./gradlew test                              # All tests
./gradlew composeApp:testDebugUnitTest      # Android unit tests
```

### IDE Setup

**Recommended**: IntelliJ IDEA or Android Studio

1. Open project: `File → Open → sar-kmp/`
2. Gradle sync runs automatically
3. Select run configuration:
   - Android: `composeApp` (Android)
   - iOS: Use Xcode
   - Web: `composeApp (wasmJsBrowserDevelopmentRun)`

---

## Common Tasks

### Adding a New Screen

1. **Create UI file** in `commonMain/`:
   ```kotlin
   // RecipeEditScreen.kt
   @Composable
   fun RecipeEditScreen(
       recipeId: String,
       onNavigateBack: () -> Unit,
       viewModel: RecipeEditViewModel
   ) { ... }
   ```

2. **Add to navigation** in `navigation/Screen.kt`:
   ```kotlin
   sealed class Screen {
       object RecipeList : Screen()
       data class RecipeDetail(...) : Screen()
       data class RecipeEdit(val recipeId: String) : Screen()  // NEW
   }
   ```

3. **Update AndroidApp.kt** (for Android):
   ```kotlin
   when (val screen = currentScreen) {
       Screen.RecipeList -> { ... }
       is Screen.RecipeDetail -> { ... }
       is Screen.RecipeEdit -> {  // NEW
           RecipeEditScreen(
               recipeId = screen.recipeId,
               onNavigateBack = { currentScreen = Screen.RecipeList },
               viewModel = remember(screen.recipeId) { ... }
           )
       }
   }
   ```

4. **Update App.kt** (for iOS/Web) - same structure

### Adding a Repository Method

```kotlin
// RecipeRepository.kt
suspend fun deleteRecipe(recipeId: String): RecipeResult<Unit> {
    return when (val result = driveService.deleteFile(recipeId)) {
        is DriveResult.Success -> {
            _recipeCache.remove(recipeId)
            refreshRecipes()  // Reload list
            RecipeResult.Success(Unit)
        }
        is DriveResult.Error -> RecipeResult.Error(result.message)
        is DriveResult.Loading -> RecipeResult.Loading
    }
}
```

### Adding a ViewModel

```kotlin
// RecipeEditViewModel.kt (commonMain/recipe/)
class RecipeEditViewModel(
    private val recipeId: String,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            when (val result = recipeRepository.updateRecipe(recipe)) {
                is RecipeResult.Success -> {
                    _saveState.value = SaveState.Success
                }
                is RecipeResult.Error -> {
                    _saveState.value = SaveState.Error(result.message)
                }
            }
        }
    }
}

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
```

### Logging

```kotlin
import net.shamansoft.kukbuk.util.Logger

Logger.d("MyTag", "Debug message: $variable")
Logger.e("MyTag", "Error: ${exception.message}")
Logger.w("MyTag", "Warning!")
Logger.i("MyTag", "Info")
```

Platform implementations:
- Android: Uses `android.util.Log`
- iOS: Uses `NSLog`
- Web: Uses `console.log`

### Debugging Android

```bash
# Install debug build
./gradlew composeApp:installDebug

# Clear logcat and filter
adb logcat -c
adb logcat -s "RecipeRepo:D" "RecipeDetailVM:D" "YamlParser:D" "*:E"

# Alternative: Android Studio Logcat (bottom panel)
```

### Testing Changes

```bash
# 1. Make changes in commonMain/ or androidMain/

# 2. Build
./gradlew composeApp:assembleDebug --quiet

# 3. Install
./gradlew composeApp:installDebug

# 4. Check logs
adb logcat -s "YourTag:D"
```

---

## Important Concepts for Java Developers

### Kotlin vs Java Quick Reference

| Kotlin | Java Equivalent | Notes |
|--------|----------------|-------|
| `val` | `final` variable | Immutable |
| `var` | Regular variable | Mutable |
| `data class Recipe(val title: String)` | POJO with equals/hashCode/toString | Auto-generated |
| `fun loadRecipes()` | `public void loadRecipes()` | `public` by default |
| `suspend fun load()` | `CompletableFuture<T> load()` | Coroutine function |
| `sealed class State` | `enum` with data | Type-safe state machine |
| `Recipe?` | `@Nullable Recipe` | Nullable type |
| `recipe!!.title` | Assumes non-null, throws if null | Avoid in production |
| `recipe?.title` | Safe call, returns null if recipe is null | |
| `recipe?.title ?: "Unknown"` | Elvis operator (null coalescing) | |
| `when (x) { ... }` | `switch` statement | More powerful |
| `if (x) y else z` | Ternary `x ? y : z` | `if` is an expression |

### Coroutines (like CompletableFuture)

```kotlin
// Kotlin
suspend fun loadRecipes(): List<Recipe> {
    val files = driveService.listFiles()  // suspend call
    return files.map { parseRecipe(it) }
}

// Launch coroutine
viewModelScope.launch {
    val recipes = loadRecipes()  // suspends, doesn't block
    _state.value = Success(recipes)
}
```

```java
// Java equivalent
CompletableFuture<List<Recipe>> loadRecipes() {
    return driveService.listFiles()
        .thenApply(files ->
            files.stream()
                .map(this::parseRecipe)
                .collect(Collectors.toList())
        );
}
```

### StateFlow (like BehaviorSubject)

```kotlin
// Kotlin
private val _state = MutableStateFlow<State>(Loading)
val state: StateFlow<State> = _state.asStateFlow()

_state.value = Success(data)  // Emit new value
```

```java
// RxJava equivalent
private BehaviorSubject<State> stateSubject =
    BehaviorSubject.createDefault(new Loading());
public Observable<State> state = stateSubject.hide();

stateSubject.onNext(new Success(data));
```

### Compose UI (Declarative)

```kotlin
@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column {
            Text(text = recipe.title, fontSize = 18.sp)
            Text(text = recipe.author ?: "Unknown")
        }
    }
}
```

Similar to React JSX or Android XML layouts, but:
- Written in Kotlin (type-safe)
- Recomposes when state changes (reactive)
- No XML files

### expect/actual (Platform-Specific Code)

```kotlin
// commonMain/Platform.kt
expect fun getPlatformName(): String

// androidMain/Platform.android.kt
actual fun getPlatformName(): String = "Android"

// iosMain/Platform.ios.kt
actual fun getPlatformName(): String = "iOS"
```

Allows shared interface with platform-specific implementations.

---

## Troubleshooting

### "Nothing happens when I click"

**Likely cause**: Wrong entry point file

- Android uses `AndroidApp.kt`, NOT `App.kt`
- Check `MainActivity.kt` to see which Composable it calls
- Verify callback is not empty lambda: `onClick = { /* TODO */ }`

### "Build failed: Unresolved reference"

**Likely cause**: Missing dependency or source set issue

```bash
# Sync Gradle
./gradlew --refresh-dependencies

# Clean build
./gradlew clean build
```

### "Authentication fails"

**Check**:
1. `google_web_client_id` in `strings.xml` (Android)
2. OAuth consent screen configured in Google Cloud Console
3. SHA-1 fingerprint registered (Android)
4. Logs: `adb logcat -s "AndroidAuth:D" "AuthRepo:D"`

### "Recipe not loading"

**Check**:
1. Google Drive permissions (needs Drive scope)
2. Recipe folder exists in Drive (named "kukbuk" or configured name)
3. YAML format valid
4. Logs: `adb logcat -s "RecipeRepo:D" "YamlParser:D" "HttpGoogleDrive:D"`

---

## File Locations Quick Reference

| What | Where |
|------|-------|
| **Android app entry** | `androidMain/AndroidApp.kt` |
| **Shared UI** | `commonMain/RecipeListScreen.kt`, `RecipeDetailScreen.kt` |
| **Recipe logic** | `commonMain/recipe/RecipeRepository.kt` |
| **Auth logic** | `commonMain/auth/AuthenticationRepository.kt` |
| **Google Drive** | `commonMain/drive/HttpGoogleDriveService.kt` |
| **Navigation** | `commonMain/navigation/Screen.kt` |
| **Android auth** | `androidMain/auth/AndroidAuthenticationService.kt` |
| **Data models** | `commonMain/recipe/Recipe.kt` |
| **YAML parsing** | `commonMain/recipe/YamlRecipeParser.kt` |
| **Dependencies** | `gradle/libs.versions.toml` |
| **User stories** | `docs/phase-1.md` |

---

## Next Steps

1. **Read User Stories**: `docs/phase-1.md` - understand product requirements
2. **Run the app**: `./gradlew composeApp:installDebug`
3. **Explore code**: Start with `AndroidApp.kt` → `RecipeListScreen.kt` → `RecipeRepository.kt`
4. **Make a change**: Add a log statement, rebuild, see it in logcat
5. **Read existing docs**: `docs/OAUTH_SETUP.md`, `docs/us-5.md`

---

## Resources

- **Kotlin Docs**: https://kotlinlang.org/docs/home.html
- **Compose Multiplatform**: https://www.jetbrains.com/lp/compose-multiplatform/
- **Coroutines Guide**: https://kotlinlang.org/docs/coroutines-guide.html
- **Ktor (HTTP)**: https://ktor.io/docs/client.html

---

**Questions?** Check existing code comments or ask the team!
