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

### US-4: Search My Recipes ✅ COMPLETED
**As a user with many recipes**, I want to search and filter my collection so that I can quickly find specific recipes.

**Acceptance Criteria:**
- [x] Search bar at top of recipe list
- [x] Real-time search as user types
- [x] Search works on recipe title, author, and description
- [x] Clear search button to reset results
- [ ] Search results highlight matching terms (nice-to-have, not critical)
- [x] Search works offline with cached data

**Technical Tasks:**
- [x] Add search TextField to RecipeListScreen
- [x] Implement search logic in repository layer (RecipeRepository.searchRecipes)
- [x] Create search filtering for recipe metadata
- [x] Add search state management in RecipeListViewModel
- [ ] Implement search result highlighting (deferred)
- [ ] Add debouncing for search input (deferred, not critical)

**Definition of Done:**
- [x] Fast, responsive search functionality
- [x] Search works across title, author, description fields
- [x] Results update in real-time as user types
- [x] Toggle search UI with search icon button
- [x] Empty search state shows helpful message

**Implementation Files:**
- `RecipeListScreen.kt` - Search UI (SearchBar, toggle button)
- `RecipeListViewModel.kt` - Search state management
- `RecipeRepository.kt` - searchRecipes() method

---

## Recipe Viewing Stories

### US-5: View Recipe Details ✅ COMPLETED
**As a user**, I want to tap on a recipe to see its full details so that I can cook from it.

**Acceptance Criteria:**
- [x] Tapping recipe from list opens detail view
- [x] Recipe detail shows all information: title, description, ingredients, instructions
- [x] Instructions are clearly numbered and easy to follow
- [x] Ingredients show amounts and units clearly
- [x] Back button returns to recipe list
- [x] Recipe loads quickly and handles long content well

**Technical Tasks:**
- [x] Create RecipeDetailScreen with full recipe layout
- [x] Implement YAML parsing for complete recipe structure
- [x] Design readable typography for cooking instructions
- [x] Add navigation between list and detail screens
- [x] Optimize for different screen sizes
- [ ] Handle recipe images if present (moved to US-5a)

**Definition of Done:**
- [x] Recipe details display all information clearly
- [x] Easy navigation between recipes and back to list
- [x] Readable on both phone and tablet screen sizes

**Implementation Files:**
- `navigation/Screen.kt` - Navigation state management
- `recipe/RecipeDetailViewModel.kt` - Detail screen state
- `RecipeDetailScreen.kt` - Full recipe UI
- `AndroidApp.kt` & `App.kt` - Navigation integration

---

### US-5a: Recipe Detail Images
**As a user**, I want to see recipe images in the detail view so that I can visually identify the dish.

**Acceptance Criteria:**
- [ ] Recipe detail displays hero image from YAML metadata if available
- [ ] Images load asynchronously without blocking UI
- [ ] Placeholder shows while image is loading
- [ ] Fallback gradient + emoji shows for recipes without images
- [ ] Images are cached locally to reduce network usage
- [ ] Image loading errors degrade gracefully to placeholder
- [ ] Images scale properly on different screen sizes
- [ ] Images maintain aspect ratio (no distortion)

**Technical Tasks:**
- [ ] Integrate Coil or Kamel image loading library for Compose Multiplatform
- [ ] Update RecipeDetailScreen to use AsyncImage component
- [ ] Implement image caching strategy
- [ ] Handle image loading states (loading, success, error)
- [ ] Add image resize/compression for performance
- [ ] Test image loading on slow network conditions
- [ ] Add network status awareness (cellular vs wifi)

**Definition of Done:**
- Recipe images display correctly in detail view
- Image loading is non-blocking and smooth
- Proper fallback for missing/broken images
- Cache is managed efficiently

**Dependencies:**
- Builds on US-5 (recipe detail screen must exist)
- Related to US-3a (similar image loading for list thumbnails)

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

## Data Model & UI Design Stories

### US-5b: Enhanced Recipe Data Model
**As a developer**, I want the Recipe data class to fully represent the schema so that we can display complete recipe information in the UI.

**Current Gaps Analysis:**
The Recipe data class is currently missing important fields from the schema:

**Missing Metadata Fields:**
- `dateCreated` (from `metadata.date_created`)
- `categories` (from `metadata.category`) - Array of categories like "dessert", "baking"
- `language` (from `metadata.language`) - Localization support
- `coverImage` - Structured object with path, alt text, dimensions
- `source` (URL) - Already captured as sourceUrl ✓

**Missing Ingredient Structure:**
- Current: `ingredients: List<String>` - Simple flat list
- Required: Each ingredient should be an object with:
  - `item` - ingredient name
  - `amount` - numeric quantity
  - `unit` - measurement unit (cups, tbsp, etc.)
  - `notes` - optional notes ("softened", "room temperature")
  - `optional` - boolean flag
  - `substitutions` - List of alternative ingredients with ratios
  - `component` - grouping (e.g., "dough", "filling", "sauce")

