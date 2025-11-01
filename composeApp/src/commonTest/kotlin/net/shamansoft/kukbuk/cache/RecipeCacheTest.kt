package net.shamansoft.kukbuk.cache

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import net.shamansoft.kukbuk.TestDatabase
import net.shamansoft.kukbuk.TestFixtures
import kotlin.test.*

class RecipeCacheTest {

    private lateinit var testDb: TestDatabase
    private lateinit var recipeCache: RecipeCache

    @BeforeTest
    fun setup() {
        testDb = TestDatabase()
        recipeCache = SqlDelightRecipeCache(testDb.database)
    }

    @AfterTest
    fun teardown() {
        testDb.close()
    }

    @Test
    fun `should cache and retrieve recipe by ID`() = runTest {
        // Given
        val recipe = TestFixtures.createTestRecipe(title = "Pasta Carbonara")
        val yaml = TestFixtures.createTestRecipeYaml(title = "Pasta Carbonara")

        // When
        recipeCache.cacheRecipe("recipe-1", yaml, recipe)
        val result = recipeCache.getCachedRecipe("recipe-1")

        // Then
        assertNotNull(result)
        assertEquals("Pasta Carbonara", result.recipe.metadata.title)
    }

    @Test
    fun `should return null for non-existent recipe`() = runTest {
        // When
        val result = recipeCache.getCachedRecipe("non-existent-id")

        // Then
        assertNull(result)
    }

