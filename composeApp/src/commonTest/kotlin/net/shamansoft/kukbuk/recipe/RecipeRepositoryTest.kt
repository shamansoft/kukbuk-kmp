package net.shamansoft.kukbuk.recipe

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.shamansoft.kukbuk.TestDatabase
import net.shamansoft.kukbuk.TestFixtures
import net.shamansoft.kukbuk.cache.RecipeCache
import net.shamansoft.kukbuk.cache.SqlDelightRecipeCache
import net.shamansoft.recipe.model.Recipe
import kotlin.test.*

class RecipeRepositoryTest {

    private lateinit var testDb: TestDatabase
    private lateinit var recipeCache: RecipeCache
    private lateinit var fakeDataSource: FakeRecipeDataSource
    private lateinit var repository: RecipeRepository

    @BeforeTest
    fun setup() {
        testDb = TestDatabase()
        recipeCache = SqlDelightRecipeCache(testDb.database)
        fakeDataSource = FakeRecipeDataSource()
        repository = RecipeRepository(fakeDataSource, recipeCache)
    }

    @AfterTest
    fun teardown() {
        testDb.close()
    }

    // ========== Load Recipes Tests ==========

    @Test
    fun `should load recipes from network when available`() = runTest {
        // Given - Network has recipe files available
        val file1 = RecipeFile("recipe-1", "Recipe 1.yaml", "2025-01-01")
        val file2 = RecipeFile("recipe-2", "Recipe 2.yaml", "2025-01-02")
        fakeDataSource.setRecipeFiles(listOf(file1, file2))

        val recipe1 = TestFixtures.createTestRecipe(title = "Recipe 1")
        val recipe2 = TestFixtures.createTestRecipe(title = "Recipe 2")
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml(title = "Recipe 1"))
        fakeDataSource.setFileContent("recipe-2", TestFixtures.createTestRecipeYaml(title = "Recipe 2"))

        // When
        repository.loadRecipes()