**Missing Instruction Structure:**
- Current: `instructions: List<String>` - Simple flat list
- Required: Each instruction should be an object with:
  - `step` - step number
  - `description` - instruction text
  - `time` - duration for this step
  - `temperature` - oven/cooking temperature if applicable
  - `media` - images or videos for the step

**Missing Sections:**
- `equipment` - List of equipment needed
- `nutrition` - Nutritional information per serving
- `storage` - Storage instructions (refrigerator, freezer, room temperature)
- `schemaVersion` and `recipeVersion` - Version tracking

**Acceptance Criteria:**
- [ ] Ingredient data class created with all required fields
- [ ] Instruction data class created with all required fields
- [ ] Recipe data class updated to use structured types instead of strings
- [ ] YAML parser updated to properly parse nested structures
- [ ] RecipeMetadata includes `categories` and `dateCreated`
- [ ] All optional fields properly handle null values
- [ ] Backward compatibility with existing simplified recipes maintained

**Technical Tasks:**
- [ ] Create sealed classes/data classes for nested structures:
  - `data class Ingredient(item, amount, unit, notes, optional, substitutions, component)`
  - `data class Substitution(item, amount, unit, notes, ratio)`
  - `data class Instruction(step, description, time, temperature, media)`
  - `data class Media(type, path, alt/thumbnail, duration)`
  - `data class NutritionInfo(servingSize, calories, protein, carbs, fat, fiber, sugar, sodium)`
  - `data class StorageInfo(refrigerator, freezer, roomTemperature)`
- [ ] Update Recipe data class to use new types
- [ ] Update YamlRecipeParser to handle nested YAML structures
- [ ] Add serialization support for new types
- [ ] Create unit tests for parsing complex YAML structures
- [ ] Update existing recipe examples/tests

**Definition of Done:**
- Recipe data model fully represents schema structure
- YAML parser correctly extracts nested data
- New data structures are serializable/deserializable
- All existing recipes still parse correctly (backward compat)
- Type safety improved for recipe data access

**Dependencies:**
- Builds on US-5 (recipe detail must exist)
- Required for US-5c (enhanced detail UI)

---

### US-5c: Tabbed Recipe Detail UI
**As a user viewing a recipe**, I want a tabbed interface showing recipe overview, ingredients, and steps so that I can easily navigate between sections while cooking.

**Design Specification:**

**Layout Architecture:**
```
┌─────────────────────────────────┐
│    Recipe Title (Sticky)        │  ← Sticky header
├─────────────────────────────────┤
│  [Overview] [Ingredients] [Steps] │  ← Tab navigation
├─────────────────────────────────┤
│                                 │
│    TAB CONTENT AREA             │
│  (Swipeable/Scrollable)         │
│                                 │
├─────────────────────────────────┤
│  Back   |   Share   |   Save     │  ← Action buttons
└─────────────────────────────────┘
```

**Tab 1: Overview Tab** (Default View)
- Hero image from `metadata.cover_image` (or placeholder gradient + emoji 🍽️)
- Recipe metadata card showing:
  - **Servings:** X servings (editable, with +/- buttons)
  - **Prep Time:** XX minutes (if available)
  - **Cook Time:** XX minutes (if available)
  - **Total Time:** XX minutes (if available)
  - **Difficulty:** Easy/Medium/Hard badge
  - **Categories:** Tags showing cuisine type, meal type, etc.
- Short description (markdown formatted from `description` field)
- Nutrition summary card (calories per serving if available)
- Quick ingredient count: "Ingredients: X" link to ingredients tab

**Tab 2: Ingredients Tab**
- Grouped ingredients by component (e.g., "For the Dough", "For the Filling")
  - Each group as a collapsible section
- For each ingredient:
  - Checkbox (starts unchecked, user can mark as added)
  - Ingredient name with bold emphasis
  - Amount + Unit (editable based on servings slider)
  - Optional indicator badge (if applicable)
  - Notes in smaller text (e.g., "softened")
  - Substitution hint/popover if available
- Search field to filter ingredients
- "Check All" / "Uncheck All" buttons for groups

**Tab 3: Steps Tab**
- Numbered, clear instruction steps
- Each step card shows:
  - Large step number (visual hierarchy)
  - Step description (Markdown formatted, large readable text for cooking)
  - Time estimate if provided (e.g., "⏱ 5 min")
  - Temperature if applicable (e.g., "🌡 375°F")
  - Media gallery if images/videos exist
  - Optional notes field
- Progress indicator showing current step
- Next/Previous step buttons at bottom
- Ability to mark step as complete (visual strikethrough)

**Navigation & Interaction:**
- Swipe left/right between tabs
- Tap tab titles to jump to specific tab
- Top menu for quick tab selection (optional on small screens)
- Smooth transitions between tabs
- Save selected tab preference for next recipe view

