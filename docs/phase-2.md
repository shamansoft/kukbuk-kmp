# MyKukBuk Mobile App - Phase 2 Enhancement Stories

## Epic: Advanced Recipe Features & Navigation Patterns

**Goal**: Enhance the recipe viewing experience with advanced navigation, personalization features, and multi-platform optimizations based on Phase 1 learnings.

---

## Recipe Navigation Enhancement Stories

### US-2.1: Tabbed Recipe Detail Navigation
**As a user viewing recipes on a tablet or desktop**, I want a tabbed interface for recipe sections so that I can quickly jump between overview, ingredients, and steps without scrolling.

**Background:**
Phase 1 implements a single-screen scrollable view optimized for mobile cooking. Phase 2 adds a tabbed alternative that's better suited for larger screens (tablets, desktop web) where users may prefer quick navigation over continuous scrolling.

**Design Specification:**

**Tabbed Navigation Layout:**
```
┌─────────────────────────────────────────┐
│  ← Back  |  Recipe Title                │  ← App bar (sticky)
├─────────────────────────────────────────┤
│  [Overview] [Ingredients] [Steps]       │  ← Tab navigation
├─────────────────────────────────────────┤
│                                         │
│  SCROLLABLE TAB CONTENT                 │
│  (Only active tab content visible)      │
│                                         │
├─────────────────────────────────────────┤
│  Share  |  Save  |  More Options        │  ← Action buttons
└─────────────────────────────────────────┘
```

**Tab 1: Overview Tab**
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
- Credits: Author, source URL, date created
- Quick links: "Go to Ingredients" and "Go to Steps" buttons
- Storage instructions (if available)
- Equipment list (if available)

**Tab 2: Ingredients Tab**
- Servings slider/counter at top (quantity scales based on this)
- Grouped ingredients by component (e.g., "For the Dough", "For the Filling")
  - Each group as a collapsible section (starts expanded)
- For each ingredient:
  - Checkbox (user can mark as gathered)
  - Ingredient name with bold emphasis
  - Amount + Unit (scales with servings slider)
  - Optional indicator badge (if applicable)
  - Notes in smaller text (e.g., "softened", "room temperature")
  - Substitution hint/popover if available
- Search field to filter ingredients (sticky at top)
- "Check All" / "Uncheck All" buttons for each group
- Import to shopping list button (Phase 2.2 feature)

**Tab 3: Steps Tab**
- Numbered, clear instruction steps
- Each step card shows:
  - Large step number (visual hierarchy)
  - Step description (Markdown formatted, large readable text for cooking)
  - Time estimate if provided (e.g., "⏱ 5 min")
  - Temperature if applicable (e.g., "🌡 375°F")
  - Media gallery if images/videos exist (with full-screen view option)
  - Optional notes field
- Progress indicator showing current step (Step 2 of 8)
- Step navigation: Previous/Next buttons or step selector
- Ability to mark step as complete (visual checkmark/strikethrough)
- Timer integration (Phase 2.3 feature)

**Navigation & Interaction:**
- Swipe left/right between tabs (on touch devices)
- Tap tab titles to jump to specific tab
- Smooth transitions between tabs (< 100ms)
- Save selected tab preference for next recipe view
- Remember scroll position within each tab when switching

**Responsive Behavior:**
- **Phone (< 600px)**: Tabs visible but scrollable horizontally if needed
- **Tablet (600-1024px)**: Full-width tabs, larger content area
- **Desktop (> 1024px)**: Optional side-by-side layout (tab content + sidebar with recipe summary)

**Platform-Specific Navigation:**
- **Android**: Material 3 tab design, swipe gestures
- **iOS**: iOS HIG tab bar at bottom or top, smooth transitions
- **Web**: Tab design with keyboard navigation support (← → keys)

**Accessibility:**
- High contrast text options for readability
- Large touch targets (min 44pt)
- Semantic labels for all interactive elements
- Screen reader support for tab navigation and content
- Keyboard shortcuts for tab navigation (Alt+1, Alt+2, Alt+3)
- Dark mode support

**Visual Design:**
- Use Material Design 3 for Android
- Use iOS HIG for iOS
- Use responsive web design for WASM
- Consistent spacing and typography across tabs
- Animations: Smooth fade/slide between tabs
- Option for high-contrast cooking mode

**Acceptance Criteria:**
- [ ] Three-tab interface displays correctly on iOS, Android, and Web
- [ ] Swipe navigation works smoothly between tabs (touch devices)
- [ ] Tab switching is fast (< 100ms)
- [ ] Overview tab shows all metadata and image
- [ ] Ingredients tab with groups, checkboxes, and quantity scaling
- [ ] Steps tab with clear numbering, timing, and media support
- [ ] All content readable in high-contrast mode
- [ ] Responsive on phone (320px) to desktop (1920px)
- [ ] Markdown in descriptions and steps renders correctly
- [ ] Images load asynchronously without blocking UI
- [ ] Tab preference saved and restored
- [ ] Scroll position preserved when switching tabs

