# Phase: Web/WASM Platform Support

## Overview
This document contains all Web/WASM-specific implementation tasks that will be addressed in a future phase after Android and iOS platforms are stable.

**Status**: 🔮 Future Phase - Not Started
**Dependencies**: Complete Android and iOS implementation first
**Estimated Effort**: 8-12 hours

---

## Web Platform Architecture

### Current Status
- ✅ Basic Compose Multiplatform Web support configured
- ✅ Web build targets defined
- ⚠️ No offline caching for Web yet
- ⚠️ Authentication flows need Web-specific testing

### Target Status
- Full offline recipe caching using IndexedDB or SQLDelight Web Worker
- Web-optimized authentication flows
- Progressive Web App (PWA) capabilities
- Web-specific UI optimizations

---

## US-7: Offline Recipe Access - Web Implementation

### Web Storage Options

#### Option 1: SQLDelight Web Worker Driver (Recommended)
Uses SQL.js compiled to JavaScript/WASM for SQLite in the browser.

**Pros:**
- Consistent API with Android/iOS
- Shared cache logic across all platforms
- Type-safe queries

**Cons:**
- Requires Web Worker setup
- Larger initial bundle size (~1-2MB for SQL.js)
- More complex configuration

#### Option 2: IndexedDB Direct
Native browser storage API for structured data.

**Pros:**
- No additional dependencies
- Native browser support
- Smaller bundle size

**Cons:**
- Different API from mobile platforms
- Manual implementation needed
- Less mature KMP libraries

#### Option 3: localStorage + JSON
Simple key-value storage with JSON serialization.

**Pros:**
- Simplest implementation
- No dependencies

**Cons:**
- Storage limits (5-10MB typically)
- No query capabilities
- Synchronous API (blocks main thread)

### Recommended: SQLDelight Web Worker Driver

---

## Web-Specific Implementation Tasks

### Phase W.1: SQLDelight Web Driver Setup

#### Dependencies
**File**: `gradle/libs.versions.toml`
```toml
[libraries]
sqldelight-web-driver = { module = "app.cash.sqldelight:web-worker-driver", version.ref = "sqldelight" }
```

#### Build Configuration
**File**: `composeApp/build.gradle.kts`
```kotlin
kotlin {
    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.web.driver)
        }
    }
}
```

#### Web Worker Driver Implementation
**File**: `composeApp/src/wasmJsMain/kotlin/net/shamansoft/kukbuk/db/DatabaseDriverFactory.wasmJs.kt`
```kotlin
package net.shamansoft.kukbuk.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            Worker(
                js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
            )
        ).also {
            RecipeDatabase.Schema.create(it)
        }
    }
}
```

#### Koin DI Configuration
**File**: `composeApp/src/wasmJsMain/kotlin/net/shamansoft/kukbuk/di/PlatformModule.wasmJs.kt`
```kotlin
actual val platformModule = module {
    single { DatabaseDriverFactory() }
}
```

### Phase W.2: Web-Specific Cache Optimizations

#### Service Worker for Offline Support
**File**: `composeApp/src/wasmJsMain/resources/service-worker.js`
```javascript
// Service worker for offline support and caching strategies
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open('kukbuk-v1').then((cache) => {
            return cache.addAll([
                '/',
                '/index.html',
                '/skiko.js',
                '/composeApp.js'
            ]);
        })
    );
});

self.addEventListener('fetch', (event) => {
    event.respondWith(
        caches.match(event.request).then((response) => {
            return response || fetch(event.request);
        })
    );
});
```

#### PWA Manifest
**File**: `composeApp/src/wasmJsMain/resources/manifest.json`
```json
{
    "name": "Kukbuk Recipe Manager",
    "short_name": "Kukbuk",
    "description": "Manage and view your recipes offline",
    "start_url": "/",
    "display": "standalone",
    "background_color": "#ffffff",
    "theme_color": "#6200ee",
    "icons": [
        {
            "src": "/icon-192.png",
            "sizes": "192x192",
            "type": "image/png"
        },
        {
            "src": "/icon-512.png",
            "sizes": "512x512",
            "type": "image/png"
        }
    ]
}
```

