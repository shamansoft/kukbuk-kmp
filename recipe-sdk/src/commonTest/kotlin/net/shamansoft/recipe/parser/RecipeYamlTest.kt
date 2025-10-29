package net.shamansoft.recipe.parser

import kotlinx.datetime.LocalDate
import net.shamansoft.recipe.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecipeYamlTest {

    @Test
    fun testParseSimpleRecipe() {
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Test Recipe"
              source: "https://example.com/recipe"
              date_created: "2025-01-15"
              servings: 4
            ingredients:
              - item: "flour"
                amount: "2"
                unit: "cups"
            instructions:
              - step: 1
                description: "Mix ingredients"
        """.trimIndent()

        val recipe = RecipeYaml.parse(yaml)

        assertEquals("1.0.0", recipe.schemaVersion)
        assertEquals("Test Recipe", recipe.metadata.title)
        assertEquals(4, recipe.metadata.servings)
        assertEquals(1, recipe.ingredients.size)
        assertEquals("flour", recipe.ingredients[0].item)
        assertEquals(1, recipe.instructions.size)
    }

    @Test
    fun testParseRecipeWithOptionalFields() {
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Complex Recipe"
              source: "https://example.com/recipe"
              author: "Chef John"
              language: "en"
              date_created: "2025-01-15"
              category:
                - "dessert"
                - "chocolate"
              tags:
                - "sweet"
                - "easy"
              servings: 8
              prep_time: "15m"
              cook_time: "30m"
              total_time: "45m"
              difficulty: "easy"
              cover_image:
                path: "/images/recipe.jpg"
                alt: "Delicious recipe"
            description: "A wonderful recipe"
            ingredients:
              - item: "sugar"
                amount: "1"
                unit: "cup"
                notes: "white sugar"
                optional: false
                component: "main"
            equipment:
              - "mixing bowl"
              - "oven"
            instructions:
              - step: 1
                description: "Preheat oven"
                time: "5m"
                temperature: "180°C"
            nutrition:
              serving_size: "1 piece"
              calories: 250
              protein: 5.5
              carbohydrates: 30.0
              fat: 12.0
            notes: "Best served warm"
            storage:
              refrigerator: "3 days"
              freezer: "1 month"
        """.trimIndent()

        val recipe = RecipeYaml.parse(yaml)

        assertEquals("Chef John", recipe.metadata.author)
        assertEquals(listOf("dessert", "chocolate"), recipe.metadata.category)
        assertEquals(listOf("sweet", "easy"), recipe.metadata.tags)
        assertEquals("easy", recipe.metadata.difficulty)
        assertNotNull(recipe.metadata.coverImage)
        assertEquals("/images/recipe.jpg", recipe.metadata.coverImage?.path)
        assertEquals("A wonderful recipe", recipe.description)
        assertEquals(listOf("mixing bowl", "oven"), recipe.equipment)
        assertNotNull(recipe.nutrition)
        assertEquals(250, recipe.nutrition?.calories)
        assertNotNull(recipe.storage)
        assertEquals("3 days", recipe.storage?.refrigerator)
    }

    @Test
    fun testParseRecipeWithSubstitutions() {
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Recipe with Substitutions"
              source: "https://example.com/recipe"
              date_created: "2025-01-15"
              servings: 4
            ingredients:
              - item: "butter"
                amount: "100"
                unit: "g"
                substitutions:
                  - item: "margarine"
                    ratio: "1:1"
                  - item: "oil"
                    amount: "80"
                    unit: "ml"
                    ratio: "0.8:1"
            instructions:
              - step: 1
                description: "Use butter or substitute"
        """.trimIndent()

        val recipe = RecipeYaml.parse(yaml)

        assertEquals(1, recipe.ingredients.size)
        assertNotNull(recipe.ingredients[0].substitutions)
        assertEquals(2, recipe.ingredients[0].substitutions?.size)
        assertEquals("margarine", recipe.ingredients[0].substitutions?.get(0)?.item)
        assertEquals("1:1", recipe.ingredients[0].substitutions?.get(0)?.ratio)
        assertEquals("80", recipe.ingredients[0].substitutions?.get(1)?.amount)
    }

    @Test
    fun testParseRecipeWithMedia() {
        val yaml = """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "Recipe with Media"
              source: "https://example.com/recipe"
              date_created: "2025-01-15"
              servings: 2
            ingredients:
              - item: "ingredient"
            instructions:
              - step: 1
                description: "Follow along"
                media:
                  - type: "image"
                    path: "/images/step1.jpg"
                    alt: "Step 1"
                  - type: "video"
                    path: "/videos/step1.mp4"
                    thumbnail: "/videos/thumb.jpg"
                    duration: "2:30"
        """.trimIndent()

        val recipe = RecipeYaml.parse(yaml)

        assertEquals(1, recipe.instructions.size)
        assertNotNull(recipe.instructions[0].media)
        assertEquals(2, recipe.instructions[0].media?.size)

        val imageMedia = recipe.instructions[0].media?.get(0)
        assertNotNull(imageMedia)
        assertEquals("image", imageMedia?.type)
        assertEquals("/images/step1.jpg", imageMedia?.path)
        assertEquals("Step 1", imageMedia?.alt)
        assertTrue(imageMedia?.isImage == true)

        val videoMedia = recipe.instructions[0].media?.get(1)
        assertNotNull(videoMedia)
        assertEquals("video", videoMedia?.type)
        assertEquals("/videos/step1.mp4", videoMedia?.path)
        assertEquals("/videos/thumb.jpg", videoMedia?.thumbnail)
        assertEquals("2:30", videoMedia?.duration)
        assertTrue(videoMedia?.isVideo == true)
    }

    @Test
    fun testSerializeSimpleRecipe() {
        val recipe = Recipe(
            schemaVersion = "1.0.0",
            recipeVersion = "1.0.0",
            metadata = RecipeMetadata(
                title = "Test Recipe",
                source = "https://example.com/recipe",
                dateCreated = LocalDate(2025, 1, 15),
                servings = 4
            ),
            ingredients = listOf(
                Ingredient(
                    item = "flour",
                    amount = "2",
                    unit = "cups"
                )
            ),
            instructions = listOf(
                Instruction(
                    step = 1,
                    description = "Mix ingredients"
                )
            )
        )

        val yaml = RecipeYaml.serialize(recipe)

        assertTrue(yaml.contains("schema_version"))
        assertTrue(yaml.contains("Test Recipe"))
        assertTrue(yaml.contains("flour"))
        assertTrue(yaml.contains("Mix ingredients"))
    }

    @Test
    fun testRoundTrip() {
        val original = Recipe(
            schemaVersion = "1.0.0",
            recipeVersion = "1.0.0",
            metadata = RecipeMetadata(
                title = "Round Trip Recipe",
                source = "https://example.com/recipe",
                author = "Test Author",
                dateCreated = LocalDate(2025, 1, 15),
                category = listOf("breakfast"),
                tags = listOf("quick", "easy"),
                servings = 2,
                prepTime = "5m",
                cookTime = "10m",
                totalTime = "15m",
                difficulty = "easy"
            ),
            description = "A simple test recipe",
            ingredients = listOf(
                Ingredient(item = "flour", amount = "2", unit = "cups", notes = "all-purpose"),
                Ingredient(item = "sugar", amount = "1", unit = "cup")
            ),
            equipment = listOf("bowl", "spoon"),
            instructions = listOf(
                Instruction(step = 1, description = "Mix ingredients", time = "2m"),
                Instruction(step = 2, description = "Bake", time = "10m", temperature = "180°C")
            ),
            nutrition = Nutrition(
                servingSize = "1 serving",
                calories = 300,
                protein = 8.0,
                carbohydrates = 45.0,
                fat = 10.0
            ),
            notes = "Store in cool place",
            storage = Storage(refrigerator = "2 days", freezer = "1 week")
        )

        // Serialize
        val yaml = RecipeYaml.serialize(original)

        // Parse back
        val parsed = RecipeYaml.parse(yaml)

        // Verify
        assertEquals(original.schemaVersion, parsed.schemaVersion)
        assertEquals(original.recipeVersion, parsed.recipeVersion)
        assertEquals(original.metadata.title, parsed.metadata.title)
        assertEquals(original.metadata.author, parsed.metadata.author)
        assertEquals(original.metadata.servings, parsed.metadata.servings)
        assertEquals(original.ingredients.size, parsed.ingredients.size)
        assertEquals(original.instructions.size, parsed.instructions.size)
        assertEquals(original.nutrition?.calories, parsed.nutrition?.calories)
    }

    @Test
    fun testSerializeRecipeWithMedia() {
        val recipe = Recipe(
            schemaVersion = "1.0.0",
            recipeVersion = "1.0.0",
            metadata = RecipeMetadata(
                title = "Recipe with Media",
                source = "https://example.com/recipe",
                dateCreated = LocalDate(2025, 1, 15),
                servings = 2
            ),
            ingredients = listOf(Ingredient(item = "test")),
            instructions = listOf(
                Instruction(
                    step = 1,
                    description = "Follow video",
                    media = listOf(
                        Media(type = "image", path = "/img/step.jpg", alt = "Step image"),
                        Media(type = "video", path = "/vid/step.mp4", thumbnail = "/vid/thumb.jpg", duration = "1:45")
                    )
                )
            )
        )

        val yaml = RecipeYaml.serialize(recipe)

        // Verify YAML contains media elements
        assertTrue(yaml.contains("type: \"image\""))
        assertTrue(yaml.contains("/img/step.jpg"))
        assertTrue(yaml.contains("type: \"video\""))
        assertTrue(yaml.contains("/vid/step.mp4"))
    }

    @Test
    fun testRoundTripWithMedia() {
        val original = Recipe(
            schemaVersion = "1.0.0",
            recipeVersion = "1.0.0",
            metadata = RecipeMetadata(
                title = "Media Recipe",
                source = "https://example.com/recipe",
                dateCreated = LocalDate(2025, 1, 27),
                servings = 2
            ),
            ingredients = listOf(Ingredient(item = "test")),
            instructions = listOf(
                Instruction(
                    step = 1,
                    description = "Watch and learn",
                    media = listOf(
                        Media(type = "image", path = "/img.jpg", alt = "Photo"),
                        Media(type = "video", path = "/vid.mp4", duration = "2:00")
                    )
                )
            )
        )

        // Serialize
        val yaml = RecipeYaml.serialize(original)

        // Parse back
        val parsed = RecipeYaml.parse(yaml)

        // Verify media survived round-trip
        assertEquals(2, parsed.instructions[0].media?.size)
        assertEquals("image", parsed.instructions[0].media?.get(0)?.type)
        assertEquals("/img.jpg", parsed.instructions[0].media?.get(0)?.path)
        assertEquals("video", parsed.instructions[0].media?.get(1)?.type)
        assertEquals("/vid.mp4", parsed.instructions[0].media?.get(1)?.path)
    }
}