        // Then - State should be Success with recipes
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Success)
        assertEquals(2, state.recipes.size)
        assertEquals("Recipe 2", state.recipes[0].title) // Sorted by lastModified descending
        assertEquals("Recipe 1", state.recipes[1].title)
    }

    @Test
    fun `should show error when network fails`() = runTest {
        // Given - Network fails
        fakeDataSource.setShouldFail(true)

        // When
        repository.loadRecipes()

        // Then - State should be Error
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Error)
    }

    @Test
    fun `should show empty state when no recipes found`() = runTest {
        // Given - Network returns empty list
        fakeDataSource.setRecipeFiles(emptyList())

        // When
        repository.loadRecipes()

        // Then - State should be Empty
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Empty)
    }

    @Test
    fun `should use cached metadata when not forcing refresh`() = runTest {
        // Given - Load recipes first
        val file = RecipeFile("recipe-1", "Recipe.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml())
        repository.loadRecipes()

        // When - Load again without forceRefresh
        fakeDataSource.setRecipeFiles(emptyList()) // Change network data
        repository.loadRecipes(forceRefresh = false)

        // Then - Should still have old data from cache
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Success)
        assertEquals(1, state.recipes.size)
    }

    @Test
    fun `should force refresh when forceRefresh is true`() = runTest {
        // Given - Load recipes first
        val file1 = RecipeFile("recipe-1", "Recipe.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file1))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml(title = "Old Recipe"))
        repository.loadRecipes()

        // When - Force refresh with new data
        val file2 = RecipeFile("recipe-2", "New Recipe.yaml", "2025-01-02")
        fakeDataSource.setRecipeFiles(listOf(file2))
        fakeDataSource.setFileContent("recipe-2", TestFixtures.createTestRecipeYaml(title = "New Recipe"))
        repository.loadRecipes(forceRefresh = true)

        // Then - Should have new data
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Success)
        assertEquals(1, state.recipes.size)
        assertEquals("New Recipe", state.recipes[0].title)
    }

    // ========== Get Recipe Tests ==========

    @Test
    fun `should load recipe from network`() = runTest {
        // Given
        val yaml = TestFixtures.createTestRecipeYaml(title = "Network Recipe")
        fakeDataSource.setFileContent("recipe-1", yaml)

        // When
        val result = repository.getRecipe("recipe-1")

        // Then
        assertTrue(result is RecipeResult.Success)
        assertEquals("Network Recipe", result.data.metadata.title)
        assertFalse(result.isOffline)
    }

    @Test
    fun `should cache recipe after loading from network`() = runTest {
        // Given
        val yaml = TestFixtures.createTestRecipeYaml(title = "Recipe to Cache")
        fakeDataSource.setFileContent("recipe-1", yaml)

        // When
        repository.getRecipe("recipe-1")

        // Then - Recipe should be in persistent cache
        val cached = recipeCache.getCachedRecipe("recipe-1")
        assertNotNull(cached)
        assertEquals("Recipe to Cache", cached.recipe.metadata.title)
    }

    @Test
    fun `should use in-memory cache on second load`() = runTest {
        // Given - Load recipe first time
        val yaml = TestFixtures.createTestRecipeYaml(title = "Cached Recipe")
        fakeDataSource.setFileContent("recipe-1", yaml)
        repository.getRecipe("recipe-1")

        // When - Load again (network now fails)
        fakeDataSource.setShouldFail(true)
        val result = repository.getRecipe("recipe-1")

        // Then - Should still get recipe from in-memory cache
        assertTrue(result is RecipeResult.Success)
        assertEquals("Cached Recipe", result.data.metadata.title)
        assertFalse(result.isOffline) // From memory cache, not marked as offline
    }

    @Test
    fun `should fallback to persistent cache when network fails`() = runTest {
        // Given - Recipe in persistent cache
        val recipe = TestFixtures.createTestRecipe(title = "Offline Recipe")
        val yaml = TestFixtures.createTestRecipeYaml(title = "Offline Recipe")
        recipeCache.cacheRecipe("recipe-1", yaml, recipe)

        // And network fails
        fakeDataSource.setShouldFail(true)

        // When
        val result = repository.getRecipe("recipe-1")

        // Then - Should get recipe from persistent cache
        assertTrue(result is RecipeResult.Success)
        assertEquals("Offline Recipe", result.data.metadata.title)
        assertTrue(result.isOffline)
    }

    @Test
    fun `should return error when recipe not found anywhere`() = runTest {
        // Given - Network fails and recipe not in cache
        fakeDataSource.setShouldFail(true)

        // When
        val result = repository.getRecipe("non-existent-recipe")

        // Then
        assertTrue(result is RecipeResult.Error)
    }

    // ========== Refresh Tests ==========

    @Test
    fun `should refresh recipes list`() = runTest {
        // Given - Initial load
        val file1 = RecipeFile("recipe-1", "Old.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file1))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml(title = "Old"))
        repository.loadRecipes()

        // When - Refresh with new data
        val file2 = RecipeFile("recipe-2", "New.yaml", "2025-01-02")
        fakeDataSource.setRecipeFiles(listOf(file2))
        fakeDataSource.setFileContent("recipe-2", TestFixtures.createTestRecipeYaml(title = "New"))
        repository.refreshRecipes()

        // Then
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Success)
        assertEquals(1, state.recipes.size)
        assertEquals("New", state.recipes[0].title)
    }

    @Test
    fun `should refresh single recipe`() = runTest {
        // Given - Recipe in memory cache
        val yaml1 = TestFixtures.createTestRecipeYaml(title = "Old Version")
        fakeDataSource.setFileContent("recipe-1", yaml1)
        repository.getRecipe("recipe-1")

        // When - Refresh with new content
        val yaml2 = TestFixtures.createTestRecipeYaml(title = "New Version")
        fakeDataSource.setFileContent("recipe-1", yaml2)
        val result = repository.refreshRecipe("recipe-1")

        // Then
        assertTrue(result is RecipeResult.Success)
        assertEquals("New Version", result.data.metadata.title)
    }

    // ========== Search Tests ==========

    @Test
    fun `should search recipes by title`() = runTest {
        // Given - Loaded recipes
        val file1 = RecipeFile("recipe-1", "Chocolate Cake.yaml", "2025-01-01")
        val file2 = RecipeFile("recipe-2", "Vanilla Cake.yaml", "2025-01-02")
        fakeDataSource.setRecipeFiles(listOf(file1, file2))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml(title = "Chocolate Cake"))
        fakeDataSource.setFileContent("recipe-2", TestFixtures.createTestRecipeYaml(title = "Vanilla Cake"))
        repository.loadRecipes()

        // When
        val result = repository.searchRecipes("Chocolate")

        // Then
        assertTrue(result is RecipeResult.Success)
        assertEquals(1, result.data.size)
        assertEquals("Chocolate Cake", result.data[0].title)
    }

    @Test
    fun `should return error when searching without loaded recipes`() = runTest {
        // When - Search without loading recipes first
        val result = repository.searchRecipes("query")

        // Then
        assertTrue(result is RecipeResult.Error)
    }

    // ========== Cache Management Tests ==========

    @Test
    fun `should clear in-memory caches`() = runTest {
        // Given - Loaded recipes
        val file = RecipeFile("recipe-1", "Recipe.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml())
        repository.loadRecipes()

        // When
        repository.clearCache()

        // Then - Should need to reload from network
        fakeDataSource.setRecipeFiles(emptyList())
        repository.loadRecipes(forceRefresh = true)
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Empty)
    }

    @Test
    fun `should enforce persistent cache limit`() = runTest {
        // Given - Load many recipes
        for (i in 1..105) {
            val yaml = TestFixtures.createTestRecipeYaml(title = "Recipe $i")
            fakeDataSource.setFileContent("recipe-$i", yaml)
            repository.getRecipe("recipe-$i")
        }

        // Then - Cache should have limit enforced (tested via RecipeCache tests)
        val count = recipeCache.getCachedRecipeCount()
        assertTrue(count <= 100)
    }

    // ========== State Flow Tests ==========

    @Test
    fun `should emit loading state when loading recipes`() = runTest {
        // Given
        val file = RecipeFile("recipe-1", "Recipe.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file))
        fakeDataSource.setFileContent("recipe-1", TestFixtures.createTestRecipeYaml())

        // When/Then
        repository.recipeListState.test {
            assertEquals(RecipeListState.Loading, awaitItem()) // Initial state

            repository.loadRecipes()

            // Should eventually get Success
            val finalState = awaitItem()
            assertTrue(finalState is RecipeListState.Success)
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun `should handle invalid YAML gracefully`() = runTest {
        // Given - Invalid YAML
        val file = RecipeFile("recipe-1", "Bad.yaml", "2025-01-01")
        fakeDataSource.setRecipeFiles(listOf(file))
        fakeDataSource.setFileContent("recipe-1", "invalid: yaml: content:")

        // When
        repository.loadRecipes()

        // Then - Should show error or empty (invalid recipes are skipped)
        val state = repository.recipeListState.value
        assertTrue(state is RecipeListState.Error || state is RecipeListState.Empty)
    }

    @Test
    fun `should warm up memory cache from persistent cache`() = runTest {
        // Given - Recipe in persistent cache only
        val recipe = TestFixtures.createTestRecipe(title = "Persistent Recipe")
        val yaml = TestFixtures.createTestRecipeYaml(title = "Persistent Recipe")
        recipeCache.cacheRecipe("recipe-1", yaml, recipe)

        // And network fails
        fakeDataSource.setShouldFail(true)

        // When - Load recipe (will hit persistent cache)
        val result1 = repository.getRecipe("recipe-1")

        // Then - Should be in memory cache now (second load doesn't hit persistent cache)
        assertTrue(result1 is RecipeResult.Success)
        assertTrue(result1.isOffline)

        // Load again - should come from memory cache (faster)
        val result2 = repository.getRecipe("recipe-1")
        assertTrue(result2 is RecipeResult.Success)
        assertFalse(result2.isOffline) // From memory cache, not marked as offline
    }
}

// ========== Fake Implementation for Testing ==========

/**
 * Fake implementation of RecipeDataSource for testing.
 * Allows controlling behavior to simulate network success/failure.
 */
class FakeRecipeDataSource : RecipeDataSource {
    private var shouldFail = false
    private var recipeFiles = emptyList<RecipeFile>()
    private val fileContents = mutableMapOf<String, String>()

    fun setShouldFail(fail: Boolean) {
        shouldFail = fail
    }

    fun setRecipeFiles(files: List<RecipeFile>) {
        recipeFiles = files
    }

    fun setFileContent(fileId: String, content: String) {
        fileContents[fileId] = content
    }

    override suspend fun listRecipeFiles(): DataSourceResult<List<RecipeFile>> {
        return if (shouldFail) {
            DataSourceResult.Error("Network failed")
        } else {
            DataSourceResult.Success(recipeFiles)
        }
    }

    override suspend fun getFileContent(fileId: String): DataSourceResult<String> {
        return if (shouldFail) {
            DataSourceResult.Error("Network failed")
        } else {
            val content = fileContents[fileId]
            if (content != null) {
                DataSourceResult.Success(content)
            } else {
                DataSourceResult.Error("File not found: $fileId")
            }
        }
    }
}