### Phase W.3: Web Authentication Enhancements

#### Google Sign-In for Web
**File**: `composeApp/src/wasmJsMain/kotlin/net/shamansoft/kukbuk/auth/WebAuthenticationService.kt`
```kotlin
// Web-specific authentication implementation
// May require different approach than mobile (e.g., Google Sign-In JavaScript library)

actual class AuthenticationService : BaseAuthenticationService() {

    override suspend fun signInWithGoogle(): AuthResult {
        // Implement using Google Sign-In JavaScript library
        // or redirect-based OAuth flow
        TODO("Implement Web-specific Google Sign-In")
    }

    // ... other methods
}
```

#### Secure Storage for Web
**File**: `composeApp/src/wasmJsMain/kotlin/net/shamansoft/kukbuk/auth/WebSecureStorage.kt`
```kotlin
// Consider using:
// - IndexedDB for tokens (more secure than localStorage)
// - Encrypted storage wrapper
// - Session storage for sensitive data

class WasmSecureStorage : SecureStorage {
    override suspend fun storeTokens(tokens: AuthTokens) {
        // Use IndexedDB or encrypted localStorage
        TODO("Implement Web secure token storage")
    }

    // ... other methods
}
```

### Phase W.4: Web UI Optimizations

#### Responsive Web Layout
- Adjust layouts for desktop screen sizes
- Optimize touch/mouse interactions
- Add keyboard shortcuts for navigation
- Implement web-specific navigation patterns (browser back button)

#### Web-Specific Components
```kotlin
@Composable
fun WebRecipeListScreen() {
    // Desktop-optimized layout
    // - Wider cards
    // - Grid layout for larger screens
    // - Sidebar navigation
}

@Composable
fun WebRecipeDetailScreen() {
    // Multi-column layout for desktop
    // - Ingredients in sidebar
    // - Instructions in main area
    // - Image gallery
}
```

### Phase W.5: Performance Optimizations for Web

#### Code Splitting
- Lazy load recipe detail screen
- Split authentication flows
- Optimize initial bundle size

#### Image Optimization
- Implement progressive image loading
- Use WebP format when supported
- Lazy load images outside viewport

#### Caching Strategy
- Implement aggressive caching for static assets
- Cache API responses in IndexedDB
- Use Cache API for offline support

---

## Testing Strategy for Web

### Browser Testing Matrix

| Browser | Version | Priority | Status |
|---------|---------|----------|--------|
| Chrome | Latest | High | 🔮 Pending |
| Firefox | Latest | High | 🔮 Pending |
| Safari | Latest | Medium | 🔮 Pending |
| Edge | Latest | Medium | 🔮 Pending |

### Test Scenarios

1. **Offline Functionality**
   - Load app offline
   - View cached recipes
   - Handle network recovery

2. **Storage Limits**
   - Test with 100+ cached recipes
   - Monitor storage quota
   - Handle storage full errors

3. **Performance**
   - Initial load time < 3 seconds
   - Time to interactive < 5 seconds
   - Recipe detail load < 1 second (cached)

4. **PWA Features**
   - Install prompt
   - Offline indicator
   - App updates

---

## Web Deployment Considerations

### Hosting Requirements
- Static file hosting (Netlify, Vercel, GitHub Pages)
- HTTPS required for service workers
- CORS configuration for API calls
- CDN for static assets

### Build Configuration
```kotlin
// build.gradle.kts
wasmJs {
    browser {
        commonWebpackConfig {
            outputFileName = "composeApp.js"
        }
    }
    binaries.executable()
}
```

### Environment Configuration
- Web-specific OAuth redirect URIs
- API endpoint configuration
- Feature flags for Web-specific features

---

## Migration Path from Mobile

### Differences to Address

1. **Storage APIs**
   - Mobile: Native SQLite
   - Web: SQL.js (SQLite in WASM) or IndexedDB