**Technical Tasks:**
- [ ] Detect screen size and choose navigation pattern (single-screen vs tabs)
  - Single-screen: phone/small screens
  - Tabs: tablet/large screens (configurable breakpoint)
- [ ] Implement tabbed UI using Compose Material 3 TabRow/Tab
- [ ] Create separate composable functions for each tab:
  - `OverviewTab(recipe: Recipe, servings: Int, state: RecipeDetailState)`
  - `IngredientsTab(recipe: Recipe, servings: Int, state: IngredientsState)`
  - `StepsTab(recipe: Recipe, state: CookingState)`
- [ ] Implement swipe gesture detection for tab navigation
- [ ] Add tab preference persistence (save to DataStore)
- [ ] Implement scroll position memory per tab
- [ ] Create responsive layout manager (determine single vs tabbed based on screen)
- [ ] Add Markdown rendering for descriptions and instructions
- [ ] Optimize image loading with proper caching
- [ ] Test on various screen sizes and orientations

**UI Components Needed:**
- `IngredientGroupCard` - Shows grouped ingredients with expander
- `IngredientItemRow` - Single ingredient with checkbox and quantity
- `StepCard` - Single instruction step with media and progress
- `NutritionBadge` - Shows macro information in compact format
- `RecipeMetadataCard` - Shows prep time, difficulty, servings, categories
- `MediaGallery` - Image/video carousel for step media with fullscreen option
- `ResponsiveRecipeDetail` - Wrapper that chooses between single-screen and tabbed layouts
- `TabNavigationView` - Tabbed navigation container with swipe support

**Dependencies:**
- Phase 1: US-5c (Single-screen recipe view must exist first)
- Phase 1: US-5b (Structured recipe data model)
- Phase 1: US-5a (Recipe images)

**Definition of Done:**
- Tabbed interface works on tablet and larger screens
- Single-screen layout still works on phones (existing Phase 1 behavior)
- Tab switching is smooth and responsive
- All recipe sections display correctly in tabs
- Preference and scroll state preserved
- All platform-specific navigation patterns work correctly
- Performance meets benchmarks (< 100ms tab switches)

---

### US-2.1a: Interactive Ingredient Checkboxes & Serving Scaling
**As a user cooking from a recipe**, I want to check off ingredients as I add them and adjust serving sizes so that I can keep track of what I've added and scale quantities easily.

**Acceptance Criteria:**
- [ ] Checkbox next to each ingredient that persists during cooking session
- [ ] "Check All" / "Uncheck All" buttons for each ingredient group
- [ ] Serving size counter with +/- buttons at top of ingredients section
- [ ] Ingredient quantities automatically scale when serving size changes
- [ ] Visual feedback when ingredients are checked (strikethrough or dimmed)
- [ ] Checkbox state preserved when navigating away and back to recipe

**Technical Tasks:**
- [ ] Create ingredient checkbox state management in ViewModel
- [ ] Add serving size state and quantity scaling logic
- [ ] Update IngredientRow composable to include checkbox
- [ ] Implement serving counter UI component
- [ ] Add state persistence (save to ViewModel, optionally DataStore)
- [ ] Create quantity recalculation function for ingredient amounts
- [ ] Handle fractional quantities (1/2 cup, 1/4 tsp, etc.)

**Definition of Done:**
- Checkboxes work smoothly without lag
- Serving scaling correctly adjusts all ingredient quantities
- State persists during cooking session
- Works on all platforms (iOS, Android, Web)

---

### US-2.1b: Step Progress Tracking
**As a user following a recipe**, I want to mark steps as complete and track my progress so that I don't lose my place while cooking.

**Acceptance Criteria:**
- [ ] Checkbox or "Mark Complete" button on each step
- [ ] Progress indicator showing "Step X of Y"
- [ ] Visual feedback for completed steps (checkmark, different color)
- [ ] Auto-scroll to next uncompleted step when marking step complete
- [ ] "Previous Step" / "Next Step" navigation buttons
- [ ] Progress state preserved when navigating away and back

**Technical Tasks:**
- [ ] Create step completion state management in ViewModel
- [ ] Add completion checkbox/button to StepCard composable
- [ ] Implement progress indicator component
- [ ] Add step navigation (prev/next) functionality
- [ ] Auto-scroll logic to next uncompleted step
- [ ] State persistence during cooking session
- [ ] Visual styling for completed vs active steps

**Definition of Done:**
- Step completion works smoothly
- Progress tracking is clear and accurate
- Navigation between steps is intuitive
- Works on all platforms (iOS, Android, Web)

---

### US-2.2: Shopping List Export
**As a user**, I want to export recipe ingredients to a shopping list so that I can purchase items before cooking.

**Acceptance Criteria:**
- [ ] Export ingredients button in ingredients tab/section
- [ ] Generate shopping list in multiple formats (text, PDF, app integration)
- [ ] Combine ingredients from multiple recipes
- [ ] Mark items as purchased
- [ ] Share shopping list with others

**Technical Tasks:**
- [ ] Create shopping list data model and repository
- [ ] Implement export functionality (text, PDF formats)
- [ ] Add UI for shopping list management
- [ ] Implement sharing integration with OS
- [ ] Add PDF generation library integration