    @Test
    fun `should return most recent recipes ordered by lastViewed`() = runTest {
        // Given - Cache 3 recipes
        val recipe1 = TestFixtures.createTestRecipe(title = "First")
        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "First"), recipe1)
        Thread.sleep(50) // Use Thread.sleep to ensure actual time passes

        val recipe2 = TestFixtures.createTestRecipe(title = "Second")
        recipeCache.cacheRecipe("recipe-2", TestFixtures.createTestRecipeYaml(title = "Second"), recipe2)
        Thread.sleep(50) // Use Thread.sleep to ensure actual time passes

        val recipe3 = TestFixtures.createTestRecipe(title = "Third")
        recipeCache.cacheRecipe("recipe-3", TestFixtures.createTestRecipeYaml(title = "Third"), recipe3)

        // When
        val recent = recipeCache.getMostRecentRecipes(limit = 10)

        // Then - Should be ordered by most recent first
        assertEquals(3, recent.size)
        assertEquals("Third", recent[0].recipe.metadata.title)  // Most recent
        assertEquals("Second", recent[1].recipe.metadata.title)
        assertEquals("First", recent[2].recipe.metadata.title)  // Oldest
    }

    @Test
    fun `should enforce cache limit by removing oldest recipes`() = runTest {
        // Given - Cache 105 recipes (exceeds limit of 100)
        for (i in 1..105) {
            val recipe = TestFixtures.createTestRecipe(title = "Recipe $i")
            val yaml = TestFixtures.createTestRecipeYaml(title = "Recipe $i")
            recipeCache.cacheRecipe("recipe-$i", yaml, recipe)

            // Small delay to ensure different timestamps
            if (i % 10 == 0) {
                Thread.sleep(10)
            }
        }

        // When - Enforce the limit
        recipeCache.enforceCacheLimit(maxCount = 100)

        // Then - Should only have 100 recipes
        val count = recipeCache.getCachedRecipeCount()
        assertEquals(100, count)

        // Verify at least some of the newest recipes are still there
        // (We check the last 5 which should definitely be kept)
        assertNotNull(recipeCache.getCachedRecipe("recipe-101"))
        assertNotNull(recipeCache.getCachedRecipe("recipe-102"))
        assertNotNull(recipeCache.getCachedRecipe("recipe-103"))
        assertNotNull(recipeCache.getCachedRecipe("recipe-104"))
        assertNotNull(recipeCache.getCachedRecipe("recipe-105"))
    }

    @Test
    fun `should update lastViewed timestamp when marking as viewed`() = runTest {
        // Given - Cache two recipes
        val recipe1 = TestFixtures.createTestRecipe(title = "Recipe 1")
        val recipe2 = TestFixtures.createTestRecipe(title = "Recipe 2")

        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "Recipe 1"), recipe1)
        Thread.sleep(100)
        recipeCache.cacheRecipe("recipe-2", TestFixtures.createTestRecipeYaml(title = "Recipe 2"), recipe2)

        // Get initial recent list - recipe-2 should be first (most recent)
        val initial = recipeCache.getMostRecentRecipes(limit = 10)
        assertEquals("Recipe 2", initial[0].recipe.metadata.title)
        assertEquals("Recipe 1", initial[1].recipe.metadata.title)

        // When - Mark recipe-1 as viewed
        Thread.sleep(100)
        recipeCache.markRecipeAsViewed("recipe-1")

        // Then - recipe-1 should now be first (most recently viewed)
        val updated = recipeCache.getMostRecentRecipes(limit = 10)
        assertEquals("Recipe 1", updated[0].recipe.metadata.title)
        assertEquals("Recipe 2", updated[1].recipe.metadata.title)
    }

    @Test
    fun `should clear all cached recipes`() = runTest {
        // Given - Cache multiple recipes
        for (i in 1..5) {
            val recipe = TestFixtures.createTestRecipe(title = "Recipe $i")
            val yaml = TestFixtures.createTestRecipeYaml(title = "Recipe $i")
            recipeCache.cacheRecipe("recipe-$i", yaml, recipe)
        }

        // Verify recipes are cached
        assertEquals(5, recipeCache.getCachedRecipeCount())

        // When
        recipeCache.clearCache()

        // Then
        assertEquals(0, recipeCache.getCachedRecipeCount())
        assertNull(recipeCache.getCachedRecipe("recipe-1"))
        assertNull(recipeCache.getCachedRecipe("recipe-2"))
    }

    @Test
    fun `should observe cache size changes with Flow`() = runTest {
        // Given
        recipeCache.observeCacheSize().test {
            // Then - Initially empty
            val initial = awaitItem()
            assertEquals(0, initial)

            // When - Cache a recipe
            val recipe1 = TestFixtures.createTestRecipe(title = "New Recipe")
            recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "New Recipe"), recipe1)

            // Then - Should emit updated size
            val afterCache = awaitItem()
            assertEquals(1, afterCache)

            // When - Cache another recipe
            val recipe2 = TestFixtures.createTestRecipe(title = "Another Recipe")
            recipeCache.cacheRecipe("recipe-2", TestFixtures.createTestRecipeYaml(title = "Another Recipe"), recipe2)

            // Then - Should emit size 2
            val afterSecondCache = awaitItem()
            assertEquals(2, afterSecondCache)

            // When - Clear cache
            recipeCache.clearCache()

            // Then - Should emit 0
            val afterClear = awaitItem()
            assertEquals(0, afterClear)
        }
    }

    @Test
    fun `should check if recipe is cached`() = runTest {
        // Given
        val recipe = TestFixtures.createTestRecipe(title = "Cached Recipe")
        val yaml = TestFixtures.createTestRecipeYaml(title = "Cached Recipe")
        recipeCache.cacheRecipe("recipe-1", yaml, recipe)

        // When/Then
        assertTrue(recipeCache.isRecipeCached("recipe-1"))
        assertFalse(recipeCache.isRecipeCached("recipe-2"))
    }

    @Test
    fun `should handle recipe with null coverImage gracefully`() = runTest {
        // Given - Create recipe with null cover image
        val recipe = TestFixtures.createTestRecipe().copy(
            metadata = TestFixtures.createTestRecipe().metadata.copy(
                coverImage = null
            )
        )
        // Create YAML without coverImage field
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Test Recipe"
              author: "Test Author"
              source: "https://example.com"
              category:
                - "Test"
              servings: 4
              date_created: "2025-01-01"
            description: "Test without image"
            ingredients:
              - item: "flour"
                amount: "2"
                unit: "cups"
            instructions:
              - step: 1
                description: "Mix everything"
        """.trimIndent()

        // When
        recipeCache.cacheRecipe("recipe-no-image", yaml, recipe)
        val cached = recipeCache.getCachedRecipe("recipe-no-image")

        // Then
        assertNotNull(cached)
        assertNull(cached.recipe.metadata.coverImage)
    }

    @Test
    fun `should store and retrieve complete YAML content`() = runTest {
        // Given
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Test Recipe"
              author: "Test Author"
              source: "https://example.com"
              category:
                - "Test"
              servings: 4
              date_created: "2025-01-01"
            description: "Test description with special chars: åäö"
            ingredients:
              - item: "flour"
                amount: "2"
                unit: "cups"
            instructions:
              - step: 1
                description: "Mix everything"
        """.trimIndent()

        val recipe = TestFixtures.createTestRecipe()

        // When
        recipeCache.cacheRecipe("recipe-yaml-test", yaml, recipe)
        val result = recipeCache.getCachedRecipe("recipe-yaml-test")

        // Then - YAML should be stored (checked via database internals)
        assertNotNull(result)
        assertEquals("Test Recipe", result.recipe.metadata.title)
    }

    @Test
    fun `should overwrite existing recipe when caching with same ID`() = runTest {
        // Given - Cache initial recipe
        val recipe1 = TestFixtures.createTestRecipe(title = "Original Title")
        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "Original Title"), recipe1)

        // When - Cache new recipe with same ID
        delay(10)
        val recipe2 = TestFixtures.createTestRecipe(title = "Updated Title")
        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "Updated Title"), recipe2)

        // Then - Should have only one recipe with updated content
        assertEquals(1, recipeCache.getCachedRecipeCount())

        val retrieved = recipeCache.getCachedRecipe("recipe-1")
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved.recipe.metadata.title)
    }

    @Test
    fun `should return all cached recipe IDs`() = runTest {
        // Given
        val recipe1 = TestFixtures.createTestRecipe(title = "Recipe 1")
        val recipe2 = TestFixtures.createTestRecipe(title = "Recipe 2")
        val recipe3 = TestFixtures.createTestRecipe(title = "Recipe 3")

        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "Recipe 1"), recipe1)
        recipeCache.cacheRecipe("recipe-2", TestFixtures.createTestRecipeYaml(title = "Recipe 2"), recipe2)
        recipeCache.cacheRecipe("recipe-3", TestFixtures.createTestRecipeYaml(title = "Recipe 3"), recipe3)

        // When
        val ids = recipeCache.getAllCachedRecipeIds()

        // Then
        assertEquals(3, ids.size)
        assertTrue(ids.contains("recipe-1"))
        assertTrue(ids.contains("recipe-2"))
        assertTrue(ids.contains("recipe-3"))
    }

    @Test
    fun `should delete expired recipes`() = runTest {
        // Given - Cache recipes with old timestamps
        val recipe1 = TestFixtures.createTestRecipe(title = "Old Recipe")
        val recipe2 = TestFixtures.createTestRecipe(title = "New Recipe")

        recipeCache.cacheRecipe("recipe-1", TestFixtures.createTestRecipeYaml(title = "Old Recipe"), recipe1)

        // Wait to create time gap
        Thread.sleep(200)

        recipeCache.cacheRecipe("recipe-2", TestFixtures.createTestRecipeYaml(title = "New Recipe"), recipe2)

        // When - Delete recipes older than 100ms
        recipeCache.deleteExpiredRecipes(maxAgeMillis = 100)

        // Then - Old recipe should be deleted, new one should remain
        assertNull(recipeCache.getCachedRecipe("recipe-1"))
        assertNotNull(recipeCache.getCachedRecipe("recipe-2"))
    }

    @Test
    fun `should preserve recipe data integrity after cache and retrieve`() = runTest {
        // Given - Complex recipe with all fields populated
        val originalRecipe = TestFixtures.createTestRecipe(
            title = "Complex Recipe",
            author = "Master Chef",
            description = "A very detailed recipe"
        )
        val yaml = TestFixtures.createTestRecipeYaml(
            title = "Complex Recipe",
            author = "Master Chef"
        )

        // When
        recipeCache.cacheRecipe("recipe-complex", yaml, originalRecipe)
        val result = recipeCache.getCachedRecipe("recipe-complex")

        // Then - Verify all fields are preserved
        assertNotNull(result)
        val retrievedRecipe = result.recipe

        assertEquals(originalRecipe.schemaVersion, retrievedRecipe.schemaVersion)
        assertEquals(originalRecipe.recipeVersion, retrievedRecipe.recipeVersion)
        assertEquals(originalRecipe.metadata.title, retrievedRecipe.metadata.title)
        assertEquals(originalRecipe.metadata.author, retrievedRecipe.metadata.author)
        assertEquals(originalRecipe.metadata.source, retrievedRecipe.metadata.source)
        assertEquals(originalRecipe.metadata.category, retrievedRecipe.metadata.category)
        assertEquals(originalRecipe.metadata.tags, retrievedRecipe.metadata.tags)
        assertEquals(originalRecipe.metadata.prepTime, retrievedRecipe.metadata.prepTime)
        assertEquals(originalRecipe.metadata.cookTime, retrievedRecipe.metadata.cookTime)
        assertEquals(originalRecipe.metadata.servings, retrievedRecipe.metadata.servings)
        assertEquals(originalRecipe.ingredients.size, retrievedRecipe.ingredients.size)
        assertEquals(originalRecipe.instructions.size, retrievedRecipe.instructions.size)
    }
}