2. **Authentication**
   - Mobile: Native SDK flows
   - Web: JavaScript-based or redirect flows

3. **File System Access**
   - Mobile: Direct file system access
   - Web: Restricted, use File System Access API or downloads

4. **Background Sync**
   - Mobile: Background services
   - Web: Service Worker background sync

### Code Sharing Strategy

**Shared (commonMain):**
- Business logic
- Data models
- Repository interfaces
- ViewModels

**Platform-Specific (wasmJsMain):**
- Storage implementation
- Authentication flows
- File handling
- Platform-specific UI optimizations

---

## Security Considerations for Web

### Concerns
1. **Token Storage**: localStorage is vulnerable to XSS
2. **CORS**: Proper configuration needed for API calls
3. **CSP**: Content Security Policy for XSS protection
4. **HTTPS**: Required for service workers and secure features

### Mitigations
1. Use HttpOnly cookies where possible
2. Implement CSRF protection
3. Sanitize all user input
4. Use SubResource Integrity (SRI) for external scripts
5. Implement rate limiting on API calls

---

## Future Enhancements (Web-Specific)

### Phase W.6: Advanced Web Features

1. **Share API Integration**
   ```kotlin
   @Composable
   fun ShareButton(recipe: Recipe) {
       // Use Web Share API to share recipes
       // Fallback to clipboard copy
   }
   ```

2. **Print Optimization**
   - Print-friendly recipe layout
   - CSS print styles
   - PDF export

3. **Keyboard Shortcuts**
   - Cmd/Ctrl + F for search
   - Arrow keys for navigation
   - Escape to close modals

4. **Accessibility Enhancements**
   - ARIA labels
   - Keyboard navigation
   - Screen reader optimization
   - High contrast mode

5. **Browser Notifications**
   - Cooking timers
   - New recipe alerts
   - Sync completion notifications

---

## Success Criteria for Web Phase

### Functional Requirements
- [ ] Offline caching works in all major browsers
- [ ] Authentication flows work on Web
- [ ] Recipes display correctly on desktop and mobile browsers
- [ ] Service worker caches app for offline use
- [ ] Data syncs properly between Web and mobile

### Performance Requirements
- [ ] Initial load < 3 seconds on 3G
- [ ] Time to interactive < 5 seconds
- [ ] Recipe detail loads < 500ms (cached)
- [ ] Smooth 60fps scrolling

### Browser Support
- [ ] Chrome/Edge (latest)
- [ ] Firefox (latest)
- [ ] Safari (latest)
- [ ] Mobile browsers (iOS Safari, Chrome Mobile)

---

## Timeline Estimate

| Phase | Description | Estimated Time |
|-------|-------------|----------------|
| **W.1** | SQLDelight Web Driver Setup | 2-3 hours |
| **W.2** | Web Cache Optimizations | 2-3 hours |
| **W.3** | Web Authentication | 2-3 hours |
| **W.4** | Web UI Optimizations | 2-3 hours |
| **W.5** | Performance Optimizations | 1-2 hours |
| **Testing** | Cross-browser testing | 2-3 hours |
| **Total** | | **11-17 hours** |

---

## Dependencies

**Must Complete First:**
1. ✅ US-1, US-2: Authentication (Android/iOS)
2. ✅ US-3, US-4: Recipe list and search (Android/iOS)
3. ✅ US-5: Recipe detail (Android/iOS)
4. 🔄 US-7: Offline caching (Android/iOS) - **In Progress**

**Can Start After:**
- Android and iOS implementations are stable
- Core features tested and working
- Database schema finalized

---

## Notes

- Web platform is lower priority than Android/iOS
- Focus on mobile-first, enhance for Web later
- Consider Progressive Web App (PWA) capabilities
- Web implementation should maintain feature parity with mobile
- Some features may need Web-specific alternatives (e.g., camera access, file system)

---

**Document Version**: 1.0
**Last Updated**: 2025-10-28
**Status**: 🔮 Future Phase Planning