---

### US-2.3: Cooking Timer Integration
**As a cook**, I want to set timers directly from recipe steps so that I don't lose track of cooking times.

**Acceptance Criteria:**
- [ ] Quick timer button on each step with suggested time
- [ ] Timer notifications when time is up
- [ ] Multiple simultaneous timers support
- [ ] Timer works in background
- [ ] Integration with OS timer app (iOS/Android)

**Technical Tasks:**
- [ ] Implement timer state management
- [ ] Add notification support
- [ ] Create timer UI components
- [ ] Integrate with platform timers
- [ ] Handle background execution

---

## Personalization & Settings Stories

### US-2.4: Recipe Favorites & Collections
**As a user**, I want to organize recipes into collections/favorites so that I can quickly find recipes I love.

**Acceptance Criteria:**
- [ ] Mark recipes as favorites
- [ ] Create custom collections (e.g., "Weeknight Dinners", "Desserts")
- [ ] Collection management UI
- [ ] Search within collections
- [ ] Share collections with others

**Technical Tasks:**
- [ ] Create collections data model
- [ ] Implement collection repository layer
- [ ] Add UI for collection management
- [ ] Create collection-based filtering in recipe list

---

### US-2.5: Recipe Rating & Notes
**As a user**, I want to rate recipes and add personal notes so that I can remember my experience.

**Acceptance Criteria:**
- [ ] Star rating system (1-5 stars)
- [ ] Add/edit personal cooking notes
- [ ] View community ratings (if connected to backend)
- [ ] Filter by rating
- [ ] Search within notes

**Technical Tasks:**
- [ ] Create ratings/notes data model
- [ ] Implement local storage for ratings/notes
- [ ] Add rating UI component
- [ ] Create notes editing screen

---

## Platform Enhancement Stories

### US-2.6: Web App (WASM) Optimization
**As a web user**, I want the web app to feel native and responsive so that I can use it on my laptop or tablet browser.

**Acceptance Criteria:**
- [ ] Keyboard navigation and shortcuts
- [ ] Responsive design for all screen sizes
- [ ] Print-friendly recipe views
- [ ] Offline support via service workers
- [ ] Performance optimization for web

---

### US-2.7: iOS App Refinements
**As an iOS user**, I want native iOS features so that the app feels like a real iOS app.

**Acceptance Criteria:**
- [ ] Integration with iOS shortcuts (Siri)
- [ ] Today widget showing current recipe
- [ ] Share sheet integration
- [ ] App clips for quick recipe sharing
- [ ] Push notifications for reminders

---

### US-2.8: Android App Refinements
**As an Android user**, I want native Android features so that the app integrates well with my phone.

**Acceptance Criteria:**
- [ ] Android widget showing recipe
- [ ] Biometric authentication (fingerprint/face unlock)
- [ ] Android share menu integration
- [ ] Adaptive theming support
- [ ] Android 14+ specific features

---

## Performance & Analytics Stories

### US-2.9: Performance Monitoring
**As a developer**, I want to monitor app performance so that I can identify and fix bottlenecks.

**Technical Tasks:**
- [ ] Add performance metrics collection
- [ ] Track app startup time
- [ ] Monitor recipe loading times
- [ ] Measure UI frame rates
- [ ] Analyze memory usage

---

### US-2.10: Usage Analytics
**As a product manager**, I want to understand how users interact with recipes so that I can improve the app.

**Acceptance Criteria:**
- [ ] Track which recipes are viewed most
- [ ] Monitor feature usage (search, favorites, etc.)
- [ ] Collect crash reports
- [ ] Privacy-respecting analytics

---

## Sprint Planning Suggestion for Phase 2

**Sprint 6 (2 weeks)**: US-2.1 (Tabbed navigation for larger screens)
**Sprint 7 (2 weeks)**: US-2.2, US-2.3 (Shopping list, timer integration)
**Sprint 8 (2 weeks)**: US-2.4, US-2.5 (Collections, ratings, notes)
**Sprint 9 (2 weeks)**: US-2.6 (Web app optimization)
**Sprint 10 (2 weeks)**: US-2.7, US-2.8 (Platform refinements)
**Sprint 11 (1 week)**: US-2.9, US-2.10 (Performance & analytics)

**Total Estimated Timeline**: 11 weeks for Phase 2

**Phase 2 Goals:**
- Support tablet and large screens with optimized navigation
- Add practical cooking features (timers, shopping lists)
- Enable recipe personalization and organization
- Optimize for all platforms (iOS, Android, Web)
- Establish performance and analytics baselines

---

## Dependencies on Phase 1

All Phase 2 stories depend on Phase 1 completion:
- US-5b: Enhanced Recipe data model (structured ingredients, instructions, nutrition)
- US-5c: Single-screen recipe detail view
- US-5a: Recipe images
- US-6: Cooking-friendly display features
- US-7: Offline recipe access
- US-8: Base app performance

Once Phase 1 MVP is complete and stable, Phase 2 development can begin in parallel or as a follow-up release.
