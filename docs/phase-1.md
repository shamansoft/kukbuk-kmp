# MyKukBuk Mobile App - Phase 1 User Stories

## Epic: MVP Mobile Recipe Viewer
**Goal**: Launch a basic mobile app that allows users to authenticate and view their saved recipes from the Chrome extension.

---

## Authentication Stories

### US-1: Google Account Login
**As a user**, I want to log in with my Google account so that I can access my saved recipes.

**Acceptance Criteria:**
- [x] User can tap "Sign in with Google" button
- [x] Google OAuth flow opens in system browser/webview
- [x] User grants permissions for Google Drive access
- [x] App receives and stores auth token securely
- [x] User is redirected to recipe list after successful login
- [x] Login state persists between app sessions

**Technical Tasks:**
- [x] Implement Google OAuth 2.0 for Android using Google Sign-In SDK
- [x] Implement Google OAuth 2.0 for iOS using GoogleSignIn SDK
- [x] Create shared authentication interface in common code
- [x] Set up secure token storage (Android Keystore / iOS Keychain)
- [x] Configure OAuth client IDs for both platforms
- [x] Add Google Drive scope permissions

**Definition of Done:**
- User can successfully authenticate on both iOS and Android
- Auth tokens are stored securely
- Login state persists across app restarts

---

### US-2: Authentication State Management
**As a user**, I want the app to remember my login status so that I don't have to sign in every time.

**Acceptance Criteria:**
- [x] App checks authentication status on startup
- [x] Valid tokens automatically log user in
- [x] Expired tokens prompt for re-authentication
- [x] User can manually log out from settings
- [x] Clear feedback when authentication fails

**Technical Tasks:**
- [x] Implement token validation logic
- [x] Create authentication state management in shared code
- [x] Add token refresh mechanism
- [x] Implement logout functionality
- [x] Add loading states for auth checks

**Definition of Done:**
- App maintains login state correctly
- Token refresh works automatically
- Logout clears all stored credentials

---

## Recipe Discovery Stories

### US-3: View My Recipe Collection
**As a logged-in user**, I want to see a list of my saved recipes so that I can browse what I have available.

**Acceptance Criteria:**
- [x] App displays all YAML files from Google Drive's "kukbuk" folder
- [x] Each recipe shows title, author (if available), and thumbnail
- [x] List loads efficiently even with many recipes
- [x] Pull-to-refresh syncs with latest Google Drive contents
- [x] Empty state shows helpful message for new users
- [x] Error handling for network/Drive access issues

**Technical Tasks:**
- [x] Integrate with Google Drive API to list files in kukbuk folder
- [x] Parse YAML metadata to extract recipe titles and basic info
- [x] Implement repository pattern for recipe data
- [x] Create RecipeListScreen with Compose UI
- [x] Add pull-to-refresh functionality
- [x] Implement proper error handling and loading states
- [x] Add empty state UI

**Definition of Done:**
- Recipe list displays all saved recipes from Chrome extension
- Smooth scrolling performance with 100+ recipes
- Pull-to-refresh works correctly
- Proper error handling for network issues

---

### US-3a: Recipe Thumbnail Images
**As a user**, I want to see recipe thumbnail images in the list so that I can quickly identify recipes visually.

**Acceptance Criteria:**
- [ ] Recipe cards display thumbnail images from Google Drive if available
- [ ] Placeholder emoji (🍽️) shows for recipes without images
- [ ] Images load without blocking the list display
- [ ] Images are cached locally to reduce network usage
- [ ] Image loading errors degrade gracefully to placeholder
- [ ] Images are properly sized and don't distort aspect ratio
- [ ] Cached images expire after 7 days to stay fresh

