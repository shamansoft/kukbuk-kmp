package net.shamansoft.kukbuk

import kotlinx.datetime.LocalDate
import net.shamansoft.recipe.model.*

/**
 * Test fixtures for creating test data.
 */
object TestFixtures {

    fun createTestRecipe(
        title: String = "Test Recipe",
        author: String = "Test Author",
        description: String = "A delicious test recipe"
    ): Recipe {
        return Recipe(
            schemaVersion = "1.0.0",
            recipeVersion = "1.0.0",
            metadata = RecipeMetadata(
                title = title,
                author = author,
                source = "https://example.com/recipe",
                category = listOf("Test Category"),
                tags = listOf("test", "sample"),
                prepTime = "PT15M",
                cookTime = "PT30M",
                totalTime = "PT45M",
                servings = 4,
                difficulty = "easy",
                coverImage = CoverImage(
                    path = "https://example.com/image.jpg",
                    alt = "Test Recipe Image"
                ),
                dateCreated = LocalDate(2025, 1, 1)
            ),
            description = description,
            ingredients = listOf(
                Ingredient(
                    item = "flour",
                    amount = "2",
                    unit = "cups",
                    notes = "all-purpose",
                    optional = false
                ),
                Ingredient(
                    item = "sugar",
                    amount = "1",
                    unit = "cup",
                    notes = null,
                    optional = false
                )
            ),
            instructions = listOf(
                Instruction(
                    step = 1,
                    description = "Mix flour and sugar"
                ),
                Instruction(
                    step = 2,
                    description = "Bake at 350°F for 30 minutes"
                )
            )
        )
    }

    fun createTestRecipeYaml(
        title: String = "Test Recipe",
        author: String = "Test Author"
    ): String {
        return """
            schema_version: "1.0.0"
            recipe_version: "1.0.0"
            metadata:
              title: "$title"
              author: "$author"
              source: "https://example.com/recipe"
              category:
                - "Test Category"
              tags:
                - test
                - sample
              prep_time: "PT15M"
              cook_time: "PT30M"
              total_time: "PT45M"
              servings: 4
              difficulty: "easy"
              cover_image:
                path: "https://example.com/image.jpg"
                alt: "Test Recipe Image"
              date_created: "2025-01-01"
            description: "A delicious test recipe"
            ingredients:
              - item: "flour"
                amount: "2"
                unit: "cups"
                notes: "all-purpose"
                optional: false
              - item: "sugar"
                amount: "1"
                unit: "cup"
                optional: false
            instructions:
              - step: 1
                description: "Mix flour and sugar"
              - step: 2
                description: "Bake at 350°F for 30 minutes"
        """.trimIndent()
    }
}