**Responsive Design:**
- Phone (320-600px): Tabs visible, vertical scrolling within tabs
- Tablet (600px+): Larger text, more whitespace, side-by-side layout option
- Landscape: Optimize for horizontal space

**Accessibility:**
- High contrast text for kitchen use (light text on dark background option)
- Large touch targets (min 44pt)
- Semantic labels for all interactive elements
- Screen reader support for tab navigation
- Dark mode support for late-night cooking

**Visual Design:**
- Use Material Design 3 for Android
- Use iOS HIG for iOS
- Consistent spacing and typography
- Bottom app bar with action buttons (share, save, go to top)
- Floating action button for common actions (e.g., timer, shopping list export)

**Acceptance Criteria:**
- [ ] Three-tab interface displays correctly on iOS and Android
- [ ] Swipe navigation works smoothly between tabs
- [ ] Overview tab shows all metadata and image
- [ ] Ingredients tab with groups and checkboxes
- [ ] Steps tab with clear numbering and cooking info
- [ ] All content is readable in kitchen lighting (high contrast)
- [ ] Responsive on phone (320px) to tablet (>600px)
- [ ] Markdown in descriptions and steps renders correctly
- [ ] Images load asynchronously without blocking UI
- [ ] Performance: Tab switches < 100ms

**Technical Tasks:**
- [ ] Implement tabbed UI using Compose Material 3 TabRow
- [ ] Create separate composable functions for each tab:
  - `OverviewTab(recipe: Recipe, servings: Int)`
  - `IngredientsTab(recipe: Recipe, servings: Int)`
  - `StepsTab(recipe: Recipe)`
- [ ] Implement swipe gesture detection for tab navigation
- [ ] Add ingredient quantity calculator based on servings
- [ ] Create checkbox state management for ingredients
- [ ] Implement step progress tracking
- [ ] Add Markdown rendering for descriptions and instructions
- [ ] Optimize image loading with proper caching
- [ ] Create high-contrast theme for cooking
- [ ] Add kitchen-friendly text sizing (large, readable fonts)

**UI Components Needed:**
- `IngredientGroupCard` - Shows grouped ingredients with expander
- `IngredientItemRow` - Single ingredient with checkbox
- `StepCard` - Single instruction step with media
- `NutritionBadge` - Shows macro information
- `RecipeMetadataCard` - Shows prep time, difficulty, etc.
- `MediaGallery` - Image/video carousel for step media

**Dependencies:**
- US-5b (Enhanced Recipe Data Model) - Required for structured data
- US-5a (Recipe Detail Images) - For image loading
- Image loading library (Coil/Kamel)

**Definition of Done:**
- Tabbed interface fully functional on iOS and Android
- All recipe sections display correctly
- Smooth navigation between tabs
- Optimized for cooking use case
- High contrast option available
- Tests for tab state management

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

**Sprint 1 (2 weeks)**: US-11, US-12, US-1 (Project setup and basic auth) ✅ COMPLETED
**Sprint 2 (2 weeks)**: US-2, US-3 (Auth state management and recipe list) ✅ COMPLETED
**Sprint 2b (1 week)**: US-3a, US-5a (Recipe images for list and detail) - TODO
**Sprint 3 (2 weeks)**: US-5 ✅ COMPLETED, US-4 ✅ COMPLETED, US-5b, US-5c (Cooking display) - TODO
**Sprint 4 (2 weeks)**: US-6, US-7 (Offline support, cooking-friendly features) - TODO
**Sprint 5 (2 weeks)**: US-8, US-9, US-10 (Performance and platform polish) - TODO

**Total Estimated Timeline**: 13 weeks for Phase 1 MVP (added US-5b and US-5c)

**Progress**:
- ✅ Sprint 1-2: Auth & recipe list complete
- ✅ US-5: Recipe detail view complete (basic version)
- ✅ US-4: Search complete
- 🔜 Next: US-3a/US-5a (images), then US-5b/US-5c (enhanced model + tabbed UI), US-6/US-7 (offline)

**Data Model Update Story Added:**
- **US-5b**: Enhance Recipe data class to fully support schema with structured Ingredients, Instructions, Nutrition, etc.
- **US-5c**: Design and implement tabbed recipe detail UI with Overview, Ingredients, and Steps tabs with swipe navigation

**UI Design Highlights (US-5c):**
- Sticky recipe title header
- Three-tab navigation: Overview | Ingredients | Steps
- Swipe left/right to navigate between tabs
- Overview: Hero image + metadata (servings, times, difficulty, categories) + description
- Ingredients: Grouped by component, with checkboxes, quantity scaling based on servings
- Steps: Numbered with timing, temperature, media support, progress tracking
- Kitchen-optimized: High contrast, large readable fonts, touch-friendly