**Technical Tasks:**
- [ ] Integrate Coil or Kamel image loading library for Compose Multiplatform
- [ ] Implement image URL extraction from YAML recipe metadata
- [ ] Create image caching strategy with expiration
- [ ] Handle image loading states (loading, success, error)
- [ ] Add image resize/compression for performance
- [ ] Implement platform-specific cache directory handling
- [ ] Add network status awareness (don't load images over cellular if disabled)

**Definition of Done:**
- Recipe thumbnails display correctly on both iOS and Android
- Image loading is non-blocking and doesn't slow list scrolling
- Proper fallback to placeholder for missing images
- Cache is managed and doesn't consume excessive storage

---

### US-4: Search My Recipes
**As a user with many recipes**, I want to search and filter my collection so that I can quickly find specific recipes.

**Acceptance Criteria:**
- [ ] Search bar at top of recipe list
- [ ] Real-time search as user types
- [ ] Search works on recipe title, author, and tags
- [ ] Clear search button to reset results
- [ ] Search results highlight matching terms
- [ ] Search works offline with cached data

**Technical Tasks:**
- [ ] Add search TextField to RecipeListScreen
- [ ] Implement search logic in repository layer
- [ ] Create search filtering for recipe metadata
- [ ] Add search state management
- [ ] Implement search result highlighting
- [ ] Add debouncing for search input

**Definition of Done:**
- Fast, responsive search functionality
- Search works across all recipe metadata fields
- Results update in real-time as user types

---

## Recipe Viewing Stories

### US-5: View Recipe Details
**As a user**, I want to tap on a recipe to see its full details so that I can cook from it.

**Acceptance Criteria:**
- [ ] Tapping recipe from list opens detail view
- [ ] Recipe detail shows all information: title, description, ingredients, instructions
- [ ] Instructions are clearly numbered and easy to follow
- [ ] Ingredients show amounts and units clearly
- [ ] Back button returns to recipe list
- [ ] Recipe loads quickly and handles long content well

**Technical Tasks:**
- [ ] Create RecipeDetailScreen with full recipe layout
- [ ] Implement YAML parsing for complete recipe structure
- [ ] Design readable typography for cooking instructions
- [ ] Add navigation between list and detail screens
- [ ] Handle recipe images if present
- [ ] Optimize for different screen sizes

**Definition of Done:**
- Recipe details display all information clearly
- Easy navigation between recipes and back to list
- Readable on both phone and tablet screen sizes

---

### US-6: Cooking-Friendly Display
**As a user cooking from a recipe**, I want the text to be large and clear so that I can read it while cooking.

**Acceptance Criteria:**
- [ ] Large, readable font sizes for ingredients and instructions
- [ ] High contrast text for kitchen lighting conditions
- [ ] Ingredients and instructions clearly separated
- [ ] Portrait and landscape orientations both work well
- [ ] Screen stays awake while viewing recipe
- [ ] Easy scrolling even with wet/dirty hands

**Technical Tasks:**
- [ ] Design cooking-optimized typography scale
- [ ] Implement high contrast color scheme
- [ ] Add orientation handling
- [ ] Implement screen wake-lock functionality
- [ ] Create touch-friendly UI elements
- [ ] Test accessibility features

**Definition of Done:**
- Recipe is easily readable in kitchen environment
- Works well in both orientations
- Screen doesn't auto-lock during cooking

---

## Data & Performance Stories

### US-7: Offline Recipe Access
**As a user**, I want to access my recently viewed recipes even without internet so that I can cook anywhere.

**Acceptance Criteria:**
- [ ] Recently viewed recipes cached locally
- [ ] Cache at least 10 most recent recipes
- [ ] Clear indicator when viewing cached vs live data
- [ ] Cached recipes include all text content
- [ ] Cache updates when connected to internet

**Technical Tasks:**
- [ ] Implement local database with SQLDelight
- [ ] Create caching strategy for recipe content
- [ ] Add cache management (size limits, expiration)
- [ ] Implement offline indicators in UI
- [ ] Add cache sync logic

**Definition of Done:**
- Recently viewed recipes work offline
- Cache management prevents storage issues
- Clear offline/online status indicators

---

### US-8: App Performance
**As a user**, I want the app to load quickly and respond smoothly so that I have a good cooking experience.

**Acceptance Criteria:**
- [ ] App launches in under 2 seconds
- [ ] Recipe list loads in under 3 seconds
- [ ] Smooth scrolling with no lag
- [ ] Recipe details open instantly for cached recipes
- [ ] No crashes or freezing during normal use

**Technical Tasks:**
- [ ] Optimize app startup time
- [ ] Implement lazy loading for recipe list
- [ ] Add image loading optimization
- [ ] Profile and optimize performance bottlenecks
- [ ] Add proper memory management
- [ ] Implement background data sync

**Definition of Done:**
- App meets performance benchmarks
- Smooth user experience on mid-range devices
- Stable performance with large recipe collections

---

## Platform-Specific Stories

### US-9: iOS Integration
**As an iOS user**, I want the app to feel native to iOS so that it integrates well with my device.

**Acceptance Criteria:**
- [ ] App follows iOS Human Interface Guidelines
- [ ] Proper iOS navigation patterns (swipe back, etc.)
- [ ] Integrates with iOS sharing system
- [ ] Supports iOS dark/light mode
- [ ] Works with iOS accessibility features

**Technical Tasks:**
- [ ] Implement iOS-specific navigation
- [ ] Add iOS sharing integration
- [ ] Test with iOS accessibility tools
- [ ] Optimize for iOS-specific UI patterns
- [ ] Add iOS app store compliance

---

### US-10: Android Integration
**As an Android user**, I want the app to feel native to Android so that it follows familiar patterns.

**Acceptance Criteria:**
- [ ] App follows Material Design guidelines
- [ ] Proper Android navigation (back button, up navigation)
- [ ] Integrates with Android sharing system
- [ ] Supports Android themes and dark mode
- [ ] Works with Android accessibility services

**Technical Tasks:**
- [ ] Implement Material 3 design system
- [ ] Add Android-specific navigation handling
- [ ] Integrate with Android sharing
- [ ] Test with TalkBack and other accessibility services
- [ ] Add Android-specific optimizations

---

## Technical Foundation Stories

### US-11: Project Setup
**As a developer**, I want a properly configured Kotlin Multiplatform project so that I can efficiently develop for both platforms.

**Technical Tasks:**
- [ ] Set up Kotlin Multiplatform Mobile project structure
- [ ] Configure Compose Multiplatform
- [ ] Set up shared and platform-specific modules
- [ ] Configure build scripts for both platforms
- [ ] Set up development environment documentation
- [ ] Configure version control and CI/CD basics

### US-12: Backend Integration
**As a developer**, I want to integrate with the existing MyKukBuk backend so that mobile and web users share the same data.

**Technical Tasks:**
- [ ] Set up HTTP client (Ktor) for API communication
- [ ] Implement authentication flow with existing backend
- [ ] Create API service interfaces
- [ ] Add proper error handling for network requests
- [ ] Implement retry logic for failed requests
- [ ] Set up request/response logging for debugging

---

## Sprint Planning Suggestion

**Sprint 1 (2 weeks)**: US-11, US-12, US-1 (Project setup and basic auth)
**Sprint 2 (2 weeks)**: US-2, US-3 (Auth state management and recipe list)
**Sprint 2b (1 week)**: US-3a (Recipe thumbnail images)
**Sprint 3 (2 weeks)**: US-5, US-6 (Recipe detail viewing)
**Sprint 4 (2 weeks)**: US-4, US-7 (Search and basic offline support)
**Sprint 5 (2 weeks)**: US-8, US-9, US-10 (Performance and platform polish)

**Total Estimated Timeline**: 11 weeks for Phase 1 MVP